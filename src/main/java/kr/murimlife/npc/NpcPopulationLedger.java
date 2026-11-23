package kr.murimlife.npc;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A second journal alongside vanilla entity-chunk storage.
 *  Never recreate an already assigned NPC on chunk unload, death or server restart.
 */
public final class NpcPopulationLedger extends SavedData {
    public static final String KEY="murimlife_npc_residents_v120";
    public record Resident(String slot,String site,String role,UUID entityUuid,String nameKo,String nameZh,String pinyin,boolean leader){}
    private final Map<String,Resident> residents=new HashMap<>();
    private final Set<String> usedNames=new HashSet<>();
    public boolean assigned(String slot){return residents.containsKey(slot);}
    public Resident getResident(String slot){return residents.get(slot);}
    public boolean usedName(String name){return usedNames.contains(name);}
    public Set<String> usedNames(){return Set.copyOf(usedNames);}
    public int countSite(String site){return (int)residents.values().stream().filter(r->r.site.equals(site)).count();}
    public int countTotal(){return residents.size();}
    public boolean put(Resident resident){
        if(residents.containsKey(resident.slot())||usedNames.contains(resident.nameKo()))return false;
        residents.put(resident.slot(),resident);usedNames.add(resident.nameKo());setDirty();return true;
    }
    public static NpcPopulationLedger get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(NpcPopulationLedger::new,NpcPopulationLedger::load,null), KEY);
    }
    public static NpcPopulationLedger load(CompoundTag tag,HolderLookup.Provider lookup){
        NpcPopulationLedger data=new NpcPopulationLedger();
        ListTag entries=tag.getList("residents",Tag.TAG_COMPOUND);
        for(int i=0;i<entries.size();i++){
            var entry=entries.getCompound(i);
            try{
                String slot=entry.getString("slot"),site=entry.getString("site"),role=entry.getString("role");
                UUID uuid=UUID.fromString(entry.getString("uuid"));String ko=entry.getString("nameKo");
                if(slot.isBlank()||ko.isBlank())continue;
                Resident r=new Resident(slot,site,role,uuid,ko,entry.getString("nameZh"),
                    entry.getString("pinyin"),entry.getBoolean("leader"));
                if(data.residents.putIfAbsent(slot,r)==null)data.usedNames.add(ko);
            }catch(IllegalArgumentException ignored){ /* a malformed entry does not crash all saves */ }
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){
        ListTag entries=new ListTag();
        for(Resident r:residents.values()){
            CompoundTag e=new CompoundTag();e.putString("slot",r.slot());e.putString("site",r.site());
            e.putString("role",r.role());e.putString("uuid",r.entityUuid().toString());
            e.putString("nameKo",r.nameKo());e.putString("nameZh",r.nameZh());
            e.putString("pinyin",r.pinyin());e.putBoolean("leader",r.leader());entries.add(e);
        }
        tag.put("residents",entries);tag.putInt("schema",2);return tag;
    }
}
