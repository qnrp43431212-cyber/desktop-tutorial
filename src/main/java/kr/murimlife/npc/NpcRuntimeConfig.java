package kr.murimlife.npc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.nio.file.Path;

/** Deliberately opt-in: uses vanilla villager models until the wuxia humanoid renderer is ready. */
public final class NpcRuntimeConfig {
    public boolean enablePersistentResidents=false;
    public boolean enableTournamentDemo=false;
    /** KOREAN, CHINESE, BOTH, or MIXED; Korean-style NPCs have no Chinese alias. */
    public String displayNameStyle="MIXED";
    public int residentGlobalCap=160;
    public int residentSiteCap=24;
    public int residentSpawnIntervalTicks=100;
    public int tournamentSpectatorCap=48;
    public int tournamentMaxDurationTicks=2400;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static NpcRuntimeConfig read(){
        Path path=FMLPaths.CONFIGDIR.get().resolve("murimlife-npc.json");
        try{
            if(!Files.exists(path)){
                NpcRuntimeConfig created=new NpcRuntimeConfig();
                Files.writeString(path,GSON.toJson(created));return created;
            }
            NpcRuntimeConfig config=GSON.fromJson(Files.readString(path),NpcRuntimeConfig.class);
            if(config==null)config=new NpcRuntimeConfig();
            if(config.displayNameStyle==null || !java.util.Set.of("KOREAN","CHINESE","BOTH","MIXED").contains(config.displayNameStyle.toUpperCase(java.util.Locale.ROOT)))
                config.displayNameStyle="MIXED";
            else config.displayNameStyle=config.displayNameStyle.toUpperCase(java.util.Locale.ROOT);
            config.residentGlobalCap=Math.clamp(config.residentGlobalCap,1,400);
            config.residentSiteCap=Math.clamp(config.residentSiteCap,1,40);
            config.residentSpawnIntervalTicks=Math.clamp(config.residentSpawnIntervalTicks,40,1200);
            config.tournamentSpectatorCap=Math.clamp(config.tournamentSpectatorCap,0,80);
            config.tournamentMaxDurationTicks=Math.clamp(config.tournamentMaxDurationTicks,200,6000);
            return config;
        }catch(Exception ex){throw new IllegalStateException("Invalid NPC config: "+path,ex);}
    }
}
