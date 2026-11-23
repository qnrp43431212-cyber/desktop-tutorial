import kr.murimlife.economy.CurrencyRules;

/** Stand-alone mathematics QA for all v1.23 currency, salary and pickpocket rules. */
public final class MurimV123PureTests {
    private static int count;
    static void check(boolean ok,String message){count++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        check(CurrencyRules.MUN_PER_SILVER==100,"silver conversion");
        check(CurrencyRules.SILVER_PER_GOLD==10,"gold conversion");
        check(CurrencyRules.total(12,45)==1245,"balance conversion");
        check("1금량 2은량 45문".equals(CurrencyRules.describe(1245)),"currency representation");
        check("0문".equals(CurrencyRules.describe(-1000)),"nonnegative formatting");
        check(CurrencyRules.total(0,0)==0,"zero");
        check(CurrencyRules.allowance("외문제자")==75,"outer salary");
        check(CurrencyRules.allowance("내문제자")==180,"inner salary");
        check(CurrencyRules.allowance("사부")==420,"instructor salary");
        check(CurrencyRules.allowance("장로")==850,"elder salary");
        check(CurrencyRules.allowance("외문제자")<CurrencyRules.allowance("내문제자"),"pay rank ordering 1");
        check(CurrencyRules.allowance("내문제자")<CurrencyRules.allowance("사부"),"pay rank ordering 2");
        check(CurrencyRules.allowance("사부")<CurrencyRules.allowance("장로"),"pay rank ordering 3");
        double low=CurrencyRules.theftChance(10,10,10,8,0,0),high=CurrencyRules.theftChance(30,30,30,8,0,0);
        check(high>low,"better statistics raise odds");
        check(CurrencyRules.theftChance(10,10,10,25,0,0)>low,"click speed raises odds");
        check(CurrencyRules.theftChance(10,10,10,8,0,6)<low,"higher victim realm harder");
        check(CurrencyRules.theftChance(10,10,10,8,0,6)>=.04,"min probability");
        check(CurrencyRules.theftChance(500,500,500,999,6,0)<=.92,"max probability");
        check(CurrencyRules.pocketReward(6,false,123)>CurrencyRules.pocketReward(1,false,123),"stronger opponent richer");
        check(CurrencyRules.pocketReward(2,true,123)>CurrencyRules.pocketReward(2,false,123),"merchant richer");
        check(CurrencyRules.killLoot(6,false,true,123)>CurrencyRules.killLoot(1,false,true,123),"high realm kill loot");
        check(CurrencyRules.killLoot(2,true,false,123)>CurrencyRules.killLoot(2,false,false,123),"merchant kill loot");
        for(int realm=0;realm<=7;realm++){
            check(CurrencyRules.pocketReward(realm,true,1)>=0,"pocket nonnegative");
            check(CurrencyRules.killLoot(realm,true,true,1)>=0,"kill nonnegative");
        }
        System.out.println("PASS "+count+" independent v1.23 balance and pickpocket assertions");
    }
}
