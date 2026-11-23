import kr.murimlife.economy.GovernanceRules;
import kr.murimlife.economy.CurrencyRules;
/** Pure-Java tests run without NeoForge; they test policy arithmetic, not server gameplay. */
public final class MurimV124PolicyTests {
    private static int tests;
    private static void check(boolean ok,String label){tests++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        check(GovernanceRules.PAY_PERIOD_DAYS==3,"pay period");
        check(GovernanceRules.DESERTION_DAYS==7,"desertion period");
        check(GovernanceRules.CORRECTION_DAYS==15,"correction period");
        check(GovernanceRules.alignment("사파")==GovernanceRules.Alignment.UNORTHODOX,"sado");
        check(GovernanceRules.alignment("정파")==GovernanceRules.Alignment.RIGHTEOUS,"righteous");
        check(GovernanceRules.exempt(GovernanceRules.alignment("마교")),"demonic exempt");
        check(GovernanceRules.exempt(GovernanceRules.alignment("혈교")),"blood exempt");
        check(!GovernanceRules.exempt(GovernanceRules.alignment("사파")),"unorthodox pays");
        check(!GovernanceRules.exempt(GovernanceRules.alignment("정파")),"righteous pays");
        check(GovernanceRules.recoverable(1000)==800,"80% seizure");
        check(GovernanceRules.recoverable(101)==80,"rounded down");
        check(GovernanceRules.recoverable(0)==0,"empty seizure");
        check(GovernanceRules.loyalty(100,1)==90,"loyalty loss");
        check(GovernanceRules.loyalty(5,3)==0,"loyalty clamp");
        check(GovernanceRules.overdue(3,10),"seventh day");
        check(!GovernanceRules.overdue(3,9),"sixth day");
        check(!GovernanceRules.overdue(-1,100),"no debt");
        check(GovernanceRules.desertionQuota(10)==7,"70% desertion");
        check(GovernanceRules.desertionQuota(1)==1,"ceil 70%");
        check(GovernanceRules.desertionQuota(0)==0,"empty sect");
        check(GovernanceRules.reputation(999,500)==1000,"max reputation");
        check(GovernanceRules.reputation(2,-500)==0,"min reputation");
        check(GovernanceRules.priceDiscountPercent(0)==0,"no discount");
        check(GovernanceRules.priceDiscountPercent(1000)==15,"discount cap");
        check(GovernanceRules.recognitionPercent(80)==0,"no celebrity");
        check(GovernanceRules.recognitionPercent(1000)<=90,"recognition cap");
        check(GovernanceRules.missionGrade(7,1000,true)==1,"mercenary cap");
        check(GovernanceRules.missionGrade(7,1000,false)==4,"member high grade");
        check(GovernanceRules.missionGrade(0,0,false)==0,"novice grade");
        for(int r=0;r<=1000;r+=20){
            check(GovernanceRules.reputation(r,0)==r,"reputation stable "+r);
            check(GovernanceRules.priceDiscountPercent(r)>=0,"discount nonnegative "+r);
            check(GovernanceRules.priceDiscountPercent(r)<=15,"discount capped "+r);
            check(GovernanceRules.recognitionPercent(r)>=0,"recognition nonnegative "+r);
            check(GovernanceRules.recognitionPercent(r)<=90,"recognition capped "+r);
            check(GovernanceRules.missionGrade(6,r,true)<=1,"mercenary bounded "+r);
        }
        for(int i=0;i<=24;i++){
            check(GovernanceRules.desertionQuota(i)>=0 && GovernanceRules.desertionQuota(i)<=i,"desertion bound "+i);
            check(GovernanceRules.recoverable(i*100L)<=i*100L,"seizure bound "+i);
        }
        for(String rank:new String[]{"외문제자","내문제자","사부","장로"})
            check(CurrencyRules.allowance(rank)>0,"pay rank "+rank);
        System.out.println("PASS: "+tests+" v1.24 pure policy assertions");
    }
}
