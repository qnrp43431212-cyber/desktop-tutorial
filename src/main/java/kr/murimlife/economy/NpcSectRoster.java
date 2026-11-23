package kr.murimlife.economy;

import kr.murimlife.MurimProgressData;
import kr.murimlife.npc.MurimHumanoid;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.util.*;

/** Player-founded NPC members are accounted for even when their chunk is unloaded. */
public final class NpcSectRoster extends SavedData {
    public static final class Contract {
        UUID entity;String sect,rank;int loyalty=100;long wallet,lastStipendDay=-1,firstMissedDay=-1;
        boolean deceased;Map<String,Long> claims=new HashMap<>();
        Contract(UUID id,String sect,int pocket){entity=id;this.sect=sect;rank="외문제자";wallet=Math.max(0,pocket);}
    }
    private final Map<UUID,Contract> roster=new HashMap<>();
    private NpcSectRoster(){}
    private static NpcSectRoster load(CompoundTag tag,HolderLookup.Provider provider){
        var out=new NpcSectRoster();var list=tag.getList("Contracts",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)try{
            var t=list.getCompound(i);UUID id=UUID.fromString(t.getString("Uuid"));
            var c=new Contract(id,t.getString("Sect"),t.getInt("Pocket"));c.rank=t.getString("Rank");
            c.wallet=Math.max(0,t.getLong("Wallet"));c.loyalty=Math.clamp(t.getInt("Loyalty"),0,100);
            c.deceased=t.getBoolean("Deceased");c.lastStipendDay=t.contains("LastStipendDay")?t.getLong("LastStipendDay"):-1;
            c.firstMissedDay=t.contains("FirstMissedDay")?t.getLong("FirstMissedDay"):-1;
            var claims=t.getCompound("Claims");for(String name:claims.getAllKeys())c.claims.put(name,Math.max(0,claims.getLong(name)));
            out.roster.putIfAbsent(id,c);
        }catch(IllegalArgumentException ignored){}return out;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var list=new ListTag();for(var c:roster.values()){
            var t=new CompoundTag();t.putString("Uuid",c.entity.toString());t.putString("Sect",c.sect);t.putString("Rank",c.rank);
            t.putLong("Wallet",c.wallet);t.putInt("Loyalty",c.loyalty);t.putLong("LastStipendDay",c.lastStipendDay);
            t.putLong("FirstMissedDay",c.firstMissedDay);t.putBoolean("Deceased",c.deceased);
            var claims=new CompoundTag();c.claims.forEach(claims::putLong);t.put("Claims",claims);list.add(t);
        }tag.put("Contracts",list);return tag;
    }
    public static NpcSectRoster get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(NpcSectRoster::new,NpcSectRoster::load),"murimlife_npc_sect_roster_v124");}
    public boolean encumbered(String sect){return roster.values().stream().anyMatch(c->
        !c.deceased&&(sect.equals(c.sect)||c.claims.getOrDefault(sect,0L)>0));}
    public int active(String sect){return (int)roster.values().stream().filter(c->sect.equals(c.sect)&&!c.deceased).count();}
    public long unpaid(String sect){long total=0;for(var c:roster.values())total+=c.claims.getOrDefault(sect,0L);return total;}
    private static MurimProgressData.Profile founder(MinecraftServer server,String sect){
        for(var p:MurimProgressData.get(server).playerProfiles().values())if(sect.equals(p.foundedSect))return p;
        return null;
    }
    /** Called only via NPC ~ chat and the server-verified in-range session. */
    public void recruit(ServerPlayer owner,MurimHumanoid npc){
        var data=MurimProgressData.get(owner.server);var p=data.player(owner.getUUID(),owner.getName().getString());
        if(p.foundedSect.isBlank()){MoneyService.message(owner,"문파 가주만 NPC 제자에게 가입을 제안할 수 있습니다.");return;}
        var tag=npc.getPersistentData();
        if(!npc.isAlive()||npc.level()!=owner.level()||npc.distanceToSqr(owner)>36||
            tag.getBoolean("MurimUniqueLeader")||tag.getBoolean("MurimOpportunityEnemy")||tag.getBoolean("MurimCultHostile")||
            tag.getBoolean("MurimLifeSpectator")||tag.getBoolean("MurimArenaFighter")||
            roster.containsKey(npc.getUUID())){
            MoneyService.message(owner,"이 NPC는 가입 제안을 받을 수 없거나 이미 다른 문파와 계약했습니다.");return;
        }
        boolean affiliated=!tag.getString("MurimNpcSite").isBlank();
        int chance=Math.clamp(55+p.reputation/20-(affiliated?30:0),10,90);
        if(owner.getRandom().nextInt(100)>=chance){MoneyService.message(owner,"상대가 제안을 거절했습니다. 신뢰를 쌓거나 다른 사람을 찾아보십시오.");return;}
        var c=new Contract(npc.getUUID(),p.foundedSect,TheftService.pocket(npc));
        roster.put(c.entity,c);setDirty();applyToLoaded(owner.server,c);
        MoneyService.message(owner,"새 NPC 문파원 영입: "+npc.getDisplayName().getString()+
            " / 외문제자 / 3일마다 "+CurrencyRules.describe(CurrencyRules.allowance(c.rank)));
    }
    public void setRank(ServerPlayer owner,MurimHumanoid npc,String rank){
        var profile=MurimProgressData.get(owner.server).player(owner.getUUID(),owner.getName().getString());
        Contract c=roster.get(npc.getUUID());
        if(c==null||!c.sect.equals(profile.foundedSect)||npc.distanceToSqr(owner)>36||
            !Set.of("외문제자","내문제자","사부","장로").contains(rank)){
            MoneyService.message(owner,"6블록 안의 소속 NPC만 승급할 수 있습니다.");return;
        }
        int realm=npc.getPersistentData().getInt("MurimRealmIndex");
        if(!kr.murimlife.cult.CultRankRules.eligible(rank,realm)){
            MoneyService.message(owner,"이 NPC의 경지("+realm+")로는 "+rank+" 직위에 오를 수 없습니다.");return;}
        c.rank=rank;setDirty();applyToLoaded(owner.server,c);
        MoneyService.message(owner,"NPC 직급 변경: "+rank);
    }
    /** Founder-only rank progression: outer -> inner -> instructor -> elder. */
    public void promote(ServerPlayer owner,MurimHumanoid npc){
        var profile=MurimProgressData.get(owner.server).player(owner.getUUID(),owner.getName().getString());
        var c=roster.get(npc.getUUID());
        if(c==null||profile.foundedSect.isBlank()||!profile.foundedSect.equals(c.sect)||
            npc.level()!=owner.level()||npc.distanceToSqr(owner)>36){
            MoneyService.message(owner,"자신의 문파에 가입한 NPC와 6블록 안에서만 승급시킬 수 있습니다.");return;
        }
        String next=switch(c.rank){case "외문제자"->"내문제자";case "내문제자"->"사부";
            case "사부"->"장로";default->"";};
        if(next.isBlank()){MoneyService.message(owner,"이미 장로입니다.");return;}
        setRank(owner,npc,next);
    }
    /** Founder may release a fully paid NPC; debts are never discarded to evade liability. */
    public void release(ServerPlayer owner,MurimHumanoid npc){
        var profile=MurimProgressData.get(owner.server).player(owner.getUUID(),owner.getName().getString());
        var c=roster.get(npc.getUUID());
        if(c==null||profile.foundedSect.isBlank()||!profile.foundedSect.equals(c.sect)||
            npc.level()!=owner.level()||npc.distanceToSqr(owner)>36){
            MoneyService.message(owner,"자신의 문파 소속 NPC에게만 해임을 제안할 수 있습니다.");return;
        }
        if(c.claims.getOrDefault(c.sect,0L)>0){
            MoneyService.message(owner,"급여 체불이 남아 있습니다. 문파 금고에서 먼저 지급해야 해임할 수 있습니다.");return;
        }
        c.sect="";c.firstMissedDay=-1;setDirty();applyToEntity(npc,c);
        MoneyService.message(owner,"NPC가 문파를 떠났습니다. 이후 급여는 발생하지 않습니다.");
    }
    /** Syncs saved wages and loyalty to the entity without forcing its chunk to stay loaded. */
    private static void applyToEntity(MurimHumanoid npc,Contract c){
        var tag=npc.getPersistentData();
        if(c.sect.isBlank()||c.deceased){tag.remove("MurimPlayerSect");tag.remove("MurimPlayerSectRank");}
        else{tag.putString("MurimPlayerSect",c.sect);tag.putString("MurimPlayerSectRank",c.rank);}
        tag.putInt("MurimPlayerSectLoyalty",c.loyalty);
        tag.putInt("MurimPocketMoney",(int)Math.min(Integer.MAX_VALUE,c.wallet));
    }
    private static void applyToLoaded(MinecraftServer server,Contract c){
        var entity=server.overworld().getEntity(c.entity);
        if(entity instanceof MurimHumanoid npc)applyToEntity(npc,c);
    }
    public static void onEntityLoad(MurimHumanoid npc){
        if(npc.level().isClientSide()||npc.getServer()==null)return;
        var c=get(npc.getServer()).roster.get(npc.getUUID());
        if(c!=null)applyToEntity(npc,c);
    }
    public void payday(MinecraftServer server,SectPayroll payroll,long day){
        for(var c:roster.values()){
            if(c.sect.isBlank()||c.deceased||c.lastStipendDay>=day)continue;
            var owner=founder(server,c.sect);if(owner==null)continue;
            if(GovernanceRules.exempt(GovernanceRules.alignment(owner.sectAlignment))){
                c.lastStipendDay=day;setDirty();continue;
            }
            long old=c.claims.getOrDefault(c.sect,0L),newWage=CurrencyRules.allowance(c.rank);
            long owed=old>Long.MAX_VALUE-newWage?Long.MAX_VALUE:old+newWage;
            long available=Math.max(0,Integer.MAX_VALUE-c.wallet);
            long paid=Math.min(available,Math.min(owed,payroll.funds(c.sect)));
            if(paid>0){payroll.withdrawUpTo(c.sect,paid);c.wallet+=paid;}
            long remaining=owed-paid;
            if(remaining>0){c.claims.put(c.sect,remaining);c.loyalty=GovernanceRules.loyalty(c.loyalty,1);
                if(c.firstMissedDay<0)c.firstMissedDay=day;
            }else{c.claims.remove(c.sect);c.loyalty=Math.min(100,c.loyalty+2);c.firstMissedDay=-1;}
            c.lastStipendDay=day;setDirty();applyToLoaded(server,c);
        }
    }
    public void checkOverdue(MinecraftServer server,SectPayroll payroll,long day){
        for(var c:roster.values()){
            if(c.sect.isBlank()||c.deceased||!GovernanceRules.overdue(c.firstMissedDay,day))continue;
            var owner=founder(server,c.sect);if(owner==null)continue;
            String sect=c.sect;
            var a=GovernanceRules.alignment(owner.sectAlignment);if(GovernanceRules.exempt(a))continue;
            long due=c.claims.getOrDefault(sect,0L);
            if(due<=0)continue;
            c.sect="";c.firstMissedDay=-1;c.loyalty=Math.max(0,c.loyalty-25);
            if(a==GovernanceRules.Alignment.UNORTHODOX){
                long seizure=Math.min(GovernanceRules.recoverable(due),Math.max(0,Integer.MAX_VALUE-c.wallet));
                long fromTreasury=payroll.withdrawUpTo(sect,seizure);
                long amountFromOwner=Math.min(MoneyService.balance(owner),seizure-fromTreasury);
                if(amountFromOwner>0&&!MoneyService.change(MurimProgressData.get(server),owner,-amountFromOwner))amountFromOwner=0;
                long recovered=fromTreasury+amountFromOwner;
                c.wallet=Math.min(Integer.MAX_VALUE,c.wallet+recovered);
                long remainder=due-recovered;
                if(remainder<=0)c.claims.remove(sect);else c.claims.put(sect,remainder);
                owner.reputation=GovernanceRules.reputation(owner.reputation,-12);
                MurimProgressData.get(server).setDirty();
            }else if(!owner.righteousLicenseRevoked){
                SectDiscipline.get(server).openCase(server,sect,owner,c.entity,day);
            }
            setDirty();applyToLoaded(server,c);
        }
    }
    public long reimburse(MinecraftServer server,String sect){
        long total=0;
        for(var c:roster.values()){
            long debt=c.claims.getOrDefault(sect,0L);if(debt<=0)continue;
            long eligible=Math.max(0,Integer.MAX_VALUE-c.wallet);
            long paid=Math.min(debt,eligible);
            c.wallet+=paid;total+=paid;
            if(paid==debt)c.claims.remove(sect);else c.claims.put(sect,debt-paid);
            c.loyalty=Math.min(100,c.loyalty+20);setDirty();applyToLoaded(server,c);
        }
        return total;
    }
    public int desert(MinecraftServer server,String sect){
        var candidates=new ArrayList<Contract>();for(var c:roster.values())if(!c.deceased&&sect.equals(c.sect))candidates.add(c);
        candidates.sort(Comparator.comparing(c->c.entity.toString()));int count=GovernanceRules.desertionQuota(candidates.size());
        for(int i=0;i<count;i++){
            var c=candidates.get(i);c.sect="";c.loyalty=Math.max(0,c.loyalty-35);c.firstMissedDay=-1;
            setDirty();applyToLoaded(server,c);
        }
        return count;
    }
    public static void onDeath(LivingDeathEvent event){
        if(!(event.getEntity() instanceof MurimHumanoid npc)||npc.level().isClientSide())return;
        var data=get(npc.getServer());
        var c=data.roster.get(npc.getUUID());
        if(c!=null){c.deceased=true;c.sect="";data.setDirty();}
    }
}
