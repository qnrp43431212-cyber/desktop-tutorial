package kr.murimlife.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** ALPHA: visible NPC-v-NPC / player-v-NPC training bout with live crowd.
 * Uses actual Murim humanoids, Epic Fight animated combat AI and wuxia skins. Runtime verification pending.
 * Temporary attendees are discarded after a match and on chunk-load after a server restart.
 */
public final class NpcArenaService {
    private record Seat(int x,int y,int z,int quadrant){}
    private static final class Session {
        final ServerLevel level;
        final UUID id=UUID.randomUUID();
        final BlockPos center;
        final boolean playerMatch;
        final UUID player;
        final long endsAt;
        final ArrayList<UUID> participants=new ArrayList<>();
        final ArrayList<UUID> spectators=new ArrayList<>();
        int seatCursor=0, combatTicks=0;
        Session(ServerLevel level, BlockPos center,boolean playerMatch,ServerPlayer p,int duration){
            this.level=level;this.center=center;this.playerMatch=playerMatch;this.player=p.getUUID();
            this.endsAt=level.getGameTime()+duration;
        }
    }
    private static Session active;
    private static List<Seat> seats;
    private static NpcNameCatalog names;
    private NpcArenaService(){}
    private static List<Seat> seatList(){
        if(seats!=null)return seats;
        try(var stream=NpcArenaService.class.getResourceAsStream("/data/murimlife/npc/arena_spectator_seats.json")){
            if(stream==null)throw new IllegalStateException("Missing seating file");
            var root=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
            ArrayList<Seat> loaded=new ArrayList<>();
            for(var node:root.getAsJsonArray("seats")){
                JsonObject j=node.getAsJsonObject();JsonArray off=j.getAsJsonArray("offset");
                loaded.add(new Seat(off.get(0).getAsInt()-27,off.get(1).getAsInt()-2,off.get(2).getAsInt()-27,
                       j.get("quarter").getAsInt()));
            }
            if(loaded.size()<80)throw new IllegalStateException("Not enough seating positions");
            // Interleave four stands for visibly even crowd distribution on slow servers.
            ArrayList<Seat> interleaved=new ArrayList<>();
            for(int j=0;j<loaded.size();j++){
                int q=j%4,n=j/4, seen=0;
                for(Seat s:loaded){
                    if(s.quadrant!=q)continue;
                    if(seen++==n){interleaved.add(s);break;}
                }
            }
            seats=List.copyOf(interleaved);return seats;
        }catch(Exception ex){throw new IllegalStateException("Arena seating invalid",ex);}
    }
    private static MurimHumanoid createTemp(ServerLevel level,BlockPos pos,String name,UUID id,boolean spectator){
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir())return null;
        if(level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).isEmpty())return null;
        if(!level.getEntitiesOfClass(Entity.class,new AABB(pos).inflate(.25,.1,.25)).isEmpty())return null;
        MurimHumanoid npc=new MurimHumanoid(MurimNpcTypes.HUMAN.get(),level);
        npc.setSkin(NpcSkinSelector.forName(name,"arena:"+pos.asLong()));
        npc.setDuel(!spectator);
        if(!spectator){
            npc.setWeaponStyle(name.equals("백권사")?kr.murimlife.npc.combat.WeaponTactics.Style.FIST:
                kr.murimlife.npc.combat.WeaponTactics.Style.SWORD);
        }
        npc.setUUID(UUID.randomUUID());npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        npc.setCustomName(Component.literal(name));npc.setCustomNameVisible(true);
        if(spectator){npc.setNoAi(true);npc.setInvulnerable(true);npc.setSilent(true);npc.setCustomNameVisible(false);}
        npc.getPersistentData().putBoolean("MurimLifeArenaTemporary",true);
        npc.getPersistentData().putUUID("MurimLifeArenaSession",id);
        npc.getPersistentData().putBoolean("MurimLifeSpectator",spectator);
        if(!level.addFreshEntity(npc))return null;
        return npc;
    }
    /** Player must stand on the CENTRAL polished diorite of the existing arena template. */
    public static String start(ServerPlayer player,boolean playerVsNpc){
        var cfg=NpcResidentService.config();
        if(!cfg.enableTournamentDemo)return "config/murimlife-npc.json: enableTournamentDemo=true 후 재시작하세요.";
        ServerLevel world=player.serverLevel();
        if(active!=null)return "현재 진행 중인 시험 경기가 있습니다. /murim arena stop";
        if(world!=player.server.overworld())return "시험 대회는 오버월드에서만 가능합니다.";
        BlockPos center=player.blockPosition();
        if(!world.getBlockState(center.below()).is(Blocks.POLISHED_DIORITE))
            return "55×55 기존 무림대회장 중앙의 광택 섬록암 바닥에서 실행하세요.";
        if(names==null)names=NpcNameCatalog.load();
        List<Seat> available=seatList();
        Session s=new Session(world,center,playerVsNpc,player,cfg.tournamentMaxDurationTicks);
        active=s; // register session before firing the EntityJoinLevelEvent for temporary entities
        BlockPos pA=center.offset(-3,0,0),pB=center.offset(3,0,0);
        MurimHumanoid a=createTemp(world,pA,"청검객",s.id,false);
        if(a==null){active=null;return "대련장 왼쪽 출전 위치가 막혔습니다.";}
        s.participants.add(a.getUUID());
        if(playerVsNpc)a.duelWith(player);
        if(!playerVsNpc){
            MurimHumanoid b=createTemp(world,pB,"백권사",s.id,false);
            if(b==null){stopInternal();return "대련장 오른쪽 출전 위치가 막혔습니다.";}
            s.participants.add(b.getUUID());
            a.duelWith(b);b.duelWith(a);
        }
        announce(s,playerVsNpc?"플레이어 대련 개시! 관중들이 착석합니다.":"무림인들의 NPC 대련 개시! 관중들이 착석합니다.");
        return "시험 경기 시작. 관중 최대 "+Math.min(cfg.tournamentSpectatorCap,available.size())+"명. "+
               (playerVsNpc?"상대 무림인을 공격할 수 있습니다.":"가만히 서서 NPC끼리의 공방을 관람할 수 있습니다.");
    }
    private static void announce(Session session,String message){
        for(ServerPlayer p:session.level.players()){
            if(p.blockPosition().distSqr(session.center)<=70*70)
                p.sendSystemMessage(Component.literal("[무림비무] "+message));
        }
    }
    public static void onTick(ServerTickEvent.Post event){
        Session session=active;if(session==null)return;
        var cfg=NpcResidentService.config();
        if(!cfg.enableTournamentDemo||session.level.getServer()!=event.getServer()){
            stopInternal();return;
        }
        if(session.level.getGameTime()>=session.endsAt){announce(session,"경기 종료 — 관중이 귀가합니다.");stopInternal();return;}
        ServerPlayer host=event.getServer().getPlayerList().getPlayer(session.player);
        if(host==null||host.serverLevel()!=session.level||host.distanceToSqr(session.center.getX(),session.center.getY(),session.center.getZ())>70*70){
            stopInternal();return;
        }
        // Bounded per-tick spawning, no synchronous generation / forceload.
        int allowed=NpcLifecycleRules.tournamentCrowdSize(cfg.tournamentSpectatorCap,seatList().size(),80);
        for(int n=0;n<2 && session.spectators.size()<allowed && session.seatCursor<seatList().size();n++){
            var seat=seatList().get(session.seatCursor++);
            BlockPos pos=session.center.offset(seat.x,seat.y,seat.z);
            var floor=session.level.getBlockState(pos.below());
            if(!floor.is(Blocks.SPRUCE_STAIRS) && !floor.is(Blocks.POLISHED_ANDESITE))continue;
            if(!session.level.hasChunkAt(pos))continue;
            String display=NpcResidentService.displayName(names.audienceName(session.seatCursor));
            MurimHumanoid npc=createTemp(session.level,pos,display,session.id,true);
            if(npc!=null)session.spectators.add(npc.getUUID());
        }
        session.combatTicks++;
        if(session.participants.isEmpty()){stopInternal();return;}
        Entity first=session.level.getEntity(session.participants.get(0));
        if(!(first instanceof MurimHumanoid a)||!a.isAlive()){announce(session,"승부가 결정되었습니다!");stopInternal();return;}
        if(kr.murimlife.npc.dialogue.MurimDialogueService.isTalking(a))return;
        if(session.playerMatch){
            if(!host.isAlive()||host.getHealth()<=4){announce(session,"플레이어의 기권 또는 패배로 비무 종료");stopInternal();return;}
            if(a.getTarget()!=host)a.duelWith(host);
        } else {
            if(session.participants.size()<2){stopInternal();return;}
            Entity second=session.level.getEntity(session.participants.get(1));
            if(!(second instanceof MurimHumanoid b)||!b.isAlive()){
                announce(session,"승부가 결정되었습니다!");stopInternal();return;
            }
            if(a.getHealth()<=3||b.getHealth()<=3){announce(session,"비무 승부 결정!");stopInternal();return;}
            if(!kr.murimlife.npc.dialogue.MurimDialogueService.isTalking(a) && a.getTarget()!=b)a.duelWith(b);
            if(!kr.murimlife.npc.dialogue.MurimDialogueService.isTalking(b) && b.getTarget()!=a)b.duelWith(a);
        }
        // Actual hits and attack animations are handled by Epic Fight AnimatedAttackGoal.
        // No artificial scripted damage. Crowd particles are visual reactions only.
        if(session.combatTicks%70==0)cheer(session);
    }
    /** Keep the host alive in a temporary training duel. */
    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){
        Session session=active;
        if(session==null||!session.playerMatch)return;
        if(!(e.getEntity() instanceof ServerPlayer host) || !host.getUUID().equals(session.player))return;
        if(!(e.getSource().getEntity() instanceof MurimHumanoid attacker))return;
        if(!session.participants.contains(attacker.getUUID()))return;
        if(host.getHealth() - e.getAmount()<=4) {
            e.setCanceled(true);
            announce(session,"플레이어 체력이 낮아져 안전하게 비무를 종료합니다.");
            stopInternal();
        }
    }
    private static void cheer(Session session){
        if(session.spectators.isEmpty())return;
        for(int i=0;i<Math.min(5,session.spectators.size());i++){
            int idx=Math.floorMod(session.combatTicks+i*11,session.spectators.size());
            Entity e=session.level.getEntity(session.spectators.get(idx));
            if(e!=null)session.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                e.getX(),e.getY()+1.5,e.getZ(),1,.12,.15,.12,0);
        }
    }
    private static void particles(ServerLevel world,double x,double y,double z){
        world.sendParticles(ParticleTypes.SWEEP_ATTACK,x,y,z,2,.2,.15,.2,0);
    }
    /** End a training bout peacefully without restarting combat after the negotiation. */
    public static void finishByNegotiation(MurimHumanoid npc,ServerPlayer player){
        Session s=active;
        if(s!=null && s.playerMatch && s.player.equals(player.getUUID())
                && s.participants.contains(npc.getUUID())){
            announce(s,"서로의 뜻이 맞아 무승부로 경기를 마쳤습니다.");
            stopInternal();
        }
    }
    public static String stop(){if(active==null)return "진행 중인 경기가 없습니다.";
        stopInternal();return "경기를 종료하고 현재 로딩된 관중과 출전자를 정리했습니다.";}
    private static void stopInternal(){
        Session s=active;active=null;
        if(s==null)return;
        for(UUID id:s.spectators){Entity e=s.level.getEntity(id);if(e!=null)e.discard();}
        for(UUID id:s.participants){Entity e=s.level.getEntity(id);if(e!=null)e.discard();}
        // Unloaded temporary entities are removed via onEntityJoin when their chunk is loaded again.
    }
    /** Clean up transient spectators/fighters after a crash/restart or a completed match. */
    public static void onEntityJoin(EntityJoinLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel))return;
        Entity e=event.getEntity();
        var tag=e.getPersistentData();
        if(!tag.getBoolean("MurimLifeArenaTemporary"))return;
        UUID oldSession=tag.getUUID("MurimLifeArenaSession");
        if(active==null||!active.id.equals(oldSession))e.discard();
    }
}
