package kr.murimlife.cult;

import kr.murimlife.CultWorldData;
import kr.murimlife.MurimProgressData;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.MurimNpcTypes;
import kr.murimlife.npc.NpcResidentService;
import kr.murimlife.npc.combat.WeaponTactics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import java.util.*;

/** World-saved demon succession. Individual names/arts/skin indices are never reissued. */
public final class CultRoster extends SavedData {
    private static final String KEY="murimlife_cult_roster_v125";
    public static final class SeatMember {
        public String id;
        public UUID entity;
        SeatMember(String id,UUID entity){this.id=id;this.entity=entity;}
    }
    private final Map<String,SeatMember> seats=new LinkedHashMap<>();
    private int nextDemon=0,nextBlood=0;
    private long deaths=0;
    private CultRoster(){
        for(String cult:List.of("마교","혈교"))for(int i=0;i<12;i++)
            seats.put(key(cult,i),new SeatMember(CultDesign.initial(cult,i).id(),null));
    }
    private static String key(String cult,int index){return cult+":"+index;}
    private static CultRoster load(CompoundTag tag,HolderLookup.Provider provider){
        CultRoster ledger=new CultRoster();
        if(!tag.contains("Members",Tag.TAG_LIST))return ledger;
        ledger.seats.clear();
        for(var t:tag.getList("Members",Tag.TAG_COMPOUND)){
            var n=(CompoundTag)t;String cult=n.getString("Cult"),id=n.getString("Id");int slot=n.getInt("Seat");
            CultDesign.Person person=CultDesign.get(id);
            if(person==null||!person.cult().equals(cult)||slot<0||slot>=12)continue;
            UUID entity=null;
            if(n.hasUUID("Entity"))entity=n.getUUID("Entity");
            ledger.seats.putIfAbsent(key(cult,slot),new SeatMember(id,entity));
        }
        ledger.nextDemon=Math.clamp(tag.getInt("NextDemon"),0,250);
        ledger.nextBlood=Math.clamp(tag.getInt("NextBlood"),0,250);
        ledger.deaths=Math.max(0,tag.getLong("Deaths"));
        return ledger;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var list=new ListTag();
        for(var e:seats.entrySet()){
            var parts=e.getKey().split(":");var n=new CompoundTag();
            n.putString("Cult",parts[0]);n.putInt("Seat",Integer.parseInt(parts[1]));
            n.putString("Id",e.getValue().id);
            if(e.getValue().entity!=null)n.putUUID("Entity",e.getValue().entity);
            list.add(n);
        }
        tag.put("Members",list);tag.putInt("NextDemon",nextDemon);
        tag.putInt("NextBlood",nextBlood);tag.putLong("Deaths",deaths);tag.putInt("Schema",1);
        return tag;
    }
    public static CultRoster get(MinecraftServer server){
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(CultRoster::new,CultRoster::load),KEY);
    }
    private static boolean exists(MinecraftServer server,String cult){
        var world=CultWorldData.get(server);
        return "마교".equals(cult)?world.demonExists():"혈교".equals(cult)&&world.bloodExists();
    }
    public int active(String cult){int count=0;for(int i=0;i<12;i++)if(seats.containsKey(key(cult,i)))count++;return count;}
    public int remaining(String cult){return Math.max(0,250-("마교".equals(cult)?nextDemon:nextBlood));}
    public String report(MinecraftServer server,String cult){
        if(!List.of("마교","혈교").contains(cult))return "마교 또는 혈교를 입력하세요.";
        if(!exists(server,cult))return cult+"은(는) 현재 월드에 존재하지 않습니다.";
        return cult+" 현재 대마두 직위 "+active(cult)+"/12석 | 앞으로 생성할 NPC의 미사용 이름 자원 "+remaining(cult)+"개 | 처치 누계 "+deaths;
    }
    private static int position(String cult,String id,Map<String,SeatMember> seats){
        for(int i=0;i<12;i++){
            var member=seats.get(key(cult,i));if(member!=null&&member.id.equals(id))return i;
        }return -1;
    }
    private void promoteAfterDeath(MinecraftServer server,String cult,int vacancy){
        int[] realms=new int[12];Arrays.fill(realms,-1);
        for(int i=vacancy+1;i<12;i++){
            var existing=seats.get(key(cult,i));
            if(existing!=null)realms[i]=CultDesign.get(existing.id).realm();
        }
        int source=CultRankRules.qualifiedSuccessor(vacancy,realms);
        if(source>=0){
            SeatMember successor=seats.remove(key(cult,source));
            seats.put(key(cult,vacancy),successor);
            if(successor.entity!=null){
                var entity=server.overworld().getEntity(successor.entity);
                if(entity instanceof MurimHumanoid npc)apply(npc,successor,vacancy);
            }
            promoteAfterDeath(server,cult,source);
        }else if(vacancy==11){
            // Reinforcements enter at the lowest rank only, never materialise as unearned senior masters.
            int index="마교".equals(cult)?nextDemon:nextBlood;
            CultDesign.Person recruit=CultDesign.reserve(cult,index);
            if(recruit!=null&&recruit.realm()>=CultRankRules.seatMinimum(11)){
                seats.put(key(cult,11),new SeatMember(recruit.id(),null));
                if("마교".equals(cult))nextDemon++;else nextBlood++;
            }
        }
    }
    /** Only actual server-authoritative death of the currently registered entity triggers succession. */
    public static void onDeath(LivingDeathEvent event){
        if(!(event.getEntity() instanceof MurimHumanoid npc)||npc.getServer()==null)return;
        String id=npc.getPersistentData().getString("MurimCultPersonId");
        CultDesign.Person person=CultDesign.get(id);
        if(person==null)return;
        MinecraftServer server=npc.getServer();CultRoster data=get(server);
        int seat=position(person.cult(),id,data.seats);if(seat<0)return;
        var member=data.seats.get(key(person.cult(),seat));
        if(member.entity==null||!member.entity.equals(npc.getUUID()))return; // guards dupes/fake tags
        data.seats.remove(key(person.cult(),seat));data.deaths++;
        data.promoteAfterDeath(server,person.cult(),seat);data.setDirty();
        if(event.getSource().getEntity() instanceof ServerPlayer victor){
            var saved=MurimProgressData.get(server);
            var player=saved.player(victor.getUUID(),victor.getName().getString());
            player.counters.merge("cultManual:"+person.art(),1,Integer::sum);
            saved.setDirty();
            victor.sendSystemMessage(Component.literal("[대마두 토벌] "+person.name()+" 처치. "+person.art()+"의 비결 단서를 얻었습니다. " +
                "수련·경지 조건을 충족하면 /murim learn 로 습득할 수 있습니다. 교단은 존속하며 하위 마두가 승계합니다."));
        }
    }
    public static void onJoin(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel world)||!(event.getEntity() instanceof MurimHumanoid npc))return;
        String id=npc.getPersistentData().getString("MurimCultPersonId");
        var person=CultDesign.get(id);if(person==null)return;
        var data=get(world.getServer());int seat=position(person.cult(),id,data.seats);
        if(seat<0){npc.discard();return;}
        var current=data.seats.get(key(person.cult(),seat));
        if(current.entity!=null&&!npc.getUUID().equals(current.entity)){npc.discard();return;}
        if(current.entity==null){current.entity=npc.getUUID();data.setDirty();}
        apply(npc,current,seat);
    }
    private static void apply(MurimHumanoid npc,SeatMember member,int seat){
        CultDesign.Person person=CultDesign.get(member.id);
        if(person==null)return;
        npc.setSkin(person.skin());
        npc.setCustomName(Component.literal("["+person.cult()+"·"+CultDesign.title(seat)+"] "+person.name()));
        npc.setCustomNameVisible(true);
        npc.setPersistenceRequired();
        var tag=npc.getPersistentData();
        tag.putString("MurimCultPersonId",person.id());tag.putString("MurimCult",person.cult());
        tag.putInt("MurimCultSeat",seat);tag.putBoolean("MurimEvil",true);
        // Named cult leaders usually permit approach to recruit; they still retaliate when harmed.
        tag.putBoolean("MurimCultHostile",tag.hasUUID("MurimCultProvokedPlayer"));tag.putString("MurimMartialArt",person.art());
        tag.putString("MurimRealm",kr.murimlife.v16.RealmRules.REALMS[person.realm()]);
        tag.putInt("MurimRealmIndex",person.realm());
        tag.putInt("MurimCultRealmIndex",person.realm());
        if(!tag.contains("MurimPocketMoney"))tag.putInt("MurimPocketMoney",1800+200*seat+300*person.realm());
        npc.setWeaponStyle(switch(person.weapon()){
            case "dao"->WeaponTactics.Style.SABER;
            case "claw"->WeaponTactics.Style.DAGGER;
            case "staff"->WeaponTactics.Style.STAFF;
            case "gauntlet","unarmed"->WeaponTactics.Style.FIST;
            default->WeaponTactics.Style.SWORD;});
        double maxHealth=90+person.realm()*18+(12-seat)*5;
        var hp=npc.getAttribute(Attributes.MAX_HEALTH);if(hp!=null)hp.setBaseValue(maxHealth);
        var attack=npc.getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null)attack.setBaseValue(8+person.realm()*2+(12-seat)*.45);
        var armor=npc.getAttribute(Attributes.ARMOR);if(armor!=null)armor.setBaseValue(6+person.realm());
        npc.setHealth(Math.min(npc.getHealth(),npc.getMaxHealth()));
    }
    /** Admin test spawn. Does not force-load chunks or duplicate an entity that is already assigned. */
    public String spawnOne(ServerPlayer player,String cult){
        if(!List.of("마교","혈교").contains(cult)||!exists(player.server,cult))return "해당 교단이 현재 월드에 없습니다.";
        if(player.serverLevel()!=player.server.overworld())return "오버월드에서만 생성할 수 있습니다.";
        for(int seat=0;seat<12;seat++){
            var current=seats.get(key(cult,seat));if(current==null||current.entity!=null)continue;
            BlockPos pos=null;
            for(int dx=4;dx<=10&&pos==null;dx++)for(int dz=-5;dz<=5;dz++){
                BlockPos candidate=player.blockPosition().offset(dx,0,dz);
                if(NpcResidentService.clearance(player.serverLevel(),candidate)){pos=candidate;break;}
            }
            if(pos==null)return "근처에 NPC가 설 수 있는 안전한 공간이 없습니다.";
            MurimHumanoid npc=new MurimHumanoid(MurimNpcTypes.HUMAN.get(),player.serverLevel());
            npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
            // Assign the entity UUID before server join hooks check the roster.
            current.entity=npc.getUUID();apply(npc,current,seat);
            npc.setHealth(npc.getMaxHealth());
            boolean ok=player.serverLevel().addFreshEntity(npc);
            if(!ok){current.entity=null;return "NPC 등록 실패 (엔티티 ID 또는 위치 충돌).";}
            setDirty();
            return cult+" "+CultDesign.title(seat)+" "+CultDesign.get(current.id).name()+" 생성 성공 (전용 스킨·사공).";
        }
        return "이미 등록된 대마두가 모두 생성되었거나 후계 명부가 소진되었습니다.";
    }
}
