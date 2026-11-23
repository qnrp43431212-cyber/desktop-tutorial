package kr.murimlife.worldgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

/** Risky world edits are opt-in until the runtime has been tested in Minecraft. */
public final class WorldgenConfig {
    public boolean enableLargeSiteGeneration = false;
    public boolean enableCityGeneration = false;
    public int minimumDistanceFromSpawn = 1024;
    public int activationDistance = 360;
    public int blocksPerStep = 128;
    public int scanSamplesPerStep = 2;
    public int playerCheckIntervalTicks = 200;
    public int jobWorkIntervalTicks = 5;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static WorldgenConfig read() {
        Path file=FMLPaths.CONFIGDIR.get().resolve("murimlife-worldgen.json");
        try {
            if(!Files.exists(file)){
                WorldgenConfig config=new WorldgenConfig();
                Files.writeString(file,GSON.toJson(config));return config;
            }
            WorldgenConfig config=GSON.fromJson(Files.readString(file),WorldgenConfig.class);
            if(config==null)config=new WorldgenConfig();
            config.activationDistance=Math.max(100,Math.min(config.activationDistance,900));
            config.blocksPerStep=Math.max(16,Math.min(config.blocksPerStep,256));
            config.scanSamplesPerStep=Math.max(1,Math.min(config.scanSamplesPerStep,4));
            config.playerCheckIntervalTicks=Math.max(100,config.playerCheckIntervalTicks);
            config.jobWorkIntervalTicks=Math.max(2,config.jobWorkIntervalTicks);
            return config;
        }catch(Exception ex){throw new IllegalStateException("Cannot read config "+file,ex);}
    }
}
