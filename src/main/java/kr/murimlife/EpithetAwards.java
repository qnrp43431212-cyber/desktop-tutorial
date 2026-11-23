package kr.murimlife;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Game-side title conditions loaded from a 275-entry research/design JSON catalog. */
public final class EpithetAwards {
    private EpithetAwards(){}
    public record Rule(String name,String trigger,int required){}
    private static final List<Rule> RULES=load();
    private static List<Rule> load() {
        try(InputStream in=EpithetAwards.class.getResourceAsStream("/assets/murimlife/rules/epithet_catalog.json")){
            if(in==null) return List.of();
            JsonObject root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            var arr=root.getAsJsonArray("catalog");var result=new ArrayList<Rule>();
            for(JsonElement e:arr){var o=e.getAsJsonObject();result.add(new Rule(o.get("name").getAsString(),o.get("trigger").getAsString(),o.get("threshold").getAsInt()));}
            return List.copyOf(result);
        }catch(Exception ex){throw new IllegalStateException("Invalid epithet catalog",ex);}
    }
    public static void evaluate(UUID owner,MurimProgressData.Profile p,MurimProgressData state) {
        for(Rule r:RULES){
            int achievement=measure(p,r.trigger());
            if(achievement>=r.required())state.claimEpithet(owner,p,r.name());
        }
    }
    private static int measure(MurimProgressData.Profile p,String trigger) {
        return switch(trigger){
            case "victories","duel_wins" -> p.wins;
            case "martial_realm" -> p.realm;
            case "help_civilians" -> p.good;
            case "evil_deeds" -> p.evil;
            case "sword_mastery" -> p.counters.getOrDefault("sword_mastery",0);
            case "saber_mastery" -> p.counters.getOrDefault("saber_mastery",0);
            case "spear_mastery" -> p.counters.getOrDefault("spear_mastery",0);
            case "inner_cultivation" -> p.training;
            case "fame" -> p.wins + p.good/3;
            default -> p.counters.getOrDefault(trigger,0);
        };
    }
}
