package kr.murimlife;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import kr.murimlife.v16.TournamentRules;

/** Playable text-simulated bracket. Physical arena/follower AI remains in backlog. */
public final class MurimTournamentService {
    private MurimTournamentService(){}
    public record Result(boolean entered,boolean champion,String description){}
    public static Result enter(ServerPlayer player) {
        var state=MurimProgressData.get(player.server);var p=state.player(player.getUUID(),player.getName().getString());
        long day=player.server.overworld().getDayTime()/24000L;
        boolean isGrand=TournamentRules.grandOpen(day);
        boolean isNew=TournamentRules.newcomerEligible(day,p.martialStartDay,p.realm);
        if(!isGrand && !isNew)return new Result(false,false,"현재 참가 가능한 대회가 없습니다.");
        // An eligible newcomer tournament takes precedence; it occurs once per character.
        boolean newcomer=isNew && p.lastNewcomerTournamentDay<0;
        long eventKey=newcomer ? p.martialStartDay+30 : 42+(day-42)/84*84;
        if(!newcomer && (!isGrand || p.lastGrandTournamentDay==eventKey))return new Result(false,false,"이미 참가했거나 정기대회가 개막하지 않았습니다.");
        if(newcomer)p.lastNewcomerTournamentDay=day;else p.lastGrandTournamentDay=eventKey;
        var random=new Random(player.server.overworld().getSeed() ^ player.getUUID().getMostSignificantBits() ^ (eventKey*0x9E3779B97F4A7C15L));
        int victories=0;
        int div=TournamentRules.divisionForRealm(p.realm);
        for(int round=0;round<3;round++) {
            double enemyPower=p.power() * (.68+random.nextDouble()*.80) * (1+round*.12);
            double chance=TournamentRules.matchOdds(p.power(),enemyPower,p.fatigue/100.0);
            if(random.nextDouble()>chance)break;
            victories++;
        }
        p.counters.merge("duel_wins",victories,Integer::sum);
        int payment=victories==3 ? (newcomer?TournamentRules.newcomerPrizeSilver():TournamentRules.grandPrizeSilver(div)) : victories*25;
        p.silver+=payment;
        if(victories==3)p.counters.merge("tournament_win",1,Integer::sum);
        EpithetAwards.evaluate(player.getUUID(),p,state);state.setDirty();
        return new Result(true,victories==3,(newcomer?"신진무인 대회":"정기 무림대회")+": "+victories+"승 / 상금 "+payment+"은 (임시 문자 대련 시뮬레이션)");
    }
}
