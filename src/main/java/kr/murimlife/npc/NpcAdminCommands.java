package kr.murimlife.npc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Separate commands below the SAME /murim root and the SAME murimlife mod ID. */
public final class NpcAdminCommands {
    private NpcAdminCommands(){}
    private static void say(ServerPlayer p,String text){p.sendSystemMessage(Component.literal("[무림 NPC] "+text));}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        kr.murimlife.npc.dialogue.MurimDialogueService.registerCommands(dispatcher);
        kr.murimlife.npc.dialogue.CivicDialogueService.registerCommands(dispatcher);
        kr.murimlife.npc.dialogue.DisputeService.register(dispatcher);
        kr.murimlife.economy.MerchantService.registerCommands(dispatcher);
        dispatcher.register(Commands.literal("murim")
            .then(Commands.literal("npc")
                .then(Commands.literal("status").executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();
                    var cfg=NpcResidentService.config();
                    var ledger=NpcPopulationLedger.get(p.serverLevel());
                    say(p,"월드에 등록된 영구 주민: "+ledger.countTotal()+"명 / 현재 로딩된 주민: "+NpcResidentService.loadedResidentCount(p.serverLevel())+"명, 로딩 인원 상한: "+cfg.residentGlobalCap+"명, 자동 생성: "+cfg.enablePersistentResidents);
                    say(p,"청크가 언로드되면 주민 엔티티는 저장되고 틱은 중단됩니다. 돌아오면 같은 개체가 로딩됩니다.");
                    return 1;
                }))
                .then(Commands.literal("leader").then(Commands.argument("site",StringArgumentType.word()).executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();
                    String site=StringArgumentType.getString(ctx,"site");
                    var leader=NpcNameCatalog.load().leader(site);
                    if(leader==null){say(p,"등록된 유명 문파·세가 ID가 아닙니다: "+site);return 0;}
                    say(p,site+" 장문·가주 "+leader.korean()+" / "+leader.hanzi()+" / "+leader.pinyin());return 1;
                })))
                .then(Commands.literal("spawn").requires(s->s.hasPermission(2))
                    .then(Commands.argument("site",StringArgumentType.word()).executes(ctx->{
                        ServerPlayer p=ctx.getSource().getPlayerOrException();
                        say(p,NpcResidentService.testSpawn(p,StringArgumentType.getString(ctx,"site")));return 1;
                    })))
            )
            .then(Commands.literal("arena")
                .then(Commands.literal("npcvnpc").requires(s->s.hasPermission(2)).executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();say(p,NpcArenaService.start(p,false));return 1;
                }))
                .then(Commands.literal("playernpc").requires(s->s.hasPermission(2)).executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();say(p,NpcArenaService.start(p,true));return 1;
                }))
                .then(Commands.literal("stop").requires(s->s.hasPermission(2)).executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();say(p,NpcArenaService.stop());return 1;
                }))
            )
        );
    }
}
