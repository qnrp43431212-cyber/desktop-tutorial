package kr.murimlife.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import kr.murimlife.worldgen.SiteLedger;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/** Persistent MurimHumanoid residents; they naturally save/unload with the world.
 *  Does not force chunks, kill/recreate NPCs, or make unloaded NPCs tick.
 */
public final class NpcResidentService {
    public record Candidate(String site,int piece,int index,int dx,int dy,int dz,String role,String zone){}
    private static NpcRuntimeConfig config;
    private static NpcNameCatalog names;
    private static Map<String,List<Candidate>> candidatesBySite=Map.of();
    private static int ticks;
    private static boolean initialized;
    private NpcResidentService(){}
    public static NpcRuntimeConfig config(){
        if(!initialized){
            initialized=true;
            config=NpcRuntimeConfig.read();
            if(config.enablePersistentResidents||config.enableTournamentDemo){
                names=NpcNameCatalog.load();
                Map<String,List<Candidate>> grouped=new java.util.HashMap<>();
                for(Candidate candidate:loadCandidates())
                    grouped.computeIfAbsent(candidate.site(),ignored->new ArrayList<>()).add(candidate);
                grouped.replaceAll((key,values)->List.copyOf(values));
                candidatesBySite=Map.copyOf(grouped);
            }
        }
        return config;
    }
    private static List<Candidate> loadCandidates(){
        try(var stream=NpcResidentService.class.getResourceAsStream("/murimnpc/npc_site_spawn_candidates.json")){
            if(stream==null)throw new IllegalStateException("Missing NPC spawn candidates");
            JsonObject root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            List<Candidate> result=new ArrayList<>();
            for(var node:root.getAsJsonArray("sites")){
                JsonObject site=node.getAsJsonObject();String sid=site.get("site_id").getAsString();
                for(var pieceNode:site.getAsJsonArray("pieces")){
                    JsonObject piece=pieceNode.getAsJsonObject();int pieceId=piece.get("piece_index").getAsInt();
                    String zone=piece.get("zone").getAsString();JsonArray roles=piece.getAsJsonArray("possible_roles");
                    String role=roles!=null&&roles.size()>0?roles.get(0).getAsString():"householder";
                    int n=0;for(var off:piece.getAsJsonArray("candidate_spawn_offsets")){
                        JsonArray xyz=off.getAsJsonArray();
                        result.add(new Candidate(sid,pieceId,n++,xyz.get(0).getAsInt(),
                                xyz.get(1).getAsInt(),xyz.get(2).getAsInt(),role,zone));
                    }
                }
            }
            return List.copyOf(result);
        }catch(Exception ex){throw new IllegalStateException("NPC spawn candidate failure",ex);}
    }
    /** Loaded count, not lifetime count: distant saved residents are not discarded or used as a global live cap. */
    public static int loadedResidentCount(ServerLevel world){
        int count=0;
        for(Entity entity:world.getAllEntities())
            if((entity instanceof MurimHumanoid || entity instanceof Villager) && entity.getPersistentData().getBoolean("MurimLifeResident"))count++;
        return count;
    }
    public static void onTick(ServerTickEvent.Post event){
        NpcRuntimeConfig cfg=config();
        if(!cfg.enablePersistentResidents)return;
        if(++ticks%cfg.residentSpawnIntervalTicks!=0)return;
        ServerLevel world=event.getServer().overworld();
        var ledger=NpcPopulationLedger.get(world);
        int loadedCount=loadedResidentCount(world);
        if(loadedCount>=cfg.residentGlobalCap)return;
        Set<String> usedNames=ledger.usedNames();
        for(SiteLedger.Job site:SiteLedger.get(world).jobs()){
            if(site.stage!=SiteLedger.Stage.COMPLETE||!"minecraft:overworld".equals(site.dimension))continue;
            if(!NpcLifecycleRules.allowNewResident(true,true,true,true,loadedCount,
                    cfg.residentGlobalCap,ledger.countSite(site.key),cfg.residentSiteCap))continue;
            boolean nearby=false;
            for(ServerPlayer player:world.players()){
                long dx=(long)player.getBlockX()-site.x,dz=(long)player.getBlockZ()-site.z;
                if(dx*dx+dz*dz<=(long)180*180){nearby=true;break;}
            }
            if(!nearby)continue;
            NpcNameCatalog.Name leader=names.leader(site.site);
            if(leader!=null){
                String leaderKey="leader@"+site.site;
                if(!ledger.assigned(leaderKey)){
                    // Prefer an indoor main hall; never put a leader outside a building on purpose.
                    for(Candidate c:candidatesBySite.getOrDefault(site.site,List.of())){
                        if(!c.site().equals(site.site)||!mainHall(c.zone()))continue;
                        BlockPos p=new BlockPos(site.x+c.dx,site.baseY+c.dy,site.z+c.dz);
                        if(spawnResident(world,ledger,leaderKey,site.key,leader.role(),p,leader,true))return;
                    }
                }
            }
            for(Candidate c:candidatesBySite.getOrDefault(site.site,List.of())){
                if(!c.site().equals(site.site))continue;
                String key="site@"+site.key+"/"+c.piece()+"/"+c.index();
                if(ledger.assigned(key))continue;
                BlockPos p=new BlockPos(site.x+c.dx,site.baseY+c.dy,site.z+c.dz);
                var name=names.take(site.site,c.role(),usedNames,key);
                if(name==null)continue;
                if(spawnResident(world,ledger,key,site.key,c.role(),p,name,false))return;
            }
        }
    }
    private static boolean mainHall(String zone){
        String s=zone.toLowerCase(java.util.Locale.ROOT);
        return s.contains("main")||s.contains("hall")||s.contains("headquarters")||
                s.contains("residence")||s.contains("throne")||s.contains("court");
    }
    public static boolean clearance(ServerLevel world,BlockPos pos){
        if(!world.hasChunkAt(pos)||!world.hasChunkAt(pos.above())||!world.hasChunkAt(pos.below()))return false;
        var below=world.getBlockState(pos.below());
        if(below.getFluidState().isEmpty() && !below.isAir() && !below.getCollisionShape(world,pos.below()).isEmpty() &&
                world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir() &&
                world.getEntitiesOfClass(Entity.class,new AABB(pos).inflate(.4,.05,.4)).isEmpty())return true;
        return false;
    }
    public static String displayName(NpcNameCatalog.Name name){
        String mode=config().displayNameStyle;
        if(name.hanzi().isBlank()||"KOREAN".equals(mode))return name.korean();
        return switch(mode){
            case "CHINESE" -> name.hanzi();
            case "MIXED", "BOTH" -> name.korean()+" ("+name.hanzi()+")";
            default -> name.korean();
        };
    }
    private static boolean spawnResident(ServerLevel world,NpcPopulationLedger ledger,String slotKey,String siteKey,
                              String role,BlockPos pos,NpcNameCatalog.Name name,boolean leader){
        if(ledger.assigned(slotKey)||ledger.usedName(name.korean())||!clearance(world,pos))return false;
        // A stable UUID is never reused for another individual or site.
        UUID id=NpcSpawnRules.stableId("minecraft:overworld",slotKey,0,0);
        if(world.getEntity(id)!=null)return false;
        MurimHumanoid npc=new MurimHumanoid(MurimNpcTypes.HUMAN.get(),world);
        npc.setSkin(NpcSkinSelector.forName(name.korean(),slotKey));
        if(leader)npc.setLeaderStats("sado".equals(name.faction()));
        else if(role.contains("disciple")||role.contains("warrior")||role.contains("guard")||role.contains("elder")||role.contains("instructor")){
            var pool=new kr.murimlife.npc.combat.WeaponTactics.Style[]{
                kr.murimlife.npc.combat.WeaponTactics.Style.SWORD,
                kr.murimlife.npc.combat.WeaponTactics.Style.SABER,
                kr.murimlife.npc.combat.WeaponTactics.Style.SPEAR,
                kr.murimlife.npc.combat.WeaponTactics.Style.STAFF,
                kr.murimlife.npc.combat.WeaponTactics.Style.DAGGER,
                kr.murimlife.npc.combat.WeaponTactics.Style.DUAL,
                kr.murimlife.npc.combat.WeaponTactics.Style.HEAVY,
                kr.murimlife.npc.combat.WeaponTactics.Style.FIST};
            npc.setWeaponStyle(pool[Math.floorMod(id.hashCode(),pool.length)]);
        }
        npc.setUUID(id);
        npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0.0f,0.0f);
        String honorific= !leader?"[무림] ":switch(role){
            case "clan_head" -> "[가주] ";
            case "bangju" -> "[방주] ";
            case "abbot" -> "[방장] ";
            default -> "[장문인] ";
        };
        npc.setCustomName(Component.literal(honorific+displayName(name)));
        npc.setCustomNameVisible(true);
        npc.setPersistenceRequired();
        var tag=npc.getPersistentData();
        tag.putBoolean("MurimLifeResident",true);
        tag.putString("MurimNpcSlot",slotKey);tag.putString("MurimNpcSite",siteKey);
        tag.putString("MurimRole",role);tag.putString("MurimNameKo",name.korean());
        tag.putString("MurimNameZh",name.hanzi());tag.putString("MurimPinyin",name.pinyin());
        tag.putBoolean("MurimUniqueLeader",leader);
        int baseRealm=leader?6:(role.contains("elder")?4:role.contains("instructor")?3:
            role.contains("inner_disciple")?2:role.contains("outer_disciple")?0:
            role.contains("guard")||role.contains("warrior")?1:0);
        tag.putInt("MurimRealmIndex",baseRealm);
        tag.putString("MurimRealm",kr.murimlife.v16.RealmRules.REALMS[baseRealm]);
        boolean spawned=world.addFreshEntity(npc);
        if(!spawned)return false;
        // The join event may already have recovered this record; treat the same UUID as success.
        var existing=ledger.getResident(slotKey);
        if(existing!=null && existing.entityUuid().equals(id))return true;
        if(!ledger.put(new NpcPopulationLedger.Resident(slotKey,siteKey,role,id,
                name.korean(),name.hanzi(),name.pinyin(),leader))){
            npc.discard();return false;
        }
        return true;
    }
    /** Recover a journal entry if a crash saved an entity chunk before the SavedData journal. */
    public static void onEntityJoin(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level))return;
        if(!(event.getEntity() instanceof MurimHumanoid || event.getEntity() instanceof Villager))return;
        Entity npc=event.getEntity();
        if(npc instanceof MurimHumanoid human)kr.murimlife.economy.NpcSectRoster.onEntityLoad(human);
        var tag=npc.getPersistentData();
        if(!tag.getBoolean("MurimLifeResident"))return;
        String slot=tag.getString("MurimNpcSlot"),name=tag.getString("MurimNameKo");
        if(slot.isBlank()||name.isBlank())return;
        var ledger=NpcPopulationLedger.get(level);
        var existing=ledger.getResident(slot);
        if(existing!=null){
            if(!existing.entityUuid().equals(npc.getUUID()))npc.discard(); // collision, never clone a named resident
            return;
        }
        ledger.put(new NpcPopulationLedger.Resident(slot,tag.getString("MurimNpcSite"),
                tag.getString("MurimRole"),npc.getUUID(),name,tag.getString("MurimNameZh"),
                tag.getString("MurimPinyin"),tag.getBoolean("MurimUniqueLeader")));
    }
    /** Manual one-NPC test; neither a village nor a tournament is auto-modified. */
    public static String testSpawn(ServerPlayer player,String faction){
        var cfg=config();
        if(!cfg.enablePersistentResidents)return "config/murimlife-npc.json에서 enablePersistentResidents=true로 설정하고 재시작하세요.";
        ServerLevel world=player.serverLevel();if(world!=player.server.overworld())return "현재 시험판은 오버월드 전용입니다.";
        if(loadedResidentCount(world)>=cfg.residentGlobalCap)return "NPC 월드 인원 제한에 도달했습니다.";
        BlockPos pos=player.blockPosition().relative(player.getDirection(),3);
        var ledger=NpcPopulationLedger.get(world);
        String slot="manual@"+pos.getX()+","+pos.getY()+","+pos.getZ();
        if(ledger.assigned(slot))return "이 시험 위치에는 이미 NPC가 배정되었습니다.";
        String role=java.util.Set.of("namgung","tangmun","paengga","jegal","mourong").contains(faction)
                ?"clan_warrior":"inner_disciple";
        if("sado".equals(faction)) {
            var leader=names.leader("sado");
            String unique="leader@sado";
            if(ledger.assigned(unique))return "사도련주는 이 월드에 이미 등록되어 있습니다.";
            return spawnResident(world,ledger,unique,"manual:sado","sado_leader",pos,leader,true)
                ?"사도련주 "+leader.korean()+"(고유 외형/강자 능력) 생성":"사도련주 생성 부지 또는 이름 검증 실패";
        }
        var name=names.take(faction,role,ledger.usedNames(),slot);
        if(name==null)return "사용 가능한 고유 이름이 부족합니다.";
        return spawnResident(world,ledger,slot,"manual:"+faction,role,pos,name,false)
            ?"영구 저장 NPC "+name.korean()+" 생성. 청크가 언로드되어도 저장되며 돌아오면 다시 나타납니다."
            :"바닥/머리 위 공간/다른 엔티티가 겹칩니다. 평평한 장소에서 다시 시험하세요.";
    }
}
