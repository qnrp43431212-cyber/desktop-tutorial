package kr.murimlife.npc.dialogue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import kr.murimlife.economy.MerchantService;
import kr.murimlife.economy.TheftService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Noncombat ~ dialogue for villagers and wandering merchants; vanilla right-click never starts a shop. */
public final class CivicDialogueService {
    private record Session(UUID npc,int token,long until){}
    private static final Map<UUID,Session> OPEN=new HashMap<>();
    private static int token;
    private static final String[] OPTIONS={"기연 소문","지금·다음 문파 모집","정기 무림대회 지정일","구파일방·오대세가·무림맹·사도련 위치",
        "정밀한 기연 정보 (유료 가능)","상점 열기","소매치기 도전","거짓 정보 상인 추궁","시민 임무 문의","대화 종료"};
    private CivicDialogueService(){}
    private static void say(ServerPlayer p,String t){p.sendSystemMessage(Component.literal("[무림 대화] "+t));}
    public static boolean open(ServerPlayer p,int entityId){
        Entity raw=p.serverLevel().getEntity(entityId);
        if(!(raw instanceof LivingEntity npc)||!NpcRoles.supported(npc)||npc instanceof kr.murimlife.npc.MurimHumanoid||
            !npc.isAlive()||p.distanceToSqr(npc)>36)return false;
        OPEN.put(p.getUUID(),new Session(npc.getUUID(),++token,p.serverLevel().getGameTime()+1200));
        say(p,npc.getDisplayName().getString()+": 무슨 정보를 찾으십니까?");
        kr.murimlife.quest.ReputationService.recognize(p,npc);show(p);return true;
    }
    private static LivingEntity find(ServerPlayer p){
        Session s=OPEN.get(p.getUUID());
        if(s==null||p.serverLevel().getGameTime()>s.until())return null;
        Entity entity=p.serverLevel().getEntity(s.npc());
        return entity instanceof LivingEntity npc && npc.isAlive() && p.distanceToSqr(npc)<=36?npc:null;
    }
    private static void show(ServerPlayer p){
        Session s=OPEN.get(p.getUUID());if(s==null)return;
        for(int i=0;i<OPTIONS.length;i++){
            final int index=i+1;
            p.sendSystemMessage(Component.literal("["+index+". "+OPTIONS[i]+"]").withStyle(style->style.withColor(ChatFormatting.AQUA)
                .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/murim civic choose "+s.token()+" "+index))));
        }
    }
    public static void choose(ServerPlayer p,int token,int option){
        Session s=OPEN.get(p.getUUID());LivingEntity npc=find(p);
        if(s==null||s.token()!=token||npc==null){OPEN.remove(p.getUUID());say(p,"대화가 만료되었습니다. ~키로 다시 말을 거십시오.");return;}
        if(option>=1&&option<=5)InformationService.ask(p,npc,switch(option){case 1->4;case 2->1;case 3->2;case 4->3;default->5;});
        else if(option==6)MerchantService.open(p,npc);
        else if(option==7){OPEN.remove(p.getUUID());TheftService.start(p,npc);return;}
        else if(option==8)DisputeService.open(p,npc);
        else if(option==9)kr.murimlife.quest.QuestService.openFromNpc(p,npc);
        else if(option==10){OPEN.remove(p.getUUID());say(p,"대화를 마쳤습니다.");return;}
        OPEN.put(p.getUUID(),new Session(s.npc(),s.token(),p.serverLevel().getGameTime()+1200));
        show(p);
    }
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("murim").then(Commands.literal("civic")
            .then(Commands.literal("choose").then(Commands.argument("token",IntegerArgumentType.integer(1))
                .then(Commands.argument("option",IntegerArgumentType.integer(1,10)).executes(ctx->{
                    choose(ctx.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(ctx,"token"),IntegerArgumentType.getInteger(ctx,"option"));return 1;}))))));
    }
}
