package kr.murimlife;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server validates all slot, skill, and cooldown claims. */
public final class MurimNetwork {
    private MurimNetwork(){}
    public record SelectSlot(int slot) implements CustomPacketPayload {
        public static final Type<SelectSlot> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"select_slot"));
        public static final StreamCodec<ByteBuf,SelectSlot> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,SelectSlot::slot,SelectSlot::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Parry() implements CustomPacketPayload {
        public static final Type<Parry> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"parry"));
        public static final StreamCodec<ByteBuf,Parry> STREAM_CODEC=StreamCodec.unit(new Parry());
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record AssignSkill(int slot,String skill) implements CustomPacketPayload {
        public static final Type<AssignSkill> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"assign_skill"));
        public static final StreamCodec<ByteBuf,AssignSkill> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AssignSkill::slot,ByteBufCodecs.STRING_UTF8,AssignSkill::skill,AssignSkill::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record ProfileSync(String summary,String arts,String stats,String sect,String slots) implements CustomPacketPayload {
        public static final Type<ProfileSync> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"profile_sync"));
        public static final StreamCodec<ByteBuf,ProfileSync> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,ProfileSync::summary,ByteBufCodecs.STRING_UTF8,ProfileSync::arts,
            ByteBufCodecs.STRING_UTF8,ProfileSync::stats,ByteBufCodecs.STRING_UTF8,ProfileSync::sect,
            ByteBufCodecs.STRING_UTF8,ProfileSync::slots,ProfileSync::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record TalkRequest(int entityId) implements CustomPacketPayload {
        public static final Type<TalkRequest> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"talk_request"));
        public static final StreamCodec<ByteBuf,TalkRequest> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,TalkRequest::entityId,TalkRequest::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record TheftClick() implements CustomPacketPayload {
        public static final Type<TheftClick> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"theft_click"));
        public static final StreamCodec<ByteBuf,TheftClick> STREAM_CODEC=StreamCodec.unit(new TheftClick());
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record TheftStatus(boolean active) implements CustomPacketPayload {
        public static final Type<TheftStatus> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"theft_status"));
        public static final StreamCodec<ByteBuf,TheftStatus> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,TheftStatus::active,TheftStatus::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record DialogueView(String title,String description,String choices) implements CustomPacketPayload {
        public static final Type<DialogueView> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"dialogue_view"));
        public static final StreamCodec<ByteBuf,DialogueView> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,DialogueView::title,ByteBufCodecs.STRING_UTF8,DialogueView::description,
            ByteBufCodecs.STRING_UTF8,DialogueView::choices,DialogueView::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record DialogueNotice(String message) implements CustomPacketPayload {
        public static final Type<DialogueNotice> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"dialogue_notice"));
        public static final StreamCodec<ByteBuf,DialogueNotice> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,DialogueNotice::message,DialogueNotice::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record DialogueDismiss() implements CustomPacketPayload {
        public static final Type<DialogueDismiss> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID,"dialogue_dismiss"));
        public static final StreamCodec<ByteBuf,DialogueDismiss> STREAM_CODEC=StreamCodec.unit(new DialogueDismiss());
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static void sync(ServerPlayer player) {
        var p=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
        String summary="경지 "+p.realmName()+" / 전투력 "+p.power()+" / 보유금 "+kr.murimlife.economy.CurrencyRules.describe(kr.murimlife.economy.MoneyService.balance(p))+" / 별호 "+p.displayedEpithet;
        String stats=String.join("  ",p.attributes.entrySet().stream().map(e->e.getKey()+":"+String.format(java.util.Locale.ROOT,"%.1f",e.getValue())).toList());
        PacketDistributor.sendToPlayer(player,new ProfileSync(summary,String.join("|",p.arts),stats,p.sect,String.join("|",java.util.Arrays.stream(p.actionSlots).map(s->s==null?"":s).toList())));
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar=event.registrar("3");
        registrar.playToClient(DialogueView.TYPE,DialogueView.STREAM_CODEC,(payload,context)->
            context.enqueueWork(()->MurimHudOverlay.dialogue(payload.title(),payload.description(),payload.choices())));
        registrar.playToClient(DialogueNotice.TYPE,DialogueNotice.STREAM_CODEC,(payload,context)->
            context.enqueueWork(()->MurimHudOverlay.notice(payload.message())));
        registrar.playToClient(DialogueDismiss.TYPE,DialogueDismiss.STREAM_CODEC,(payload,context)->
            context.enqueueWork(MurimHudOverlay::closeDialogue));
        registrar.playToServer(TheftClick.TYPE,TheftClick.STREAM_CODEC,(payload,context)->{
            if(context.player() instanceof ServerPlayer p)context.enqueueWork(()->kr.murimlife.economy.TheftService.click(p));
        });
        registrar.playToClient(TheftStatus.TYPE,TheftStatus.STREAM_CODEC,(payload,context)->{
            context.enqueueWork(()->ClientEvents.setTheftActive(payload.active()));
        });
        registrar.playToServer(TalkRequest.TYPE,TalkRequest.STREAM_CODEC,(payload,context)->{
            if(!(context.player() instanceof ServerPlayer player))return;
            context.enqueueWork(()->kr.murimlife.npc.dialogue.MurimDialogueService.onTilde(player,payload.entityId()));
        });
        registrar.playToServer(SelectSlot.TYPE,SelectSlot.STREAM_CODEC,(payload,context)->{
            if(!(context.player() instanceof ServerPlayer player))return;
            context.enqueueWork(()->{
                if(payload.slot()==-1){sync(player);return;}
                if(payload.slot()<0 ||payload.slot()>=12)return;
                MurimCombat.use(player,payload.slot());sync(player);
            });
        });
        registrar.playToServer(Parry.TYPE,Parry.STREAM_CODEC,(payload,context)->{
            if(!(context.player() instanceof ServerPlayer player))return;
            context.enqueueWork(()->{
                var d=MurimProgressData.get(player.server);var p=d.player(player.getUUID(),player.getName().getString());
                long t=player.server.overworld().getGameTime();
                if(p.fatigue<90 && t-p.lastParryKeyTick>=24){p.parryUntil=t+9;p.lastParryKeyTick=t;p.fatigue=Math.min(100,p.fatigue+1);d.setDirty();}
            });
        });
        registrar.playToServer(AssignSkill.TYPE,AssignSkill.STREAM_CODEC,(payload,context)->{
            if(!(context.player() instanceof ServerPlayer player))return;
            context.enqueueWork(()->{
                var d=MurimProgressData.get(player.server);var p=d.player(player.getUUID(),player.getName().getString());
                if(MurimCombat.assign(p,payload.slot(),payload.skill()))d.setDirty();
                sync(player);
            });
        });
        registrar.playToClient(ProfileSync.TYPE,ProfileSync.STREAM_CODEC,(payload,context)->{
            context.enqueueWork(()->ClientProfileCache.accept(payload));
        });
    }
}
