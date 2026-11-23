package kr.murimlife.cult;
/** High-ranking cultists prefer recruitment; murderous refusal remains exceptionally rare. */
public final class CultMentorRules {
    private CultMentorRules(){}
    public static int admissionChance(int playerRealm){return Math.clamp(90+playerRealm,90,97);}
    public static int apprenticeChance(int masterRealm,int learnerRealm,int favor){
        return Math.clamp(84-Math.max(0,masterRealm-learnerRealm)*4+favor/7,58,92);
    }
    public static int hostileRefusalChance(int favor){return favor<=-20?3:1;}
    public static int lessonsRequired(int masterRealm,int learnerRealm){return Math.clamp(5+(masterRealm-learnerRealm)*2,5,17);}
    public static boolean mastered(int lessons,int needed,int realm,int minRealm){
        return lessons>=needed && realm>=minRealm;
    }
}
