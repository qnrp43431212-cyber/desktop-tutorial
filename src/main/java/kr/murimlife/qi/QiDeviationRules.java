package kr.murimlife.qi;
/** Deterministic cultivation hazard bounds, independent of game APIs for auditability. */
public final class QiDeviationRules {
    private QiDeviationRules(){}
    public static int risk(double strain,double fatigue,double mental,int realm,boolean forcing){
        double raw=Math.max(0,strain)*.78+Math.max(0,fatigue-35)*.50+
            Math.max(0,17-mental)*1.4+(forcing?17:0)+realm*1.8;
        return (int)Math.clamp(Math.round(raw),0,100);
    }
    public static int chance(int risk){return Math.clamp((risk-32)*2/3,0,46);}
    public static int severity(int risk){return risk<52?1:risk<70?2:risk<86?3:4;}
    public static int rounds(int severity){return 2+Math.clamp(severity,1,4);}
    public static int insight(int severity,int mistakes){return Math.max(1,severity*(9+severity*3)-mistakes*3);}
    public static long aftereffectTicks(int severity,int mistakes){return 12000L*severity+6000L*mistakes;}
    public static int answer(long seed,int phase){return Math.floorMod((int)(seed^(seed>>>32))+phase*7,3);}
}
