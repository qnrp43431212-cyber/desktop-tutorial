package kr.murimlife.economy;

import kr.murimlife.MurimProgressData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Three-day payroll; individual arrears survive logout, quitting a sect and server restarts. */
public final class SectPayroll extends SavedData {
    private final Map<String,Long> treasury=new HashMap<>();
    private long lastProcessedDay=-1;
    private static long checkTicks;
    private SectPayroll(){}
    private static SectPayroll load(CompoundTag tag,HolderLookup.Provider provider){
        SectPayroll p=new SectPayroll();p.lastProcessedDay=tag.contains("LastProcessedDay")?tag.getLong("LastProcessedDay"):-1;
        var map=tag.getCompound("Treasuries");for(String key:map.getAllKeys())p.treasury.put(key,Math.max(0,map.getLong(key)));
        return p;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        tag.putLong("LastProcessedDay",lastProcessedDay);
        var t=new CompoundTag();treasury.forEach(t::putLong);tag.put("Treasuries",t);return tag;
    }
    public static SectPayroll get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(SectPayroll::new,SectPayroll::load),"murimlife_sect_payroll_v123");}
    public long funds(String sect){return treasury.getOrDefault(sect,0L);}
    public void deposit(String sect,long amount){
        if(amount<=0||sect.isBlank())return;
        long old=funds(sect);if(Long.MAX_VALUE-old<amount)return;
        treasury.put(sect,old+amount);setDirty();
    }
    /** Claimed money is never fabricated: only available treasury funds are removed. */
    public long withdrawUpTo(String sect,long amount){
        long paid=Math.min(Math.max(0,amount),funds(sect));
        if(paid>0){treasury.put(sect,funds(sect)-paid);setDirty();}return paid;
    }
    private static MurimProgressData.Profile owner(String sect,Collection<MurimProgressData.Profile> profiles){
        for(var p:profiles)if(sect.equals(p.foundedSect))return p;return null;
    }
    private static GovernanceRules.Alignment alignment(String sect,MurimProgressData.Profile owner){
        return GovernanceRules.alignment(owner==null?sect:owner.sectAlignment);
    }
    private void cycle(MinecraftServer server,long payday){
        MurimProgressData data=MurimProgressData.get(server);var profiles=data.playerProfiles();
        for(var entry:profiles.entrySet()){
            var member=entry.getValue();String sect=member.sect;
            if(sect.isBlank()||"무소속".equals(sect)||!member.foundedSect.isBlank()||member.lastStipendDay>=payday)continue;
            var founder=owner(sect,profiles.values());
            if(GovernanceRules.exempt(alignment(sect,founder))){member.lastStipendDay=payday;data.setDirty();continue;}
            long newWage=CurrencyRules.allowance(member.sectRank);
            long previous=member.wageClaims.getOrDefault(sect,0L);
            long owed=previous>Long.MAX_VALUE-newWage?Long.MAX_VALUE:previous+newWage;
            long budget=founder==null?owed:funds(sect);
            long paid=Math.min(owed,budget);
            // Credit first so a full wallet cannot swallow the treasury money.
            if(paid>0&&!MoneyService.change(data,member,paid))paid=0;
            if(paid>0 && founder!=null)withdrawUpTo(sect,paid);
            long leftover=owed-paid;
            if(leftover>0){
                if(member.wageFirstMissedDay<0)member.wageFirstMissedDay=payday;
                member.loyalty=GovernanceRules.loyalty(member.loyalty,1);
                member.wageClaims.put(sect,leftover);
            }else{
                member.wageFirstMissedDay=-1;
                member.loyalty=Math.min(100,member.loyalty+2);
                member.wageClaims.remove(sect);
            }
            member.wageDebt=leftover;member.wageCreditorSect=leftover>0?sect:"";
            member.lastStipendDay=payday;data.setDirty();
            if(leftover>0 && founder!=null){
                var recipient=server.getPlayerList().getPlayer(entry.getKey());
                if(recipient!=null)recipient.sendSystemMessage(Component.literal("[문파] 급여 일부 또는 전액 체불: "+CurrencyRules.describe(leftover)+" / 충성도 "+member.loyalty));
            }
        }
        NpcSectRoster.get(server).payday(server,this,payday);
    }
    private void checkOverdue(MinecraftServer server,long today){
        var data=MurimProgressData.get(server);var profiles=data.playerProfiles();
        for(var entry:profiles.entrySet()){
            var member=entry.getValue();String sect=member.sect;
            if(sect.isBlank()||"무소속".equals(sect)||!member.foundedSect.isBlank() ||
                !GovernanceRules.overdue(member.wageFirstMissedDay,today))continue;
            var founder=owner(sect,profiles.values());if(founder==null)continue;
            var alignment=alignment(sect,founder);if(GovernanceRules.exempt(alignment))continue;
            long amount=member.wageClaims.getOrDefault(sect,0L);
            if(amount<=0)continue;
            // Seven days unpaid -> leave. Claim is still stored with original sect, including if re-employed.
            member.sect="무소속";member.sectRank="외문제자";member.wageFirstMissedDay=-1;
            member.loyalty=Math.max(0,member.loyalty-25);data.setDirty();
            if(alignment==GovernanceRules.Alignment.UNORTHODOX){
                long seizure=GovernanceRules.recoverable(amount);
                long fromTreasury=withdrawUpTo(sect,seizure);
                long fromOwner=Math.max(0,seizure-fromTreasury);
                long available=MoneyService.balance(founder);
                fromOwner=Math.min(available,fromOwner);
                if(fromOwner>0&&!MoneyService.change(data,founder,-fromOwner))fromOwner=0;
                long recovered=fromTreasury+fromOwner;
                if(recovered>0 && MoneyService.change(data,member,recovered)){
                    long remaining=amount-recovered;
                    if(remaining==0)member.wageClaims.remove(sect);
                    else member.wageClaims.put(sect,remaining);
                    member.wageDebt=remaining;data.setDirty();
                }else if(recovered>0){
                    // Exceptional full wallet: put seized money back into sect treasury rather than losing it.
                    if(fromTreasury>0)deposit(sect,fromTreasury);
                    if(fromOwner>0)MoneyService.change(data,founder,fromOwner);
                }
                founder.reputation=GovernanceRules.reputation(founder.reputation,-12);data.setDirty();
            }else if(!founder.righteousLicenseRevoked){
                SectDiscipline.get(server).openCase(server,sect,founder,entry.getKey(),today);
            }
            var online=server.getPlayerList().getPlayer(entry.getKey());
            if(online!=null)MoneyService.message(online,"7일 이상 급여를 받지 못해 문파를 탈퇴했습니다. 체불액 채권은 저장됩니다.");
        }
        NpcSectRoster.get(server).checkOverdue(server,this,today);
    }
    public static void tick(ServerTickEvent.Post event){
        if(++checkTicks%200!=0)return;
        var server=event.getServer();long today=server.overworld().getGameTime()/24000L;
        SectPayroll payroll=get(server);
        long payday=(today/3)*3;
        if(payday>=3&&payroll.lastProcessedDay<payday){
            // v1.23 -> v1.24 migration: no artificial years of retrospective payroll on first load.
            if(payroll.lastProcessedDay<0)payroll.lastProcessedDay=Math.max(0,payday-3);
            long steps=0;
            for(long d=payroll.lastProcessedDay+3;d<=payday&&steps<64;d+=3,steps++)payroll.cycle(server,d);
            payroll.lastProcessedDay=payday;payroll.setDirty();
        }
        payroll.checkOverdue(server,today);
    }
}
