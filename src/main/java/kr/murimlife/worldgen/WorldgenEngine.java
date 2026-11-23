package kr.murimlife.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/** Opt-in server-side world generation. Main-thread only; never executes existing forceload functions. */
public final class WorldgenEngine {
    // Load only when opt-in is enabled. A broken optional catalog must not crash safe-mode startup.
    public static List<SiteCatalog.Site> SITES=List.of();
    private static final Logger LOG=LoggerFactory.getLogger("murimlife.worldgen");
    private static WorldgenConfig CONFIG;
    private static long tickCount=0;
    private WorldgenEngine() { }

    public static void onTick(ServerTickEvent.Post event){
        MinecraftServer server=event.getServer();
        if(CONFIG==null){
            CONFIG=WorldgenConfig.read();
            if(CONFIG.enableLargeSiteGeneration){
                try{SITES=SiteCatalog.read();}
                catch(RuntimeException error){
                    // Reject all potentially destructive jobs if a template registry cannot load.
                    LOG.error("MURIM worldgen disabled: catalog failed validation",error);
                    CONFIG.enableLargeSiteGeneration=false;
                }
            }
            LOG.info("MURIM LIFE worldgen: enabled={}, cities={}, catalog={}",
                    CONFIG.enableLargeSiteGeneration,CONFIG.enableCityGeneration,SITES.size());
        }
        if(!CONFIG.enableLargeSiteGeneration || SITES.isEmpty())return;
        ++tickCount;
        if(tickCount%CONFIG.jobWorkIntervalTicks!=0 && tickCount%CONFIG.playerCheckIntervalTicks!=0)return;
        ServerLevel level=server.overworld();
        SiteLedger ledger=SiteLedger.get(level);
        SiteLedger.Job job=ledger.jobs().stream().filter(SiteLedger.Job::active).findFirst().orElse(null);
        if(job!=null) {
            // Don't build unattended / inaccessible areas; resume if a player returns.
            if(tickCount%CONFIG.jobWorkIntervalTicks==0 && nearPlayer(level,job.x,job.z,CONFIG.activationDistance+420)) process(level,ledger,job);
            return;
        }
        if(tickCount%CONFIG.playerCheckIntervalTicks!=0)return;
        for(ServerPlayer player:level.players()) {
            if(chooseSiteForPlayer(level,ledger,player))break;
        }
    }
    private static boolean nearPlayer(ServerLevel level,int x,int z,int limit){
        for(ServerPlayer p:level.players()){
            long dx=(long)p.getBlockX()-x,dz=(long)p.getBlockZ()-z;
            if(dx*dx+dz*dz<=(long)limit*limit)return true;
        }
        return false;
    }
    private static boolean chooseSiteForPlayer(ServerLevel level,SiteLedger ledger,ServerPlayer player) {
        int tileX=Math.floorDiv(player.getBlockX(),1024),tileZ=Math.floorDiv(player.getBlockZ(),1024);
        // 3x3 search avoids a sharp trigger edge when moving between sectors.
        for(int ox=-1;ox<=1;ox++)for(int oz=-1;oz<=1;oz++) {
            int cx=tileX+ox,cz=tileZ+oz;
            long hash=PlanningCore.cellSeed(level.getSeed(),cx,cz);
            SiteCatalog.Site site=SITES.get(Math.floorMod((int)(hash>>>32),SITES.size()));
            if(site.category().equals("CITY")&&!CONFIG.enableCityGeneration)continue;
            if(site.unique()&&ledger.uniqueExists(site.id()))continue;
            int x=cx*1024+160+(int)Math.floorMod(hash>>>12,96L);
            int z=cz*1024+160+(int)Math.floorMod(hash>>>22,96L);
            String key=site.id()+"@"+cx+","+cz;
            if(ledger.hasKey(key))continue;
            int centerX=x+site.width()/2,centerZ=z+site.depth()/2;
            if(!nearPlayer(level,centerX,centerZ,CONFIG.activationDistance))continue;
            BlockPos spawn=level.getSharedSpawnPos();
            long dx=(long)centerX-spawn.getX(),dz=(long)centerZ-spawn.getZ();
            if(dx*dx+dz*dz<(long)CONFIG.minimumDistanceFromSpawn*CONFIG.minimumDistanceFromSpawn)continue;
            BlockPos borderMin=new BlockPos(x,70,z);
            BlockPos borderMax=new BlockPos(x+site.width()-1,70,z+site.depth()-1);
            if(!level.getWorldBorder().isWithinBounds(borderMin)||!level.getWorldBorder().isWithinBounds(borderMax))continue;
            if(WorldSiteSafety.reservedCollision(ledger,site,x,z))continue;
            SiteLedger.Job j=new SiteLedger.Job(key,site.id(),"minecraft:overworld",x,z);
            ledger.insert(j);
            LOG.info("Surveying site {} at {},{} size {}x{}",key,x,z,site.width(),site.depth());
            return true;
        }
        return false;
    }
    private static void process(ServerLevel level,SiteLedger ledger,SiteLedger.Job job) {
        SiteCatalog.Site site=SITES.stream().filter(s->s.id().equals(job.site)).findFirst().orElse(null);
        if(site==null){job.stage=SiteLedger.Stage.NEEDS_REPAIR;job.reason="MISSING_SITE";ledger.changed();return;}
        try{
            if(job.stage==SiteLedger.Stage.SURVEY){
                for(int i=0;i<CONFIG.scanSamplesPerStep&&job.stage==SiteLedger.Stage.SURVEY;i++)
                    WorldSiteSafety.surveyOne(level,ledger,job,site);
                return;
            }
            if(job.stage==SiteLedger.Stage.DENSE_SURVEY){denseSurveyStep(level,ledger,job,site);return;}
            if(job.stage==SiteLedger.Stage.PAINT_SURVEY){paintSurveyStep(level,ledger,job,site);return;}
            if(job.stage==SiteLedger.Stage.FOUNDATIONS){foundationStep(level,ledger,job,site);return;}
            if(job.stage==SiteLedger.Stage.PAINT){paintStep(level,ledger,job,site);return;}
            if(job.stage==SiteLedger.Stage.PIECES){pieceStep(level,ledger,job,site);}
        }catch(Exception error){
            job.stage=SiteLedger.Stage.NEEDS_REPAIR;
            job.reason=error.getClass().getSimpleName()+": "+error.getMessage();
            ledger.changed();LOG.error("Worldgen halted at {}. NO automatic retry. Check backup.",job.key,error);
        }
    }
    /** Full-column occupancy check of EVERY future building footprint, before touching any blocks. */
    private static void denseSurveyStep(ServerLevel level,SiteLedger ledger,SiteLedger.Job job,SiteCatalog.Site site) {
        int remaining=CONFIG.blocksPerStep;
        while(remaining>0 && job.foundationIndex<site.pieces().size()){
            SiteCatalog.Piece piece=site.pieces().get(job.foundationIndex);
            StructureTemplate tpl=level.getServer().getStructureManager().get(piece.template())
                    .orElseThrow(()->new IllegalStateException("Missing structure "+piece.template()));
            int w=tpl.getSize().getX(),d=tpl.getSize().getZ();
            if(w<1||d<1||piece.x()<0||piece.z()<0 || piece.x()+w>site.width()+4 || piece.z()+d>site.depth()+4)
                throw new IllegalStateException("Bad piece bounds: "+piece.template());
            while(remaining>0&&job.foundationCursor<w*d){
                int column=job.foundationCursor;
                int x=job.x+piece.x()+column/d,z=job.z+piece.z()+column%d;
                if(!WorldSiteSafety.ready(level,x,z))return;
                job.foundationCursor++; // Never create missing chunks on server tick.
                int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
                BlockPos pos=new BlockPos(x,y,z);
                // Verify building footprint in three dimensions, not merely its ground surface.
                int roofY=job.baseY+piece.y()+tpl.getSize().getY()+1;
                if(y>job.baseY+1||job.baseY-y>site.permittedRelief()+1||
                        !WorldSiteSafety.natural(level.getBlockState(pos))||
                        !WorldSiteSafety.solidBase(level,x,z,y)||
                        !WorldSiteSafety.clearAirspace(level,x,z,y,roofY)||
                        !level.structureManager().getAllStructuresAt(pos).isEmpty()){
                    WorldSiteSafety.reject(ledger,job,"DENSE_VOLUME_CONFLICT "+x+","+z);return;
                }
                remaining--;
            }
            if(job.foundationCursor>=w*d){job.foundationIndex++;job.foundationCursor=0;}
        }
        if(job.foundationIndex==site.pieces().size()){
            // The following stage reuses the same persistent cursors for foundations.
            job.foundationCursor=0;job.foundationIndex=0;
            job.stage=SiteLedger.Stage.PAINT_SURVEY;
        }
        ledger.changed();
    }
    /** Inspect every road and wall tile before ANY world mutation (not just broad samples). */
    private static void paintSurveyStep(ServerLevel level,SiteLedger ledger,SiteLedger.Job job,SiteCatalog.Site site){
        int remaining=CONFIG.blocksPerStep;
        while(remaining>0&&job.paintIndex<site.paints().size()){
            var paint=site.paints().get(job.paintIndex);
            while(remaining>0&&job.paintCursor<paint.size()){
                var at=paint.at(job.paintCursor);
                // Inspect each column just once (painting a wall has several vertical blocks).
                if(at.y()!=paint.ay()){job.paintCursor++;remaining--;continue;}
                int wx=job.x+at.x(),wz=job.z+at.z();
                if(!WorldSiteSafety.ready(level,wx,wz))return;
                job.paintCursor++;
                int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,wx,wz)-1;
                BlockPos ground=new BlockPos(wx,y,wz);
                int wallTop=job.baseY+paint.by();
                if(y>job.baseY+1 || job.baseY-y>site.permittedRelief()+2||
                        !WorldSiteSafety.natural(level.getBlockState(ground))||
                        !WorldSiteSafety.solidBase(level,wx,wz,y)||
                        !WorldSiteSafety.clearAirspace(level,wx,wz,y,wallTop)||
                        !level.structureManager().getAllStructuresAt(ground).isEmpty()){
                    WorldSiteSafety.reject(ledger,job,"WALL_OR_ROAD_PROTECTED "+wx+","+wz);return;
                }
                remaining--;
            }
            if(job.paintCursor>=paint.size()){job.paintIndex++;job.paintCursor=0;}
        }
        if(job.paintIndex==site.paints().size()){
            job.paintIndex=0;job.paintCursor=0;job.stage=SiteLedger.Stage.FOUNDATIONS;
        }
        ledger.changed();
    }
    /** Build a supporting pad under each distinct building footprint; per-tick cap prevents huge edits. */
    private static void foundationStep(ServerLevel level,SiteLedger ledger,SiteLedger.Job job,SiteCatalog.Site site) {
        if(job.foundationIndex==0 && job.foundationCursor==0){
            if(!WorldSiteSafety.safeInitialArea(level,ledger,job,site))return;
        }
        int remaining=CONFIG.blocksPerStep;
        while(remaining>0&&job.foundationIndex<site.pieces().size()){
            SiteCatalog.Piece p=site.pieces().get(job.foundationIndex);
            StructureTemplate tpl=level.getServer().getStructureManager().get(p.template())
                .orElseThrow(()->new IllegalStateException("Missing template "+p.template()));
            int width=tpl.getSize().getX(),depth=tpl.getSize().getZ();
            if(width<1||depth<1||p.x()<0||p.z()<0||p.x()+width>site.width()+4||p.z()+depth>site.depth()+4)
                throw new IllegalStateException("Illegal footprint "+p.template());
            int total=width*depth;
            while(remaining>0&&job.foundationCursor<total){
                int column=job.foundationCursor;
                int wx=job.x+p.x()+column/depth;
                int wz=job.z+p.z()+column%depth;
                if(!WorldSiteSafety.ready(level,wx,wz))return;
                job.foundationCursor++;
                int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,wx,wz)-1;
                if(surface>job.baseY+1)
                    throw new IllegalStateException("Unexpected hill under building at "+wx+","+wz);
                // Only bridge small gaps; deep caves/voids are excluded during survey.
                for(int y=Math.max(surface+1,job.baseY-5);y<=job.baseY;y++){
                    BlockPos pos=new BlockPos(wx,y,wz);
                    if(level.getBlockState(pos).isAir()||level.getBlockState(pos).canBeReplaced())
                        level.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),2);
                }
                remaining--;
            }
            if(job.foundationCursor>=total){job.foundationCursor=0;job.foundationIndex++;}
        }
        if(job.foundationIndex==site.pieces().size())job.stage=SiteLedger.Stage.PAINT;
        ledger.changed();
    }
    private static void paintStep(ServerLevel level,SiteLedger ledger,SiteLedger.Job job,SiteCatalog.Site site) {
        int left=CONFIG.blocksPerStep;
        while(left>0&&job.paintIndex<site.paints().size()){
            var paint=site.paints().get(job.paintIndex);
            Block block=BuiltInRegistries.BLOCK.get(paint.block());
            if(!BuiltInRegistries.BLOCK.containsKey(paint.block()))throw new IllegalArgumentException("Missing block "+paint.block());
            while(left>0&&job.paintCursor<paint.size()) {
                var at=paint.at(job.paintCursor);
                BlockPos pos=new BlockPos(job.x+at.x(),job.baseY+at.y(),job.z+at.z());
                if(!WorldSiteSafety.ready(level,pos.getX(),pos.getZ()))return;
                job.paintCursor++;
                if(at.y()==0){
                    for(int under=1;under<=5;under++){
                        BlockPos below=pos.below(under);
                        if(level.getBlockState(below).isAir() || level.getBlockState(below).canBeReplaced())
                            level.setBlock(below,net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),2);
                        else break;
                    }
                }
                level.setBlock(pos,block.defaultBlockState(),2);
                left--;
            }
            if(job.paintCursor>=paint.size()){job.paintCursor=0;job.paintIndex++;}
        }
        if(job.paintIndex==site.paints().size()){job.stage=SiteLedger.Stage.PIECES;job.pieceIndex=0;}
        ledger.changed();
    }
    private static void pieceStep(ServerLevel level,SiteLedger ledger,SiteLedger.Job job,SiteCatalog.Site site){
        if(job.pieceIndex==site.pieces().size()){
            job.stage=SiteLedger.Stage.COMPLETE;ledger.changed();LOG.info("MURIM site complete {}",job.key);return;
        }
        SiteCatalog.Piece piece=site.pieces().get(job.pieceIndex);
        Optional<StructureTemplate> o=level.getServer().getStructureManager().get(piece.template());
        if(o.isEmpty())throw new IllegalStateException("Missing template: "+piece.template());
        StructureTemplate template=o.get();
        if(template.getSize().getX()<1||template.getSize().getZ()<1)throw new IllegalStateException("Empty template");
        BlockPos origin=new BlockPos(job.x+piece.x(),job.baseY+piece.y(),job.z+piece.z());
        if(piece.x()<0||piece.z()<0||piece.x()+template.getSize().getX()>site.width()+4||
                piece.z()+template.getSize().getZ()>site.depth()+4)
            throw new IllegalStateException("Template exceeds reserved site: "+piece.template());
        // A whole piece is atomic with respect to loaded chunks; never write half of an unloaded piece.
        if(!WorldSiteSafety.allReady(level,origin.getX(),origin.getZ(),
                origin.getX()+template.getSize().getX()-1,
                origin.getZ()+template.getSize().getZ()-1))return;
        // All edits on main server thread. In the event of partial exceptions, STOP and mark for repair.
        if(!template.placeInWorld(level,origin,origin,new StructurePlaceSettings().setIgnoreEntities(true),level.getRandom(),2))
            throw new IllegalStateException("placeInWorld returned false: "+piece.template());
        job.pieceIndex++;ledger.changed();
    }
}
