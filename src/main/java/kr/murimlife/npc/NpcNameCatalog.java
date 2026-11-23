package kr.murimlife.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Curated unique leaders + members, and distinct local Korean/Chinese-style name candidates.
 *  All IDs are authored original game characters, not historical or canon personnel.
 */
public final class NpcNameCatalog {
    public record Name(String key,String faction,String role,String korean,String hanzi,String pinyin,String style) {
        public String label(boolean useChineseCharacters){
            if (useChineseCharacters && !hanzi.isBlank()) return hanzi;
            return korean;
        }
    }
    private final Map<String,Name> leaders;
    private final Map<String,List<Name>> factionMembers;
    private final List<Name> procedural;
    private final Set<String> forbiddenLegacy;

    private NpcNameCatalog(Map<String,Name> leaders,Map<String,List<Name>> members,List<Name> procedural,Set<String> legacy){
        this.leaders=Map.copyOf(leaders);
        Map<String,List<Name>> copy=new HashMap<>();
        members.forEach((k,v)->copy.put(k,List.copyOf(v)));
        this.factionMembers=Map.copyOf(copy);
        this.procedural=List.copyOf(procedural);
        this.forbiddenLegacy=Set.copyOf(legacy);
    }
    private static JsonObject read(String filename){
        try(var resource=NpcNameCatalog.class.getResourceAsStream("/data/murimlife/npc/"+filename)){
            if(resource==null)throw new IllegalStateException("Missing NPC roster: "+filename);
            return JsonParser.parseReader(new InputStreamReader(resource, StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception ex){throw new IllegalStateException("Cannot load NPC roster: "+filename,ex);}
    }
    private static String val(JsonObject j,String key){return j.get(key).getAsString();}
    public static NpcNameCatalog load(){
        Map<String,Name> leaders=new HashMap<>();
        Map<String,List<Name>> members=new HashMap<>();
        Set<String> used=new HashSet<>();
        Set<String> legacy=new HashSet<>();
        for(NpcSeedProfiles.Seed profile:NpcSeedProfiles.read())legacy.add(profile.name());
        JsonArray names=read("named_leaders_16.json").getAsJsonArray("leaders");
        if(names.size()!=16)throw new IllegalStateException("Missing named leaders");
        for(var node:names){
            JsonObject j=node.getAsJsonObject();
            var name=new Name(val(j,"npc_key"),val(j,"faction_id"),val(j,"role"),val(j,"name_ko"),
                val(j,"name_zh"),val(j,"pinyin"),"CHINESE");
            if(!used.add(name.korean())||legacy.contains(name.korean())||leaders.putIfAbsent(name.faction(),name)!=null)
                throw new IllegalStateException("Leader name collision "+name.korean());
        }
        JsonArray party=read("named_faction_members_330.json").getAsJsonArray("members");
        if(party.size()!=330)throw new IllegalStateException("Missing named members");
        for(var node:party){
            JsonObject j=node.getAsJsonObject();
            var name=new Name(val(j,"npc_key"),val(j,"faction_id"),val(j,"role"),val(j,"name_ko"),
                val(j,"name_zh"),val(j,"pinyin"),val(j,"name_style"));
            if(!used.add(name.korean())||legacy.contains(name.korean()))
                throw new IllegalStateException("Duplicated authored NPC: "+name.korean());
            members.computeIfAbsent(name.faction(),k->new ArrayList<>()).add(name);
        }
        if(leaders.size()!=16||members.size()!=15)
            throw new IllegalStateException("Incorrect leader / faction distribution");
        for(var entry:members.entrySet())if(entry.getValue().size()!=22)
            throw new IllegalStateException("Missing members: "+entry.getKey());
        List<Name> bank=new ArrayList<>();
        for(var node:read("procedural_name_bank.json").getAsJsonArray("names")){
            JsonObject j=node.getAsJsonObject();
            String value=val(j,"name_ko");
            if(!used.add(value)||legacy.contains(value)) throw new IllegalStateException("Duplicated name bank: "+value);
            bank.add(new Name("bank:"+bank.size(),"","resident",value,val(j,"name_zh"),
                val(j,"pinyin"),val(j,"style")));
        }
        if(bank.size()<800)throw new IllegalStateException("Name bank too small");
        return new NpcNameCatalog(leaders,members,bank,legacy);
    }
    public Name leader(String faction){return leaders.get(faction);}
    public Name audienceName(int seatIndex){return procedural.get(Math.floorMod(31*seatIndex+17,procedural.size()));}
    public List<Name> factionMembers(String faction){return factionMembers.getOrDefault(faction,List.of());}
    /** Returns an unused authored individual (pre-existing saved names are never reassigned). */
    public Name take(String faction,String requestedRole,Set<String> reserved,String salt){
        if(reserved==null)throw new IllegalArgumentException("reserved names missing");
        var own=factionMembers(faction);
        for(Name n:own)if(n.role().equals(requestedRole)&&!reserved.contains(n.korean()))return n;
        for(Name n:own)if(!reserved.contains(n.korean()))return n;
        if(procedural.isEmpty())return null;
        int start=Math.floorMod(salt.hashCode(),procedural.size());
        int parity=Math.floorMod(salt.hashCode()>>>8,2);
        // Alternate Korean-style and Chinese-style among unnamed townspeople.
        for(int pass=0;pass<2;pass++)for(int i=0;i<procedural.size();i++){
            Name n=procedural.get((start+i)%procedural.size());
            if(pass==0 && ("KOREAN".equals(n.style()) ? 0:1)!=parity)continue;
            if(!reserved.contains(n.korean())&&!forbiddenLegacy.contains(n.korean()))return n;
        }
        return null; // never repeat a name after the global pool is exhausted
    }
}
