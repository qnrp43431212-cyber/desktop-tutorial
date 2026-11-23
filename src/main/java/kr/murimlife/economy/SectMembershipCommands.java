package kr.murimlife.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import kr.murimlife.MurimProgressData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Founder-authorized invitations and rank changes; does not bypass canonical sect entrance trials. */
public final class SectMembershipCommands {
    private record Invite(UUID founder,String sect,long expires){}
    private static final Map<UUID,Invite> PENDING=new HashMap<>();
    private static final Set<String> RANKS=Set.of("외문제자","내문제자","사부","장로");
    private SectMembershipCommands(){}
    private static void invite(ServerPlayer founder,String name){
        var data=MurimProgressData.get(founder.server);var profile=data.player(founder.getUUID(),founder.getName().getString());
        if(profile.foundedSect.isBlank()){MoneyService.message(founder,"문파 가주만 초대할 수 있습니다.");return;}
        ServerPlayer target=founder.server.getPlayerList().getPlayerByName(name);
        if(target==null||target==founder||target.level()!=founder.level()||target.distanceToSqr(founder)>144){MoneyService.message(founder,"12블록 이내의 온라인 플레이어만 초대할 수 있습니다.");return;}
        var member=data.player(target.getUUID(),target.getName().getString());
        if(!"무소속".equals(member.sect)){MoneyService.message(founder,"상대는 이미 문파에 속해 있습니다.");return;}
        PENDING.put(target.getUUID(),new Invite(founder.getUUID(),profile.foundedSect,target.serverLevel().getGameTime()+600));
        target.sendSystemMessage(Component.literal("[문파 초대] "+founder.getName().getString()+"이(가) '"+profile.foundedSect+"' 문파에 초대했습니다. ")
            .append(Component.literal("[수락]").withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/murim sect accept")))));
        MoneyService.message(founder,"초대를 보냈습니다. 유효시간 30초.");
    }
    private static void accept(ServerPlayer player){
        Invite invitation=PENDING.remove(player.getUUID());if(invitation==null||player.serverLevel().getGameTime()>invitation.expires())return;
        ServerPlayer founder=player.server.getPlayerList().getPlayer(invitation.founder());
        if(founder==null||founder.level()!=player.level()||founder.distanceToSqr(player)>144)return;
        var data=MurimProgressData.get(player.server);var own=data.player(player.getUUID(),player.getName().getString());
        var leader=data.player(founder.getUUID(),founder.getName().getString());
        if(!"무소속".equals(own.sect)||!leader.foundedSect.equals(invitation.sect()))return;
        own.sect=invitation.sect();own.sectRank="외문제자";own.lastStipendDay=-1;data.setDirty();
        MoneyService.message(player,"'"+own.sect+"' 문파에 입문했습니다. 외문제자부터 시작합니다.");
        MoneyService.message(founder,player.getName().getString()+"이(가) 문파에 들어왔습니다.");
    }
    private static void promote(ServerPlayer founder,String name,String rank){
        var data=MurimProgressData.get(founder.server);var own=data.player(founder.getUUID(),founder.getName().getString());
        if(own.foundedSect.isBlank()||!RANKS.contains(rank))return;
        var target=founder.server.getPlayerList().getPlayerByName(name);
        if(target==null||target.level()!=founder.level()||target.distanceToSqr(founder)>144)return;
        var member=data.player(target.getUUID(),target.getName().getString());
        if(!own.foundedSect.equals(member.sect)||!member.foundedSect.isBlank())return;
        if(!kr.murimlife.cult.CultRankRules.eligible(rank,member.realm)){
            MoneyService.message(founder,"상대의 경지("+member.realmName()+")가 "+rank+" 직위에 미달합니다.");return;}
        member.sectRank=rank;data.setDirty();
        MoneyService.message(founder,"문파원 "+name+"의 직위를 "+rank+"(으)로 변경했습니다. 다음 정기급여부터 적용됩니다.");
    }
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(Commands.literal("murim").then(Commands.literal("sect")
            .then(Commands.literal("invite").then(Commands.argument("player",StringArgumentType.word()).executes(ctx->{
                invite(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"player"));return 1;})))
            .then(Commands.literal("accept").executes(ctx->{accept(ctx.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("promote").then(Commands.argument("player",StringArgumentType.word())
                .then(Commands.argument("rank",StringArgumentType.word()).executes(ctx->{
                    promote(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"player"),StringArgumentType.getString(ctx,"rank"));return 1;}))))
            .then(Commands.literal("leave").executes(ctx->{
                var player=ctx.getSource().getPlayerOrException();var data=MurimProgressData.get(player.server);
                var pr=data.player(player.getUUID(),player.getName().getString());
                if(!pr.foundedSect.isBlank()||"무소속".equals(pr.sect))return 0;
                pr.sect="무소속";pr.sectRank="외문제자";data.setDirty();
                MoneyService.message(player,"문파를 떠났습니다. 탈퇴 후 급여가 중단됩니다.");return 1;
            }))
        ));
    }
}
