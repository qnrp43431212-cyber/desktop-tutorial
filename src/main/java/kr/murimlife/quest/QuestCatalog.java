package kr.murimlife.quest;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exactly 30 Alliance and 27 Sado quests per each of five ranks, in pack resources. */
public final class QuestCatalog {
    public record Quest(String id,String faction,int grade,String title,String objective,String item,int count,
                        int rewardMun,int reputation,int good){}
    public static final String[] GRADES={"견습","하급","중급","상급","최상급"};
    private static final List<Quest> ALL=load();
    private QuestCatalog(){}
    private static List<Quest> load(){
        try(var input=QuestCatalog.class.getResourceAsStream("/data/murimlife/quests/catalog.json")){
            if(input==null)throw new IllegalStateException("Missing murimlife quest catalog");
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonArray();
            List<Quest> quests=new ArrayList<>();Set<String> ids=new HashSet<>();
            for(var node:json){var o=node.getAsJsonObject();
                var q=new Quest(o.get("id").getAsString(),o.get("faction").getAsString(),o.get("grade").getAsInt(),
                    o.get("title").getAsString(),o.get("objective").getAsString(),o.get("item").getAsString(),
                    o.get("count").getAsInt(),o.get("rewardMun").getAsInt(),o.get("reputation").getAsInt(),o.get("good").getAsInt());
                if(!ids.add(q.id())||q.grade()<0||q.grade()>=GRADES.length||q.count()<=0)throw new IllegalStateException("Invalid quest " + q.id());
                quests.add(q);
            }
            for(int g=0;g<5;g++){
                final int grade=g;
                if(quests.stream().filter(q->q.faction().equals("alliance")&&q.grade()==grade).count()!=30||
                   quests.stream().filter(q->q.faction().equals("sado")&&q.grade()==grade).count()!=27)
                    throw new IllegalStateException("Bad rank mission count: "+g);
            }
            return List.copyOf(quests);
        }catch(Exception ex){throw new IllegalStateException("Cannot load MURIM LIFE quests",ex);}
    }
    public static List<Quest> list(String faction,int grade){return ALL.stream()
        .filter(q->q.faction().equals(faction)&&q.grade()==grade).toList();}
    public static Quest find(String id){for(var q:ALL)if(q.id().equals(id))return q;return null;}
    public static int count(){return ALL.size();}
}
