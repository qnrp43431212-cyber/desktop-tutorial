package kr.murimlife.npc.dialogue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** 30 distinct combat responses and 24 distinct peaceful responses for EACH of six choices. */
public final class DialogueLines {
    private static final String[] KEYS={"draw","delay","provoke","mercy","info","end"};
    private static final Map<String,List<String>> COMBAT=load("combat");
    private static final Map<String,List<String>> DAILY=load("daily");
    private DialogueLines(){}
    private static Map<String,List<String>> load(String name){
        String path="/data/murimlife/dialogue/"+name+"_lines.json";
        try(var in=DialogueLines.class.getResourceAsStream(path)){
            if(in==null)throw new IllegalStateException("Missing dialogue resource: "+path);
            JsonObject j=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String,List<String>> result=new HashMap<>();
            for(String key:KEYS){
                List<String> lines=new ArrayList<>();JsonArray arr=j.getAsJsonArray(key);
                for(var el:arr)lines.add(el.getAsString());
                if(lines.size()<(name.equals("combat")?30:24))throw new IllegalStateException("Too few "+name+"/"+key);
                result.put(key,List.copyOf(lines));
            }
            return Map.copyOf(result);
        }catch(Exception ex){throw new IllegalStateException("Dialogue catalogue failure",ex);}
    }
    /** Avoid repeating a line within a conversation until this action's pool is exhausted. */
    public static String select(boolean battle,int option,Random random,Map<Integer,Set<Integer>> used){
        List<String> pool=(battle?COMBAT:DAILY).get(KEYS[option]);
        Set<Integer> visited=used.computeIfAbsent(option,k->new HashSet<>());
        if(visited.size()>=pool.size())visited.clear();
        int start=random.nextInt(pool.size());
        for(int i=0;i<pool.size();i++){
            int idx=(start+i)%pool.size();
            if(visited.add(idx))return pool.get(idx);
        }
        return pool.get(start);
    }
}
