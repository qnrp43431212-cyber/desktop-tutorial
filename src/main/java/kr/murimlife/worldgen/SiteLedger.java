package kr.murimlife.worldgen;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent save journal. Contains one record per site; partial jobs are never retried from zero. */
public final class SiteLedger extends SavedData {
    public static final String SAVE_KEY="murimlife_worldgen_sites_v117";
    public enum Stage { SURVEY, DENSE_SURVEY, PAINT_SURVEY, FOUNDATIONS, PAINT, PIECES, COMPLETE, REJECTED, NEEDS_REPAIR }
    public static final class Job {
        public String key,site,dimension,reason="";
        public Stage stage=Stage.SURVEY;
        public int x,z,baseY,scanCursor,paintCursor,paintIndex,pieceIndex,foundationIndex,foundationCursor,minY=Integer.MAX_VALUE,maxY=Integer.MIN_VALUE;
        public int[] sampledHeights=new int[2048];
        public int heightCount;
        public Job(String key,String site,String dimension,int x,int z){
            this.key=key;this.site=site;this.dimension=dimension;this.x=x;this.z=z;
        }
        public boolean active(){return stage==Stage.SURVEY||stage==Stage.DENSE_SURVEY||stage==Stage.PAINT_SURVEY||
                stage==Stage.FOUNDATIONS||stage==Stage.PAINT||stage==Stage.PIECES;}
        public boolean blocksUniqueness(){return active()||stage==Stage.COMPLETE||stage==Stage.NEEDS_REPAIR;}
        CompoundTag write(){
            CompoundTag n=new CompoundTag();
            n.putString("key",key);n.putString("site",site);n.putString("dimension",dimension);
            n.putString("stage",stage.name());n.putString("reason",reason);
            n.putInt("x",x);n.putInt("z",z);n.putInt("baseY",baseY);
            n.putInt("scanCursor",scanCursor);n.putInt("paintIndex",paintIndex);
            n.putInt("paintCursor",paintCursor);n.putInt("pieceIndex",pieceIndex);
            n.putInt("foundationIndex",foundationIndex);n.putInt("foundationCursor",foundationCursor);
            n.putInt("minY",minY);n.putInt("maxY",maxY);
            n.putInt("heightCount",heightCount);
            n.putIntArray("sampledHeights",java.util.Arrays.copyOf(sampledHeights,heightCount));
            return n;
        }
        static Job read(CompoundTag n){
            Job j=new Job(n.getString("key"),n.getString("site"),n.getString("dimension"),n.getInt("x"),n.getInt("z"));
            try { j.stage=Stage.valueOf(n.getString("stage")); }
            catch(IllegalArgumentException ex) { j.stage=Stage.NEEDS_REPAIR; }
            j.reason=n.getString("reason");
            j.baseY=n.getInt("baseY");j.scanCursor=n.getInt("scanCursor");
            j.paintIndex=n.getInt("paintIndex");j.paintCursor=n.getInt("paintCursor");
            j.pieceIndex=n.getInt("pieceIndex");
            j.foundationIndex=n.getInt("foundationIndex");j.foundationCursor=n.getInt("foundationCursor");j.minY=n.getInt("minY");j.maxY=n.getInt("maxY");
            int[] arr=n.getIntArray("sampledHeights");
            j.sampledHeights=java.util.Arrays.copyOf(arr,2048);
            j.heightCount=Math.min(Math.min(arr.length,2048),Math.max(0,n.getInt("heightCount")));
            return j;
        }
    }
    private final List<Job> jobs=new ArrayList<>();
    public List<Job> jobs(){return List.copyOf(jobs);}
    public void insert(Job job){jobs.add(job);setDirty();}
    public void changed(){setDirty();}
    public boolean hasKey(String key){return jobs.stream().anyMatch(j->j.key.equals(key));}
    public boolean uniqueExists(String site){
        return jobs.stream().anyMatch(j->j.site.equals(site)&&j.blocksUniqueness());
    }
    public static SiteLedger get(ServerLevel level) {
        // The same SavedData is shared across all dimensions, guaranteeing global at-most-one uniqueness.
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(SiteLedger::new, SiteLedger::load, null),SAVE_KEY);
    }
    public static SiteLedger load(CompoundTag tag, HolderLookup.Provider provider){
        SiteLedger ledger=new SiteLedger();
        ListTag list=tag.getList("jobs", Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++) ledger.jobs.add(Job.read(list.getCompound(i)));
        return ledger;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider){
        ListTag list=new ListTag();for(Job job:jobs) list.add(job.write());
        tag.put("jobs",list);tag.putInt("version",1);return tag;
    }
}
