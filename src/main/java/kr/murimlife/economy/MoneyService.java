package kr.murimlife.economy;

import kr.murimlife.MurimProgressData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Backwards-compatible: Profile.silver remains legacy silver taels; copper adds change. */
public final class MoneyService {
    private MoneyService(){}
    public static long balance(MurimProgressData.Profile p){return CurrencyRules.total(p.silver,p.copper);}
    public static boolean change(MurimProgressData data,MurimProgressData.Profile p,long delta){
        long before=balance(p),next=before+delta;
        if((delta>0 && next<before)||next<0||next>Integer.MAX_VALUE*100L)return false;
        p.silver=(int)(next/100);p.copper=(int)(next%100);data.setDirty();return true;
    }
    public static void message(ServerPlayer player,String note){player.sendSystemMessage(Component.literal("[강호 경제] "+note));}
    public static void add(ServerPlayer p,long mun,String reason){
        if(mun<=0)return;
        var data=MurimProgressData.get(p.server);var pr=data.player(p.getUUID(),p.getName().getString());
        if(change(data,pr,mun))message(p,reason+" · +"+CurrencyRules.describe(mun)+" (잔고 "+CurrencyRules.describe(balance(pr))+")");
    }
    public static boolean pay(ServerPlayer p,long mun,String reason){
        if(mun<0)return false;
        var data=MurimProgressData.get(p.server);var pr=data.player(p.getUUID(),p.getName().getString());
        if(!change(data,pr,-mun)){message(p,"금전이 부족합니다. 필요: "+CurrencyRules.describe(mun));return false;}
        message(p,reason+" · -"+CurrencyRules.describe(mun));return true;
    }
}
