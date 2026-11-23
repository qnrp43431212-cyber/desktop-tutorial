package kr.murimlife;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import kr.murimlife.v16.RecruitmentRules;
import kr.murimlife.v16.TournamentRules;
import kr.murimlife.npc.NpcAdminCommands;

/** User-visible, real server backed progression inspection and practice commands. */
public final class MurimCommands {
    private MurimCommands(){}
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> disp=e.getDispatcher();
        disp.register(Commands.literal("murim")
            .then(Commands.literal("status").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var profile=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
                say(p,"경지 "+profile.realmName()+" | 경지 진척 "+profile.realmProgress+"% | 종합전투력 "+profile.power()+" | 보유금 "+kr.murimlife.economy.CurrencyRules.describe(kr.murimlife.economy.MoneyService.balance(profile))+" | 별호 "+profile.displayedEpithet);return 1;}))
            .then(Commands.literal("breakthrough").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var state=MurimProgressData.get(p.server);
                var profile=state.player(p.getUUID(),p.getName().getString());
                long day=p.server.overworld().getDayTime()/24000L;
                int requiredTrain=12*(profile.realm+1)*(profile.realm+1);
                boolean ready=profile.realm<7&&profile.lastBreakthroughDay!=day&&
                    profile.training>=requiredTrain&&profile.fatigue<70&&profile.inner>=20&&
                    kr.murimlife.v16.RealmRules.canAttemptBreakthrough(profile.realm,profile.power());
                if(!ready){say(p,"돌파 불가: 해당 경지의 전투력·수련 횟수("+requiredTrain+")·내력 20 이상·피로도 70 미만, 하루 1회 조건이 필요합니다.");return 0;}
                profile.lastBreakthroughDay=day;
                profile.inner-=20;profile.fatigue=Math.min(100,profile.fatigue+20);
                boolean deviated=kr.murimlife.qi.QiDeviationService.checkRisk(p,true,profile.realm+1);
                if(!deviated){profile.realm++;profile.realmProgress=10;}
                state.setDirty();say(p,deviated?"돌파 중 주화입마 발생. /murim qi confront 에서 극복해야 돌파를 마칠 수 있습니다.":
                    "돌파 성공! 현재 경지: "+profile.realmName());return 1;
            }))
            .then(Commands.literal("learn").then(Commands.argument("skill",StringArgumentType.greedyString()).executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var d=MurimProgressData.get(p.server);var prof=d.player(p.getUUID(),p.getName().getString());
                boolean ok=MurimCombat.learn(prof,StringArgumentType.getString(ctx,"skill"));
                if(ok)d.setDirty();say(p,ok?"무공을 습득했습니다. M에서 기술 칸에 배치하세요.":"수련 부족·미등록 무공 또는 이미 습득한 기술입니다.");return ok?1:0;
            })))
            .then(Commands.literal("train").then(Commands.argument("stat",StringArgumentType.word()).executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var world=MurimProgressData.get(p.server);var profile=world.player(p.getUUID(),p.getName().getString());
                String stat=StringArgumentType.getString(ctx,"stat");long day=p.server.overworld().getDayTime()/24000L;
                boolean ok=world.practice(profile,stat,p.server.overworld().getGameTime(),day);
                say(p,ok ? stat+" 수련 성공: "+String.format(java.util.Locale.ROOT,"%.2f",profile.attributes.get(stat)) : "수련할 수 없는 능력치이거나 휴식이 필요합니다.");return ok?1:0;})))
            .then(Commands.literal("aliases").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var pr=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
                say(p,"보유 별호: "+String.join(", ",pr.epithets)+" | 대표: "+pr.displayedEpithet);return 1;}))
            .then(Commands.literal("display").then(Commands.argument("alias",StringArgumentType.greedyString()).executes(ctx->{var p=ctx.getSource().getPlayerOrException();var d=MurimProgressData.get(p.server);var pr=d.player(p.getUUID(),p.getName().getString());String alias=StringArgumentType.getString(ctx,"alias");
                boolean ok=d.displayEpithet(p.getUUID(),pr,alias);say(p,ok?"대표 별호 설정: "+alias:"본인이 보유한 별호가 아닙니다.");return ok?1:0;})))
            .then(Commands.literal("recruitment").executes(ctx->{var p=ctx.getSource().getPlayerOrException();long seed=p.server.overworld().getSeed(),day=p.server.overworld().getDayTime()/24000L;
                for(String s:new String[]{"소림사","무당파","화산파","청성파","곤륜파","남궁세가"})say(p,s+": "+(RecruitmentRules.isOpen(seed,s,day)?"모집 중":"다음 모집 "+RecruitmentRules.nextOpenDay(seed,s,day)+"일째"));return 1;}))
            .then(Commands.literal("tournament")
                .then(Commands.literal("enter").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var result=MurimTournamentService.enter(p);say(p,result.description());return result.entered()?1:0;}))
                .executes(ctx->{var p=ctx.getSource().getPlayerOrException();var profile=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());long d=p.server.overworld().getDayTime()/24000L;
                say(p,"정기 무림대회: "+(TournamentRules.grandOpen(d)?"개최기간":"미개최")+" | 신진무인 자격: "+TournamentRules.newcomerEligible(d,profile.martialStartDay,profile.realm)+" (실제 대련장 경기 구현 예정)");return 1;}))
            .then(Commands.literal("npcs").executes(ctx->{var p=ctx.getSource().getPlayerOrException();say(p,"원거리 무림인 시뮬레이션 기록: "+MurimProgressData.get(p.server).simulatedNpcCount()+"명 (아직 월드에 생성되는 NPC 엔티티가 아님)");return 1;}))
        );
        kr.murimlife.economy.EconomyCommands.register(disp);
        NpcAdminCommands.register(disp);
        kr.murimlife.quest.QuestService.registerCommands(disp);
        kr.murimlife.economy.SectGovernanceCommands.register(disp);
        kr.murimlife.cult.CultCommands.register(disp);
        kr.murimlife.qi.QiDeviationService.registerCommands(disp);
        kr.murimlife.medicine.AlchemyService.registerCommands(disp);
        disp.register(Commands.literal("murim").then(Commands.literal("mentor")
            .then(Commands.literal("leave").executes(ctx->{kr.murimlife.cult.CultMentorshipService.leave(ctx.getSource().getPlayerOrException());return 1;}))
            .executes(ctx->{kr.murimlife.cult.CultMentorshipService.status(ctx.getSource().getPlayerOrException());return 1;})));
    }
    private static void say(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("[무림] "+s));}
}
