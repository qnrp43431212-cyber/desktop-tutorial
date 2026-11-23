package kr.murimlife.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

/** Conservative survey. Never assumes unknown modded/player blocks are natural. */
final class WorldSiteSafety {
    private WorldSiteSafety() { }
    private static final Set<String> NATURAL = Set.of(
        "grass_block","dirt","coarse_dirt","rooted_dirt","podzol","mycelium","stone",
        "andesite","diorite","granite","deepslate","tuff","calcite","moss_block",
        "gravel","sand","red_sand","clay","snow_block","terracotta","mud","packed_mud",
        "muddy_mangrove_roots","netherrack" /* rejected later by dimension guard */);
    // getChunkSource().hasChunk is non-generating: no /forceload and no synchronous terrain creation.
    static boolean ready(ServerLevel level,int blockX,int blockZ){
        return level.getChunkSource().hasChunk(blockX>>4,blockZ>>4);
    }
    static boolean allReady(ServerLevel level,int x0,int z0,int x1,int z1){
        for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++)
            if(!level.getChunkSource().hasChunk(cx,cz))return false;
        return true;
    }
    /** Check the actual future building airspace. Tree trunks, leaves, walls, containers, etc. are obstacles. */
    static boolean clearAirspace(ServerLevel level,int x,int z,int terrainY,int highestY){
        if(highestY>=level.getMaxBuildHeight() || terrainY<level.getMinBuildHeight())return false;
        for(int y=terrainY+1;y<=highestY;y++){
            BlockState state=level.getBlockState(new BlockPos(x,y,z));
            if(!state.getFluidState().isEmpty() || (!state.isAir() && !state.canBeReplaced()))return false;
        }
        return true;
    }
    /** Reject buildings over open caves: shallow decorative fill cannot safely underpin them. */
    static boolean solidBase(ServerLevel level,int x,int z,int terrainY){
        for(int down=1;down<=4;down++){
            BlockState below=level.getBlockState(new BlockPos(x,terrainY-down,z));
            if(below.isAir() || !below.getFluidState().isEmpty())return false;
        }
        return true;
    }
    static boolean natural(BlockState state) {
        ResourceLocation key=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return "minecraft".equals(key.getNamespace()) &&
                (NATURAL.contains(key.getPath()) || key.getPath().endsWith("_terracotta"));
    }
    static int axis(int origin,int size,int index) {
        return Math.min(origin+size-1,origin+index*16);
    }
    static int samples(SiteCatalog.Site site){return site.samplesPerAxisX()*site.samplesPerAxisZ();}
    static void surveyOne(ServerLevel level,SiteLedger ledger,SiteLedger.Job job,SiteCatalog.Site site) {
        int perRow=site.samplesPerAxisZ();
        int index=job.scanCursor;
        if(index>=samples(site))return;
        int x=axis(job.x,site.width(),index/perRow);
        int z=axis(job.z,site.depth(),index%perRow);
        // Explicit, bounded generation: one chunk can be requested per sampling step;
        // never keep a permanent force-load ticket.
        if(!ready(level,x,z))return; // Pause cleanly until an existing player-loaded chunk is ready.
        BlockPos check=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1,z);
        if(check.getY()<=level.getMinBuildHeight()+3 || check.getY()>=level.getMaxBuildHeight()-80){
            reject(ledger,job,"Y_LIMIT "+x+","+z);return;
        }
        if(!level.getWorldBorder().isWithinBounds(check)){
            reject(ledger,job,"WORLD_BORDER "+x+","+z);return;
        }
        BlockState top=level.getBlockState(check);
        if(!top.getFluidState().isEmpty() || !natural(top)){
            reject(ledger,job,"BLOCKED_GROUND "+x+","+z+" "+top.getBlock());return;
        }
        // Works for already-known vanilla/modded structure starts at sampled positions;
        // some future mod-generated starts are not yet visible at this point.
        if(!level.structureManager().getAllStructuresAt(check).isEmpty()) {
            reject(ledger,job,"EXISTING_STRUCTURE "+x+","+z);return;
        }
        int y=check.getY();
        // Reject sampled terrain suspended over a cave or sharp undercut.
        for(int down=1;down<=4;down++){
            BlockPos below=check.below(down);
            if(level.getBlockState(below).isAir()){
                reject(ledger,job,"CAVE_UNDERSIDE "+x+","+z);return;
            }
        }
        job.minY=Math.min(job.minY,y);job.maxY=Math.max(job.maxY,y);
        if(job.maxY-job.minY>site.permittedRelief()){
            reject(ledger,job,"SLOPE "+job.minY+".."+job.maxY);return;
        }
        if(job.heightCount>=job.sampledHeights.length){reject(ledger,job,"SURVEY_CAPACITY");return;}
        job.sampledHeights[job.heightCount++]=y;
        job.scanCursor++;
        if(job.scanCursor==samples(site)){
            job.baseY=job.maxY; // Build on the high edge, not buried by hills.
            job.stage=SiteLedger.Stage.DENSE_SURVEY;
        }
        ledger.changed();
    }
    static void reject(SiteLedger ledger,SiteLedger.Job job,String reason) {
        job.stage=SiteLedger.Stage.REJECTED;job.reason=reason;ledger.changed();
    }
    static boolean safeInitialArea(ServerLevel level,SiteLedger ledger, SiteLedger.Job j,SiteCatalog.Site site){
        // The survey covers the perimeter + interior grid; this check is a final quick
        // protection check before the first mutation (not a proof against all mod structures).
        int step=16;
        for(int dx=0;dx<site.width();dx+=step)for(int dz=0;dz<site.depth();dz+=step){
            int x=j.x+dx,z=j.z+dz;
            if(!ready(level,x,z))return false; // Do not force-load missing terrain for validation.
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
            BlockPos pos=new BlockPos(x,y,z);
            if(!level.getBlockState(pos).getFluidState().isEmpty()||!natural(level.getBlockState(pos))||
                    !level.structureManager().getAllStructuresAt(pos).isEmpty()){
                reject(ledger,j,"AREA_CHANGED_OR_PROTECTED "+x+","+z);return false;
            }
        }
        return true;
    }
    static boolean reservedCollision(SiteLedger ledger,SiteCatalog.Site site,int x,int z){
        var next=new PlanningCore.Claim(site.id()+"@"+x+","+z,site.id(),
            new PlanningCore.Area(x,z,x+site.width()-1,z+site.depth()-1),site.gap(),site.unique());
        // Legacy saved jobs whose old site definition is missing are conservatively protected.
        for(SiteLedger.Job j:ledger.jobs()){
            if(!j.blocksUniqueness())continue;
            SiteCatalog.Site o=WorldgenEngine.SITES.stream().filter(s->s.id().equals(j.site)).findFirst().orElse(null);
            if(o==null)return true;
            var old=new PlanningCore.Claim(j.key,j.site,
                    new PlanningCore.Area(j.x,j.z,j.x+o.width()-1,j.z+o.depth()-1),o.gap(),o.unique());
            if(PlanningCore.check(next,List.of(old))!=PlanningCore.Rejection.ACCEPT)return true;
        }
        return false;
    }
}
