package kr.murimlife.cult;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable registry: 24 seat holders plus 500 UNBORN future NPC identity templates, not 500 active or mandatory successors. */
public final class CultDesign {
    public record Person(String id,String cult,String name,String art,int skin,int realm,
                         String type,String weapon,int training,int innerCost,int cooldown,double power){}
    private static final Map<String,List<Person>> BY_CULT;
    private static final Map<String,Person> BY_ID;
    private static final Map<String,Person> BY_ART;
    static {
        var groups=new LinkedHashMap<String,List<Person>>();
        groups.put("마교",new ArrayList<>());groups.put("혈교",new ArrayList<>());
        var byId=new LinkedHashMap<String,Person>();var byArt=new LinkedHashMap<String,Person>();
        Set<String> usedNames=new HashSet<>(),usedSkins=new HashSet<>();
        try(var input=CultDesign.class.getResourceAsStream("/data/murimlife/cult/cult_roster_524.json")) {
            if(input==null)throw new IllegalStateException("Cult roster resource absent");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("schema").getAsInt()!=1)throw new IllegalStateException("Wrong cult roster schema");
            var seats=root.getAsJsonArray("seats");if(seats.size()!=12)throw new IllegalStateException("Expected 12 ranks");
            for(int i=0;i<12;i++)if(seats.get(i).getAsJsonObject().get("min_realm").getAsInt()!=CultRankRules.seatMinimum(i))
                throw new IllegalStateException("Seat realm requirement mismatch");
            for(var element:root.getAsJsonArray("people")){
                var o=element.getAsJsonObject();
                Person p=new Person(o.get("id").getAsString(),o.get("cult").getAsString(),
                    o.get("name_ko").getAsString(),o.get("technique").getAsString(),
                    o.get("skin").getAsInt(),o.get("realm").getAsInt(),
                    o.get("kind").getAsString(),o.get("weapon").getAsString(),
                    o.get("training").getAsInt(),o.get("inner_cost").getAsInt(),
                    o.get("cooldown").getAsInt(),o.get("power").getAsDouble());
                if(!groups.containsKey(p.cult())||p.name().isBlank()||p.art().isBlank()||
                   p.realm()<4||p.realm()>7||p.skin()<442||p.skin()>965||
                   p.art().contains("천마")||p.art().contains("혈마")||
                   !usedNames.add(p.name())||!usedSkins.add(Integer.toString(p.skin()))||
                   byId.putIfAbsent(p.id(),p)!=null||byArt.putIfAbsent(p.art(),p)!=null)
                    throw new IllegalStateException("Duplicate or forbidden cult member "+p.id());
                groups.get(p.cult()).add(p);
            }
        }catch(Exception e){throw new IllegalStateException("Cult roster invalid",e);}
        for(var pair:groups.entrySet()){
            if(pair.getValue().size()!=262)throw new IllegalStateException("Expected 12+250 unique people in "+pair.getKey());
            for(int seat=0;seat<12;seat++)if(pair.getValue().get(seat).realm()<CultRankRules.seatMinimum(seat))
                throw new IllegalStateException("Senior demon below minimum realm");
            pair.setValue(List.copyOf(pair.getValue()));
        }
        BY_CULT=Map.copyOf(groups);BY_ID=Map.copyOf(byId);BY_ART=Map.copyOf(byArt);
    }
    private CultDesign(){}
    public static Person get(String id){return BY_ID.get(id);}
    public static Person art(String skill){return BY_ART.get(skill);}
    public static Collection<Person> all(){return BY_ID.values();}
    public static Person initial(String cult,int seat){return BY_CULT.get(cult).get(seat);}
    public static Person reserve(String cult,int index){
        var pool=BY_CULT.get(cult);return pool!=null&&index>=0&&index<250?pool.get(12+index):null;
    }
    public static String title(int seat){return switch(seat){
        case 0->"대호법";case 1->"좌호법";case 2->"우호법";case 3->"대마장로";
        case 4->"사영장로";case 5->"교형장로";case 6->"음월전주";case 7->"흑혼전주";
        case 8->"외단주";case 9->"암당주";case 10->"귀위대주";case 11->"비도대주";
        default->throw new IllegalArgumentException("seat invalid");};}
}
