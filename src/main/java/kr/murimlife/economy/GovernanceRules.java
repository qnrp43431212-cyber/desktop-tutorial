package kr.murimlife.economy;

/** Pure domain policy: no karma is awarded merely for fame. */
public final class GovernanceRules {
    public static final long PAY_PERIOD_DAYS=3, DESERTION_DAYS=7, CORRECTION_DAYS=15;
    private GovernanceRules(){}
    public enum Alignment { RIGHTEOUS, UNORTHODOX, DEMONIC, BLOOD }
    public static Alignment alignment(String text){return switch(text){
        case "사파","사도련" -> Alignment.UNORTHODOX;
        case "마교" -> Alignment.DEMONIC;
        case "혈교" -> Alignment.BLOOD;
        default -> Alignment.RIGHTEOUS;
    };}
    public static boolean exempt(Alignment a){return a==Alignment.DEMONIC||a==Alignment.BLOOD;}
    public static long recoverable(long unpaid){return unpaid<=0?0:(unpaid/10)*8+((unpaid%10)*8)/10;}
    public static int loyalty(int prior,int unpaidPeriods){return Math.clamp(prior-10*Math.max(0,unpaidPeriods),0,100);}
    public static boolean overdue(long firstMissedDay,long today){return firstMissedDay>=0&&today-firstMissedDay>=DESERTION_DAYS;}
    public static int desertionQuota(int members){return Math.clamp((members*7+9)/10,0,members);}
    public static int reputation(int old,int delta){return (int)Math.clamp((long)old+delta,0L,1000L);}
    public static int missionGrade(int realm,int reputation,boolean mercenary){
        int unlocked=Math.min(4,Math.max(0,realm/2)+(reputation>=220?1:0)+(reputation>=600?1:0));
        return mercenary?Math.min(1,unlocked):unlocked;
    }
    public static int priceDiscountPercent(int reputation){return Math.min(15,Math.max(0,reputation)/100*2);}
    public static int recognitionPercent(int reputation){return Math.min(90,Math.max(0,reputation-80)/10);}
}
