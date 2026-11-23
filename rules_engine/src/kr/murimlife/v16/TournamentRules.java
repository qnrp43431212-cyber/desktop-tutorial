package kr.murimlife.v16;
import java.util.*;
/** Server-side competition schedule and non-exploitable eligibility checks. */
public final class TournamentRules {
    private TournamentRules(){}
    public enum Tournament { NEWCOMER, MARTIAL_ALLIANCE }
    public static final int GRAND_PERIOD=84; // longer than 21 day sect recruitment
    public static final int GRAND_START=42;
    public static final int NEWCOMER_DELAY=30;
    public static boolean grandOpen(long worldDay) {
        return worldDay>=GRAND_START && Math.floorMod(worldDay-GRAND_START,GRAND_PERIOD)<3;
    }
    public static boolean newcomerEligible(long worldDay,long startedMartialDay,int realmIndex) {
        if(startedMartialDay<0)return false;
        long elapsed=worldDay-startedMartialDay;
        return elapsed>=NEWCOMER_DELAY && elapsed<NEWCOMER_DELAY+14 && realmIndex<=2;
    }
    public static int grandPrizeSilver(int division) {
        return switch(division){case 0->800;case 1->2200;case 2->5000;default->throw new IllegalArgumentException("division");};
    }
    public static int newcomerPrizeSilver(){return 250;}
    public static int divisionForRealm(int realm){return realm<3?0:realm<5?1:2;}
    public static double matchOdds(double ownPower,double enemyPower,double fatigue) {
        double relative=Math.max(.01,ownPower)/Math.max(.01,enemyPower);
        return Math.min(.97,Math.max(.03,(relative/(1+relative))*(1-Math.min(.85,fatigue)*.4)));
    }
}
