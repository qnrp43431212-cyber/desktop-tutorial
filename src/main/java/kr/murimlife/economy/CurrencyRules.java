package kr.murimlife.economy;

/** Wuxia denominations, all prices and transactions are stored in copper mun. */
public final class CurrencyRules {
    private CurrencyRules(){}
    public static final int MUN_PER_SILVER=100, SILVER_PER_GOLD=10;
    public static long total(int silver,int copper){return Math.max(0L,(long)silver)*MUN_PER_SILVER+Math.max(0,copper);}
    public static String describe(long mun){
        mun=Math.max(0,mun);long gold=mun/1000,silver=mun%1000/100,copper=mun%100;
        return (gold>0?gold+"금량 ":"")+(silver>0?silver+"은량 ":"")+copper+"문";
    }
    public static int allowance(String rank){
        return switch(rank){case "장로"->850;case "사부"->420;case "내문제자"->180;default->75;};
    }
    public static double theftChance(double agility,double technique,double sense,int clicks,int ownRealm,int victimRealm){
        double result=.13+Math.max(0,agility-10)*.010+Math.max(0,technique-10)*.010+
            Math.max(0,sense-10)*.006+Math.min(30,Math.max(0,clicks))*.024
            -Math.max(0,victimRealm-ownRealm)*.135;
        return Math.max(.04,Math.min(.92,result));
    }
    public static int pocketReward(int realm,boolean merchant,int seed){
        int base=merchant?80:18;int bonus=merchant?125:44;
        return base+Math.max(0,realm)*Math.max(0,realm)*bonus+Math.floorMod(seed,merchant?100:55);
    }
    public static int killLoot(int realm,boolean merchant,boolean villain,int seed){
        int base=merchant?120:35;
        if(villain)base+=80;
        return base+Math.max(0,realm)*Math.max(0,realm)*(villain?140:90)+Math.floorMod(seed,130);
    }
}
