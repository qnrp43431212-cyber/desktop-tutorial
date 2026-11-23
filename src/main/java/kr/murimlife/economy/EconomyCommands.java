package kr.murimlife.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import kr.murimlife.MurimProgressData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** User wallet, founder treasury and operator-only recruitment hooks until full entrance trials exist. */
public final class EconomyCommands {
    private static final Set<String> RANKS=Set.of("외문제자","내문제자","사부","장로");
    private static final Set<String> OFFICIAL=Set.of("소림사","무당파","화산파","청성파","곤륜파","아미파","종남파","공동파","점창파","개방","남궁세가","사천당문","하북팽가","제갈세가","모용세가","무림맹","사도련");
    private EconomyCommands(){}
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("murim")
            .then(Commands.literal("money").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();var profile=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
                MoneyService.message(p,"현재 소지금 "+CurrencyRules.describe(MoneyService.balance(profile)));return 1;
            }))
            .then(Commands.literal("sect")
                .then(Commands.literal("found").then(Commands.argument("name",StringArgumentType.greedyString()).executes(ctx->{
                    ServerPlayer player=ctx.getSource().getPlayerOrException();String sect=StringArgumentType.getString(ctx,"name").trim();
                    var data=MurimProgressData.get(player.server);var profile=data.player(player.getUUID(),player.getName().getString());
                    if(!"무소속".equals(profile.sect)||sect.length()<2||sect.length()>15||sect.contains("§")||sect.contains("/")||OFFICIAL.contains(sect)){
                        MoneyService.message(player,"무소속이고 2~15자의 새로운 문파 이름을 사용할 수 있어야 합니다.");return 0;}
                    if(!kr.murimlife.cult.CultRankRules.eligible("가주",profile.realm)){
                        MoneyService.message(player,"문파 창설은 최소 초절정(경지 4)에 도달해야 합니다.");return 0;}
                    if(data.playerProfiles().values().stream().anyMatch(p->p.foundedSect.equals(sect))){MoneyService.message(player,"이미 존재하는 문파입니다.");return 0;}
                    if(!MoneyService.pay(player,5000,"문파 창설 비용 (50은량)"))return 0;
                    profile.sect=sect;profile.foundedSect=sect;profile.sectRank="가주";data.setDirty();
                    MoneyService.message(player,"새 문파 '"+sect+"' 창설 완료. 급여는 문파 금고에서 3일마다 지급됩니다. 금고에 돈을 입금하십시오.");return 1;
                })))
                .then(Commands.literal("treasury")
                    .then(Commands.literal("balance").executes(ctx->{
                        ServerPlayer player=ctx.getSource().getPlayerOrException();var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
                        if(pr.foundedSect.isBlank())return 0;
                        MoneyService.message(player,"문파 금고: "+CurrencyRules.describe(SectPayroll.get(player.server).funds(pr.foundedSect)));return 1;
                    }))
                    .then(Commands.literal("deposit").then(Commands.argument("mun",IntegerArgumentType.integer(1,1000000)).executes(ctx->{
                        ServerPlayer player=ctx.getSource().getPlayerOrException();var pr=MurimProgressData.get(player.server).player(player.getUUID(),player.getName().getString());
                        if(pr.foundedSect.isBlank())return 0;
                        int amount=IntegerArgumentType.getInteger(ctx,"mun");
                        if(!MoneyService.pay(player,amount,"문파 금고 입금"))return 0;
                        SectPayroll.get(player.server).deposit(pr.foundedSect,amount);return 1;
                    }))))
                // Temporary owner/ops administration: no fake completion of entrance trials.
                .then(Commands.literal("setmember").requires(src->src.hasPermission(2))
                    .then(Commands.argument("player",StringArgumentType.word())
                        .then(Commands.argument("sect",StringArgumentType.word())
                            .then(Commands.argument("rank",StringArgumentType.word()).executes(ctx->{
                                ServerPlayer owner=ctx.getSource().getPlayerOrException();
                                ServerPlayer target=owner.server.getPlayerList().getPlayerByName(StringArgumentType.getString(ctx,"player"));
                                String sect=StringArgumentType.getString(ctx,"sect"),rank=StringArgumentType.getString(ctx,"rank");
                                if(target==null||!RANKS.contains(rank)){MoneyService.message(owner,"온라인 플레이어 또는 등급이 올바르지 않습니다.");return 0;}
                                var data=MurimProgressData.get(owner.server);var member=data.player(target.getUUID(),target.getName().getString());
                                if(!member.foundedSect.isBlank())return 0;
                                if(!kr.murimlife.cult.CultRankRules.eligible(rank,member.realm)){
                                    MoneyService.message(owner,"상대 경지에 비해 직위가 높습니다.");return 0;}
                                member.sect=sect;member.sectRank=rank;member.lastStipendDay=-1;data.setDirty();
                                MoneyService.message(owner,"문파원 등급 지정: "+member.name+" / "+sect+" / "+rank);return 1;
                            })))))
            )
        );
        SectMembershipCommands.register(d);
    }
}
