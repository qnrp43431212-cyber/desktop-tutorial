package kr.murimlife.npc;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
/** Unique stable visual identity for all 346 authored named people, including the Sado leader. */
public final class NpcSkinSelector {
    private static Map<String,Integer> byName;
    private NpcSkinSelector(){}
    private static synchronized Map<String,Integer> names(){
        if(byName!=null)return byName;
        try(var in=NpcSkinSelector.class.getResourceAsStream("/data/murimlife/npc/wuxia_named_skins.json")){
            if(in==null)throw new IllegalStateException("wuxia_named_skins.json missing");
            var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String,Integer> map=new HashMap<>();
            for(var entry:root.getAsJsonArray("entries")){
                var o=entry.getAsJsonObject();String name=o.get("name_ko").getAsString();int idx=o.get("skin").getAsInt();
                if(map.putIfAbsent(name,idx)!=null)throw new IllegalStateException("Duplicate named skin "+name);
            }
            if(map.size()!=346)throw new IllegalStateException("Expected 346 individually mapped skins");
            byName=Map.copyOf(map);return byName;
        }catch(Exception e){throw new IllegalStateException("Wuxia skin registry invalid",e);}
    }
    public static int forName(String koreanName,String slot){
        Integer named=names().get(koreanName);if(named!=null)return named;
        return 346+Math.floorMod((koreanName+":"+slot).hashCode(),96);
    }
}
