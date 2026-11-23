package kr.murimlife.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import kr.murimlife.MurimProgressData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** Founder decisions are owner-authorized and persisted in the founder profile. */
public final class SectGovernanceCommands {
    private SectGovernanceCommands(){}
    private static void alignment(ServerPlayer founder,String choice){
        var data=MurimProgressData.get(founder.server);var own=data.player(founder.getUUID(),founder.getName().getString());
        if(own.foundedSect.isBlank()){MoneyService.message(founder,"문파 가주만 문파 성향을 정할 수 있습니다.");return;}
        if((choice.equals("마교")||choice.equals("혈교"))&&own.realm<6){
            MoneyService.message(founder,"마교·혈교 창설 교주는 최소 현경(경지 6)에 올라야 합니다.");return;}
        if(!java.util.Set.of("정파","사파","마교","혈교").contains(choice)){MoneyService.message(founder,"정파·사파·마교·혈교만 가능합니다.");return;}
        if(data.playerProfiles().values().stream().anyMatch(p->
            p!=own && (own.foundedSect.equals(p.sect)||p.wageClaims.getOrDefault(own.foundedSect,0L)>0)) ||
            SectDiscipline.get(founder.server).audit(own.foundedSect)!=null || own.righteousLicenseRevoked ||
            NpcSectRoster.get(founder.server).encumbered(own.foundedSect)){
            MoneyService.message(founder,"기존 문파원·급여 채무·감찰 사건이 있으면 성향을 바꿀 수 없습니다.");return;
        }
        own.sectAlignment=choice;data.setDirty();
        MoneyService.message(founder,"문파 성향: "+choice+(GovernanceRules.exempt(GovernanceRules.alignment(choice))?" / 마교·혈교는 급여 의무 면제":" / 급여 의무 적용"));
    }
    private static void finances(ServerPlayer player){
        var data=MurimProgressData.get(player.server);var own=data.player(player.getUUID(),player.getName().getString());
        if(own.foundedSect.isBlank()){MoneyService.message(player,"현재 소속: "+own.sect+" / 충성도 "+own.loyalty+
            " / 체불: "+own.wageClaims.toString());return;}
        String sect=own.foundedSect;long owed=0;int members=0,late=0;
        for(var member:data.playerProfiles().values()){
            if(sect.equals(member.sect)&&member.foundedSect.isBlank())members++;
            long debt=member.wageClaims.getOrDefault(sect,0L);owed+=Math.max(0,debt);
            if(debt>0)late++;
        }
        var npcRoster=NpcSectRoster.get(player.server);
        owed+=npcRoster.unpaid(sect);
        MoneyService.message(player,"문파 "+sect+" / 성향 "+own.sectAlignment+" / 정파 자격 "+(!own.righteousLicenseRevoked)+
            " / 플레이어 문파원 "+members+"명 / NPC 문파원 "+npcRoster.active(sect)+"명 / 플레이어 채권자 "+late+"명 / 체불 합계 "+CurrencyRules.describe(owed)+
            " / 금고 "+CurrencyRules.describe(SectPayroll.get(player.server).funds(sect)));
        SectDiscipline.get(player.server).progress(player);
    }
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("murim")
            .then(Commands.literal("reputation").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var own=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
                MoneyService.message(p,"강호 평판 "+own.reputation+"/1000 | 은폐 "+
                    (kr.murimlife.quest.ReputationService.disguised(p)?"예":"아니오")+
                    " | 상점 할인 "+GovernanceRules.priceDiscountPercent(own.reputation)+"% | 선업 "+own.good+" (평판과 별개)");return 1;}))
            .then(Commands.literal("sect")
                .then(Commands.literal("alignment").then(Commands.argument("faction",StringArgumentType.word()).executes(ctx->{
                    alignment(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"faction"));return 1;})))
                .then(Commands.literal("finances").executes(ctx->{finances(ctx.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("audit").executes(ctx->{
                    var p=ctx.getSource().getPlayerOrException();var profile=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
                    SectDiscipline.get(p.server).progress(p);return 1;}))
                .then(Commands.literal("supply")
                    .then(Commands.literal("iron").executes(ctx->{SectDiscipline.get(ctx.getSource().getServer()).deliver(ctx.getSource().getPlayerOrException(),"iron");return 1;}))
                    .then(Commands.literal("bread").executes(ctx->{SectDiscipline.get(ctx.getSource().getServer()).deliver(ctx.getSource().getPlayerOrException(),"bread");return 1;})))
            )
        );
    }
}
