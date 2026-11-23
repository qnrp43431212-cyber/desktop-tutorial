package kr.murimlife.opportunity;

import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.MurimNpcTypes;
import kr.murimlife.npc.combat.WeaponTactics;
import kr.murimlife.worldgen.SiteLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Twelve rare self-contained structures: 6 embedded in natural caves, 6 crumbling surface ruins.
 * Server-side generation ONLY after nearby chunks load, without forceloading or replacing known
 * constructions. Deterministic cells and persistent records prevent duplicate placements.
 */
public final class OpportunitySites extends SavedData {
    public record Kind(String id,String korean,boolean cave,int danger,int trap){ }
    public static final List<Kind> KINDS=List.of(
        new Kind("jade_marrow_grotto","옥수 동굴",true,1,0),
        new Kind("forgotten_sword_tomb","망각된 검총",true,3,1),
        new Kind("poison_herb_cavern","독초 암굴",true,2,2),
        new Kind("frozen_meditation_den","한빙 수련굴",true,3,3),
        new Kind("sealed_demon_chamber","봉마 석실",true,5,4),
        new Kind("underground_miner_vault","암맥 비고",true,4,5),
        new Kind("collapsed_shrine","무너진 산신묘",false,1,1),
        new Kind("abandoned_sword_pavilion","폐검각",false,3,4),
        new Kind("ruined_herbal_clinic","폐약방",false,2,2),
        new Kind("ancient_duel_arena","옛 비무장",false,4,5),
        new Kind("forsaken_watchpost","버려진 봉화대",false,3,0),
        new Kind("looted_caravan_inn","약탈된 객잔",false,2,3));
    public record Site(String key,int type,int x,int y,int z) { }
    private final Set<String> visited=new HashSet<>();
    private final List<Site> sites=new ArrayList<>();
    private static long ticks;
    private static OpportunityConfig config;
    private OpportunitySites(){}
    private static OpportunitySites load(CompoundTag tag,HolderLookup.Provider provider){
        OpportunitySites out=new OpportunitySites();
        var v=tag.getList("Visited",Tag.TAG_STRING);
        for(int i=0;i<v.size();i++)out.visited.add(v.getString(i));
        var all=tag.getList("Sites",Tag.TAG_COMPOUND);
        for(int i=0;i<all.size();i++){
            var n=all.getCompound(i);int type=n.getInt("Type");
            if(type>=0&&type<KINDS.size())out.sites.add(new Site(n.getString("Key"),type,n.getInt("X"),n.getInt("Y"),n.getInt("Z")));
        }
        return out;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){
        var vis=new ListTag();for(String key:visited)vis.add(net.minecraft.nbt.StringTag.valueOf(key));tag.put("Visited",vis);
        var all=new ListTag();for(Site s:sites){var n=new CompoundTag();n.putString("Key",s.key());n.putInt("Type",s.type());n.putInt("X",s.x());n.putInt("Y",s.y());n.putInt("Z",s.z());all.add(n);}tag.put("Sites",all);
        return tag;
    }
    public static OpportunitySites get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(
        new SavedData.Factory<>(OpportunitySites::new,OpportunitySites::load),"murimlife_opportunities_v123");}
    public List<Site> all(){return List.copyOf(sites);}
    public Optional<Site> closest(int x,int z){return sites.stream().min(Comparator.comparingLong(s->{long dx=(long)x-s.x(),dz=(long)z-s.z();return dx*dx+dz*dz;}));}
    public boolean containsSiteNear(int x,int z,int range){long limit=(long)range*range;return sites.stream().anyMatch(s->{long dx=(long)x-s.x(),dz=(long)z-s.z();return dx*dx+dz*dz<=limit;});}
    private static long mix(long seed,int x,int z){
        long v=seed^((long)x*0x9E3779B97F4A7C15L)^((long)z*0xC2B2AE3D27D4EB4FL);
        v^=v>>>30;v*=0xBF58476D1CE4E5B9L;v^=v>>>27;v*=0x94D049BB133111EBL;return v^(v>>>31);
    }
    private static boolean loaded(ServerLevel w,int x,int z,int radius){
        return w.hasChunkAt(new BlockPos(x-radius,64,z-radius))&&w.hasChunkAt(new BlockPos(x+radius,64,z-radius))&&
               w.hasChunkAt(new BlockPos(x-radius,64,z+radius))&&w.hasChunkAt(new BlockPos(x+radius,64,z+radius));
    }
    private static boolean stone(BlockState state){
        Block b=state.getBlock();return b==Blocks.STONE||b==Blocks.DEEPSLATE||b==Blocks.TUFF||b==Blocks.ANDESITE||
               b==Blocks.DIORITE||b==Blocks.GRANITE||b==Blocks.CALCITE||b==Blocks.DRIPSTONE_BLOCK||
               b==Blocks.DIRT||b==Blocks.GRASS_BLOCK||b==Blocks.GRAVEL||b==Blocks.COARSE_DIRT||
               b==Blocks.MOSS_BLOCK||b==Blocks.ROOTED_DIRT;
    }
    private static boolean harmless(BlockState state){return state.isAir()||stone(state)||state.is(Blocks.CAVE_AIR)||state.is(Blocks.GLOW_LICHEN)||state.is(Blocks.POINTED_DRIPSTONE)||
        state.is(Blocks.SHORT_GRASS)||state.is(Blocks.TALL_GRASS)||state.is(Blocks.FERN)||state.is(Blocks.DEAD_BUSH)||
        state.is(Blocks.SNOW);}
    private static boolean clear(ServerLevel w,BlockPos base,boolean cave){
        int naturalFloor=0;
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            BlockPos bottom=base.offset(dx,-1,dz);
            if(cave){
                if(!harmless(w.getBlockState(bottom)))return false;
                if(stone(w.getBlockState(bottom)))naturalFloor++;
            }else{
                int height=w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,bottom.getX(),bottom.getZ());
                if(Math.abs(height-base.getY())>2 || !stone(w.getBlockState(new BlockPos(bottom.getX(),height-1,bottom.getZ()))))return false;
            }
            // Survey every part before modifying anything. Player blocks and vanilla structures are never replaced.
            for(int h=0;h<=5;h++){
                var s=w.getBlockState(base.offset(dx,h,dz));
                if(!harmless(s))return false;
                if(!cave && !s.isAir() && h>=2)return false;
            }
        }
        return !cave || naturalFloor>=65;
    }
    private static BlockPos tryLocation(ServerLevel w,int x,int z,Kind kind){
        if(!loaded(w,x,z,17))return null;
        if(kind.cave()){
            // Require an existing air pocket with natural rock underfoot: a cave, not a new synthetic dimension.
            for(int y:new int[]{16,22,28,34,40,46,52,58,4,-4})for(int dx:new int[]{0,-8,8})for(int dz:new int[]{0,8,-8}){
                BlockPos at=new BlockPos(x+dx,y,z+dz);
                if(at.getY()<w.getMinBuildHeight()+8 || at.getY()>w.getMaxBuildHeight()-9)continue;
                if(!w.getBlockState(at).isAir()||!stone(w.getBlockState(at.below())))continue;
                if(clear(w,at,true))return at;
            }
            return null;
        }
        int top=w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        if(top<w.getSeaLevel()+3||top>w.getMaxBuildHeight()-12)return null;
        BlockPos at=new BlockPos(x,top,z);
        if(clear(w,at,false))return at;
        return null;
    }
    private static Block masonry(int type,int dx,int dz){
        int n=Math.floorMod(type*11+dx*5+dz*13,13);
        if(type==3||type==4||type==5)return n<3?Blocks.COBBLED_DEEPSLATE:n<7?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_BRICKS;
        return n<3?Blocks.MOSSY_STONE_BRICKS:n<7?Blocks.CRACKED_STONE_BRICKS:Blocks.STONE_BRICKS;
    }
    private static void block(ServerLevel w,BlockPos at,Block b){w.setBlock(at,b.defaultBlockState(),2);}
    private static void build(ServerLevel w,BlockPos base,int type,long hash){
        Kind kind=KINDS.get(type);Random random=new Random(hash);
        // v1.24: site-specific broken perimeter rather than a perfect 13x13 square platform.
        // Keep all changes within the already surveyed 13x13x6 area; preserve surrounding rock and terrain.
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++){
            int jitter=Math.floorMod(Objects.hash(hash,type,dx*23,dz*43),11)-5;
            double radial=Math.sqrt(dx*dx*(type%2==0?1.11:.92)+dz*dz*(type%2==0?.92:1.11));
            double edge=4.65+jitter*.11;
            if(radial>edge)continue; // natural rock/vegetation remains outside the organic footprint
            BlockPos floor=base.offset(dx,-1,dz);
            Block stone=masonry(type,dx,dz);
            block(w,floor,stone);
            if((type==0||type==2||type==6||type==8) && Math.floorMod(dx*7+dz*11+type,17)==0)
                block(w,floor,Blocks.MOSS_BLOCK);
            boolean gate=dz<=-3 && Math.abs(dx)<=1;
            boolean interior=radial<3.45||gate;
            int collapsedHeight=1+Math.floorMod(Objects.hash(hash,type*7,dx,dz),4);
            boolean remnant=!gate && radial>=3.45 && radial<edge &&
                Math.floorMod(Objects.hash(hash>>>8,dx*7,dz*13),7)!=0;
            if(kind.cave())collapsedHeight=Math.min(4,collapsedHeight+1);
            for(int h=0;h<=4;h++){
                BlockPos at=base.offset(dx,h,dz);
                if(h==4 && kind.cave())continue; // leave a raw rock ceiling above the shrine
                if(interior || !remnant || h>=collapsedHeight)block(w,at,Blocks.AIR);
                else block(w,at,stone);
            }
        }
        // Clear a genuine north-facing entrance and lay an uneven approach without boxing in the doorway.
        for(int dz=-5;dz<=-3;dz++){
            block(w,base.offset(0,-1,dz),masonry(type,0,dz));
            for(int h=0;h<3;h++)for(int dx=-1;dx<=1;dx++)block(w,base.offset(dx,h,dz),Blocks.AIR);
        }
        // Ruined buttresses, broken altar frames and asymmetric relic pedestals distinguish all 12 sites.
        for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}){
            int height=1+Math.floorMod(Objects.hash(type,dx,dz,hash>>>12),4);
            for(int h=0;h<height;h++)if((dx!=3 || dz!=3 || h<height-1))
                block(w,base.offset(dx,h,dz),masonry(type,dx,dz));
        }
        if(type==1||type==7||type==9)for(int dx=-3;dx<=3;dx++){
            if(dx==0||dx==1)continue; // a passable route to the central treasure
            if(Math.floorMod(type+dx,4)!=0)block(w,base.offset(dx,0,3),masonry(type,dx,3));
        }
        if(type==0||type==2||type==8){
            block(w,base.offset(-3,0,2),Blocks.MOSS_BLOCK);
            block(w,base.offset(3,0,-2),Blocks.MOSS_BLOCK);
        }
        if(type==4||type==10){
            block(w,base.offset(-2,0,-3),Blocks.SOUL_SAND);
            block(w,base.offset(2,0,3),Blocks.SOUL_SAND);
        }
        if(type==6||type==7||type==11){
            // Stumps of collapsed exterior timberwork instead of symmetric square stone boxes.
            block(w,base.offset(-3,1,1),Blocks.STRIPPED_SPRUCE_LOG);
            block(w,base.offset(3,0,-1),Blocks.OAK_SLAB);
        }
        // Hazards use real vanilla damage and slowdown blocks; traps cannot be farmed infinitely.
        switch(kind.trap()){
            case 0 -> {block(w,base.offset(-2,0,-2),Blocks.COBWEB);block(w,base.offset(2,0,1),Blocks.COBWEB);}
            case 1 -> {block(w,base.offset(-1,-1,1),Blocks.MAGMA_BLOCK);block(w,base.offset(2,-1,2),Blocks.MAGMA_BLOCK);}
            case 2 -> {block(w,base.offset(-2,0,-2),Blocks.SWEET_BERRY_BUSH);block(w,base.offset(2,0,2),Blocks.COBWEB);}
            case 3 -> {block(w,base.offset(-1,-1,2),Blocks.POWDER_SNOW);block(w,base.offset(1,-1,-2),Blocks.POWDER_SNOW);}
            case 4 -> {block(w,base.offset(-2,-1,0),Blocks.MAGMA_BLOCK);block(w,base.offset(2,0,0),Blocks.COBWEB);}
            case 5 -> {block(w,base.offset(-2,-1,-2),Blocks.MAGMA_BLOCK);block(w,base.offset(2,-1,2),Blocks.MAGMA_BLOCK);}
        }
        BlockPos treasure=base.offset(0,0,2);
        block(w,treasure,Blocks.CHEST);
        if(w.getBlockEntity(treasure) instanceof ChestBlockEntity chest){
            chest.setItem(11,new ItemStack(Items.EMERALD,2+type));
            chest.setItem(12,new ItemStack(type>=4?Items.DIAMOND:Items.GOLD_INGOT,1+type/3));
            chest.setItem(14,new ItemStack(type%3==0?Items.ENCHANTED_BOOK:type%3==1?Items.EXPERIENCE_BOTTLE:Items.GOLDEN_APPLE,1));
            chest.setItem(15,new ItemStack(Items.IRON_INGOT,3+type));
            chest.setChanged();
        }
        // Actual persistent adversaries occupy the site and defend its treasures.
        int count=1+(type>=4?1:0)+(type==4||type==9?1:0);
        for(int i=0;i<count;i++){
            MurimHumanoid enemy=new MurimHumanoid(MurimNpcTypes.HUMAN.get(),w);
            enemy.setCustomName(Component.literal(type>=4?"[기연 수호자] "+kind.korean():"[기연 도굴꾼] "+kind.korean()));
            enemy.setCustomNameVisible(true);enemy.setSkin(346+Math.floorMod((int)(hash>>>i),96));
            enemy.setWeaponStyle(WeaponTactics.Style.values()[(type+i)%WeaponTactics.Style.values().length]);
            enemy.getPersistentData().putBoolean("MurimOpportunityEnemy",true);
            enemy.getPersistentData().putBoolean("MurimEvil",true);
            enemy.getPersistentData().putInt("MurimRealmIndex",Math.min(6,kind.danger()+i-1));
            enemy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(26+kind.danger()*12);
            enemy.setHealth(enemy.getMaxHealth());
            enemy.moveTo(base.getX()+(i-1)*2.5+.5,base.getY(),base.getZ()-2+.5,0,0);
            enemy.setPersistenceRequired();w.addFreshEntity(enemy);
        }
    }
    public static void tick(ServerTickEvent.Post event){
        if(++ticks%160!=0)return;
        if(config==null)config=OpportunityConfig.read();
        if(!config.enabled||config.maximumSitesPerWorld==0)return;
        ServerLevel world=event.getServer().overworld();OpportunitySites journal=get(world);
        if(journal.sites.size()>=config.maximumSitesPerWorld)return;
        for(ServerPlayer player:world.players()){
            int sx=Math.floorDiv(player.getBlockX(),384),sz=Math.floorDiv(player.getBlockZ(),384);
            for(int ox=-1;ox<=1;ox++)for(int oz=-1;oz<=1;oz++){
                int cellX=sx+ox,cellZ=sz+oz;String key=cellX+":"+cellZ;
                if(journal.visited.contains(key))continue;
                long hash=mix(world.getSeed(),cellX,cellZ);
                if(Math.floorMod(hash,10000)>=Math.round(config.sectorChance*10000)){
                    journal.visited.add(key);journal.setDirty();continue;}
                int type=Math.floorMod((int)(hash>>>32),KINDS.size());
                int x=cellX*384+160+(int)Math.floorMod(hash>>>9,64);
                int z=cellZ*384+160+(int)Math.floorMod(hash>>>19,64);
                long dx=(long)player.getBlockX()-x,dz=(long)player.getBlockZ()-z;
                if(dx*dx+dz*dz>144*144 || dx*dx+dz*dz<40*40)continue;
                if(!loaded(world,x,z,17))continue;
                long spawnDX=(long)world.getSharedSpawnPos().getX()-x,spawnDZ=(long)world.getSharedSpawnPos().getZ()-z;
                if(spawnDX*spawnDX+spawnDZ*spawnDZ<(long)config.minimumDistanceFromSpawn*config.minimumDistanceFromSpawn){journal.visited.add(key);journal.setDirty();continue;}
                if(journal.containsSiteNear(x,z,180)) {journal.visited.add(key);journal.setDirty();continue;}
                boolean reserved=SiteLedger.get(world).jobs().stream().anyMatch(j->
                    j.stage!=SiteLedger.Stage.REJECTED && (long)(j.x-x)*(j.x-x)+(long)(j.z-z)*(j.z-z)<200*200);
                if(reserved){journal.visited.add(key);journal.setDirty();continue;}
                BlockPos at=tryLocation(world,x,z,KINDS.get(type));
                // An unsuccessful site is journaled, not retried blindly every tick in the same chunk.
                journal.visited.add(key);journal.setDirty();
                if(at==null)continue;
                build(world,at,type,hash);
                journal.sites.add(new Site(key,type,at.getX(),at.getY(),at.getZ()));journal.setDirty();
                return; // at most one new structure per tick pass
            }
        }
    }
}
