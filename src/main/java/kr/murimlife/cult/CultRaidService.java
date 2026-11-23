package kr.murimlife.cult;

import kr.murimlife.CultWorldData;
import kr.murimlife.MurimProgressData;
import kr.murimlife.economy.SectDiscipline;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.MurimNpcTypes;
import kr.murimlife.npc.NpcResidentService;
import kr.murimlife.npc.combat.WeaponTactics;
import kr.murimlife.worldgen.SiteLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Bounded, server-spawned cult incursions. Kill credit, site target and cooldown persist across restarts. */
public final class CultRaidService extends SavedData {
    public static final class Raid {
        UUID id,owner;String cult,goal;
        long started,deadline;int killed,total;
        final Set<UUID> survivors=new HashSet<>();
        Raid(UUID id,UUID owner,String cult,String goal,long started,long deadline){
            this.id=id;this.owner=owner;this.cult=cult;this.goal=goal;this.started=started;this.deadline=deadline;
        }
    }
    private static final String KEY="murimlife_cult_raids_v126";
    private final Map<UUID,Raid> active=new HashMap<>();
    private final Map<UUID,Long> lastDay=new HashMap<>();
    private static long ticks;
    private CultRaidService(){}
    private static CultRaidService load(CompoundTag t,HolderLookup.Provider provider){
        var out=new CultRaidService();
        for(var key:t.getCompound("LastDay").getAllKeys())try{out.lastDay.put(UUID.fromString(key),t.getCompound("LastDay").getLong(key));}catch(IllegalArgumentException ignored){}
        for(var raw:t.getList("Raids",Tag.TAG_COMPOUND))try{
            var n=(CompoundTag)raw;var raid=new Raid(n.getUUID("Id"),n.getUUID("Owner"),n.getString("Cult"),n.getString("Goal"),n.getLong("Started"),n.getLong("Deadline"));
            raid.total=n.getInt("Total");raid.killed=n.getInt("Killed");
            for(var uuid:n.getList("Survivors",Tag.TAG_STRING))try{
                raid.survivors.add(UUID.fromString(uuid.getAsString()));
            }catch(IllegalArgumentException ignored){}
            if(raid.total>0&&raid.total<=8)out.active.put(raid.id,raid);
        }catch(Exception ignored){}return out;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var days=new CompoundTag();lastDay.forEach((id,day)->days.putLong(id.toString(),day));tag.put("LastDay",days);
        var list=new ListTag();for(Raid raid:active.values()){
            var n=new CompoundTag();n.putUUID("Id",raid.id);n.putUUID("Owner",raid.owner);
            n.putString("Cult",raid.cult);n.putString("Goal",raid.goal);n.putLong("Started",raid.started);
            n.putLong("Deadline",raid.deadline);n.putInt("Total",raid.total);n.putInt("Killed",raid.killed);
            // StringTag UUID list avoids Minecraft NBT long-array/endian ambiguities.
            var ids=new ListTag();for(UUID id:raid.survivors)ids.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
            n.put("Survivors",ids);list.add(n);
        }tag.put("Raids",list);return tag;
    }
    public static CultRaidService get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(CultRaidService::new,CultRaidService::load),KEY);}
    private static void say(ServerPlayer p,String s){p.sendSystemMessage(Component.literal("[마교·혈교 습격] "+s));}
    public static boolean hostileTo(MurimHumanoid npc,Player target){
        var tag=npc.getPersistentData();
        if(!tag.getBoolean("MurimCultHostile")&&!tag.getBoolean("MurimCultRaider"))return tag.getBoolean("MurimOpportunityEnemy");
        if(!(target instanceof ServerPlayer p))return false;
        if(tag.hasUUID("MurimRaidOwner")&&!tag.getUUID("MurimRaidOwner").equals(p.getUUID()))return false;
        if(tag.hasUUID("MurimCultProvokedPlayer")&&!tag.getUUID("MurimCultProvokedPlayer").equals(p.getUUID()))return false;
        String cult=tag.getString("MurimCult");
        var profile=MurimProgressData.get(p.server).player(p.getUUID(),p.getName().getString());
        return !cult.equals(profile.sect); // Do not attack the very cult members one is trying to recruit.
    }
    private String cultFor(ServerPlayer player){
        var state=CultWorldData.get(player.server);var available=new ArrayList<String>();
        if(state.demonExists())available.add("마교");if(state.bloodExists())available.add("혈교");
        if(available.isEmpty())return "";
        return available.get(player.getRandom().nextInt(available.size()));
    }
    public boolean activeFor(UUID player){return active.values().stream().anyMatch(r->r.owner.equals(player));}
    /** A corrective inspection can guarantee an incursion opportunity within its 15-day window. */
    public static void summonForAudit(ServerPlayer founder){
        var raid=get(founder.server);
        if(raid.activeFor(founder.getUUID()))return;
        raid.launch(founder,true);
    }
    public void launch(ServerPlayer owner,boolean audit){
        if(owner.serverLevel()!=owner.server.overworld()||owner.isSpectator()||!owner.isAlive()||activeFor(owner.getUUID())||active.size()>=12)return;
        var profile=MurimProgressData.get(owner.server).player(owner.getUUID(),owner.getName().getString());
        if("마교".equals(profile.sect)||"혈교".equals(profile.sect))return;
        String cult=cultFor(owner);if(cult.isBlank())return;
        long day=owner.server.overworld().getGameTime()/24000L;
        if(!audit && day-lastDay.getOrDefault(owner.getUUID(),-100L)<5)return;
        if(audit && day-lastDay.getOrDefault(owner.getUUID(),-100L)<2)return;
        int count=audit?4:3+owner.getRandom().nextInt(3);
        boolean nearSect=SiteLedger.get(owner.serverLevel()).jobs().stream().anyMatch(j->j.stage==SiteLedger.Stage.COMPLETE &&
            Math.abs(j.x-owner.getBlockX())<=70&&Math.abs(j.z-owner.getBlockZ())<=70);
        String goal=nearSect?"인근 문파 본부 습격과 점령 시도":"강호 길목 기습";
        UUID id=UUID.randomUUID();long now=owner.serverLevel().getGameTime();
        Raid raid=new Raid(id,owner.getUUID(),cult,goal,now,now+18000L);
        // Register first: EntityJoinLevelEvent validates the raid id during addFreshEntity().
        active.put(id,raid);
        for(int i=0;i<count;i++){
            BlockPos spawn=null;
            for(int n=0;n<16;n++){
                int dx=(owner.getRandom().nextBoolean()?1:-1)*(9+owner.getRandom().nextInt(9));
                int dz=(owner.getRandom().nextBoolean()?1:-1)*(5+owner.getRandom().nextInt(13));
                BlockPos candidate=owner.blockPosition().offset(dx,0,dz);
                if(NpcResidentService.clearance(owner.serverLevel(),candidate)){spawn=candidate;break;}
            }
            if(spawn==null)break;
            var mob=new MurimHumanoid(MurimNpcTypes.HUMAN.get(),owner.serverLevel());
            mob.moveTo(spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,owner.getRandom().nextFloat()*360f,0);
            mob.getPersistentData().putBoolean("MurimCultRaider",true);
            mob.getPersistentData().putBoolean("MurimCultHostile",true);
            mob.getPersistentData().putBoolean("MurimEvil",true);
            mob.getPersistentData().putString("MurimCult",cult);
            mob.getPersistentData().putUUID("MurimRaidOwner",owner.getUUID());
            mob.getPersistentData().putUUID("MurimRaidId",id);
            mob.getPersistentData().putLong("MurimRaidDeadline",raid.deadline);
            mob.getPersistentData().putInt("MurimRealmIndex",Math.clamp(profile.realm+1,1,5));
            // Raider skins use the generic pool, never the 524 reserved unique demon skins.
            mob.setSkin(346+Math.floorMod(Objects.hash(id,i,cult),96));
            mob.setWeaponStyle(i%2==0?WeaponTactics.Style.SABER:WeaponTactics.Style.SWORD);
            var hp=mob.getAttribute(Attributes.MAX_HEALTH);if(hp!=null)hp.setBaseValue(40+profile.realm*10);
            var attack=mob.getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null)attack.setBaseValue(6+profile.realm*2);
            mob.setHealth(mob.getMaxHealth());mob.setTarget(owner);
            mob.setCustomName(Component.literal("["+cult+" 침공대] "+(i+1)+"번 전투원"));mob.setCustomNameVisible(true);
            if(owner.serverLevel().addFreshEntity(mob))raid.survivors.add(mob.getUUID());
        }
        if(raid.survivors.isEmpty()){active.remove(id);return;}
        raid.total=raid.survivors.size();lastDay.put(owner.getUUID(),day);setDirty();
        say(owner,cult+" 전투원 "+raid.total+"명이 "+goal+"을 시작했습니다. 무고한 행인을 보호하고 습격군을 몰아내십시오. (남은 시간 15분)");
    }
    public static void onDeath(LivingDeathEvent event){
        if(!(event.getEntity() instanceof MurimHumanoid npc)||npc.getServer()==null)return;
        var tag=npc.getPersistentData();if(!tag.getBoolean("MurimCultRaider")||!tag.hasUUID("MurimRaidId"))return;
        var data=get(npc.getServer());Raid raid=data.active.get(tag.getUUID("MurimRaidId"));
        if(raid==null||!raid.survivors.remove(npc.getUUID()))return;
        raid.killed++;data.setDirty();
        if(event.getSource().getEntity() instanceof ServerPlayer attacker && attacker.getUUID().equals(raid.owner)){
            var progress=MurimProgressData.get(attacker.server);var pr=progress.player(attacker.getUUID(),attacker.getName().getString());
            pr.counters.merge("cultRaidKills",1,Integer::sum);progress.setDirty();
            SectDiscipline.get(attacker.server).raiderDefeated(attacker);
            say(attacker,raid.cult+" 습격대 격퇴 "+raid.killed+"/"+raid.total);
        }
        if(raid.survivors.isEmpty()){
            data.active.remove(raid.id);data.setDirty();
            ServerPlayer owner=npc.getServer().getPlayerList().getPlayer(raid.owner);
            if(owner!=null){say(owner,"습격 저지 성공! 본부 점령 시도를 물리쳤습니다.");
                SectDiscipline.get(owner.server).raidRepelled(owner);}
        }
    }
    public static void onJoin(EntityJoinLevelEvent event){
        if(!(event.getEntity() instanceof MurimHumanoid mob)||mob.getServer()==null)return;
        var t=mob.getPersistentData();if(!t.getBoolean("MurimCultRaider"))return;
        var state=get(mob.getServer());
        if(!t.hasUUID("MurimRaidId")||!state.active.containsKey(t.getUUID("MurimRaidId"))||
                mob.level().getGameTime()>t.getLong("MurimRaidDeadline"))mob.discard();
    }
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%200!=0)return;
        var server=event.getServer();var world=server.overworld();var service=get(server);long now=world.getGameTime();
        for(Raid raid:List.copyOf(service.active.values()))if(now>raid.deadline){
            for(UUID id:raid.survivors)if(world.getEntity(id) instanceof MurimHumanoid npc)npc.discard();
            service.active.remove(raid.id);service.setDirty();
            ServerPlayer owner=server.getPlayerList().getPlayer(raid.owner);
            if(owner!=null)say(owner,"습격군을 완전히 저지하지 못했습니다. 침공대가 물러났습니다.");
        }
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            if(p.serverLevel()!=world||service.activeFor(p.getUUID()))continue;
            var pr=MurimProgressData.get(server).player(p.getUUID(),p.getName().getString());
            if("마교".equals(pr.sect)||"혈교".equals(pr.sect))continue;
            long day=now/24000L;
            boolean audit= !pr.foundedSect.isBlank() && SectDiscipline.get(server).audit(pr.foundedSect)!=null;
            if(audit){
                if(day-service.lastDay.getOrDefault(p.getUUID(),-100L)>=2)service.launch(p,true);
            }else if(pr.realm>=2 && day-service.lastDay.getOrDefault(p.getUUID(),-100L)>=5 && p.getRandom().nextInt(500)==0){
                service.launch(p,false);
            }
        }
    }
}
