package kr.murimlife.opportunity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Files;
import java.nio.file.Path;

/** Player-safe procedural generation switches, written to config/murimlife-opportunities.json. */
public final class OpportunityConfig {
    public boolean enabled=true;
    public double sectorChance=0.67;
    public int minimumDistanceFromSpawn=150;
    public int maximumSitesPerWorld=1000;
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    public static OpportunityConfig read(){
        Path path=FMLPaths.CONFIGDIR.get().resolve("murimlife-opportunities.json");
        try{
            if(!Files.exists(path)){
                OpportunityConfig c=new OpportunityConfig();Files.writeString(path,GSON.toJson(c));return c;
            }
            OpportunityConfig c=GSON.fromJson(Files.readString(path),OpportunityConfig.class);
            if(c==null)c=new OpportunityConfig();
            if(!Double.isFinite(c.sectorChance))c.sectorChance=.67;
            c.sectorChance=Math.clamp(c.sectorChance,0,1);
            c.minimumDistanceFromSpawn=Math.clamp(c.minimumDistanceFromSpawn,80,800);
            c.maximumSitesPerWorld=Math.clamp(c.maximumSitesPerWorld,0,10000);
            return c;
        }catch(Exception e){throw new IllegalStateException("Invalid MURIM LIFE opportunity config: "+path,e);}
    }
}
