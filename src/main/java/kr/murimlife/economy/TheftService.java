package kr.murimlife.economy;

import kr.murimlife.MurimNetwork;
import kr.murimlife.MurimProgressData;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.dialogue.NpcRoles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

/** Five seconds of REAL right-click presses. The server owns target, clock, score and payout. */
public final class TheftService {
    private record Session(UUID target,long start,int clicks,long lastClick){}
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    private TheftService(){}
    public static boolean active(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
    public static int realm(LivingEntity n){
        if(n.getPersistentData().contains("MurimRealmIndex"))return Math.clamp(n.getPersistentData().getInt("MurimRealmIndex"),0,7);
        if(n.getPersistentData().getBoolean("MurimLeader"))return 5;
        String label=n.getPersistentData().getString("MurimRealm");
        for(int i=7;i>=0;i--)if(label.contains(new String[]{"삼류","이류","일류","절정","초절정","화경","현경","생사경"}[i]))return i;
        return 0;
    }
    public static int pocket(LivingEntity npc){
        var tag=npc.getPersistentData();
        if(!tag.contains("MurimPocketMoney")){
            int value=CurrencyRules.pocketReward(realm(npc),NpcRoles.role(npc)==NpcRoles.Role.MERCHANT,npc.getUUID().hashCode());
            tag.putInt("MurimPocketMoney",value);
        }
        return Math.max(0,tag.getInt("MurimPocketMoney"));
    }
    public static boolean start(ServerPlayer player,LivingEntity target){
        if(!NpcRoles.supported(target)||player.distanceToSqr(target)>25||!target.isAlive()||
                target.level()!=player.level()||active(player)||target.getPersistentData().getBoolean("MurimOpportunityEnemy"))return false;
        long day=player.serverLevel().getGameTime()/24000L;
        if(target.getPersistentData().getLong("MurimLastTheftDay")==day && target.getPersistentData().getBoolean("MurimTheftTried")){
            MoneyService.message(player,"오늘 이미 이 사람을 노렸습니다. 다른 날을 기다리십시오.");return false;
        }
        ACTIVE.put(player.getUUID(),new Session(target.getUUID(),player.serverLevel().getGameTime(),0,-10000));
        PacketDistributor.sendToPlayer(player,new MurimNetwork.TheftStatus(true));
        MoneyService.message(player,"소매치기 시작! 5초 동안 마우스 우클릭을 빠르게 반복하십시오. 5블록 내에 머물러야 합니다.");
        return true;
    }
    public static void click(ServerPlayer player){
        var s=ACTIVE.get(player.getUUID());if(s==null)return;
        long now=player.serverLevel().getGameTime();
        if(now>=s.start()+100||now-s.lastClick()<2)return; // 10 clicks/s cap; prevents packet flood.
        var entity=player.serverLevel().getEntity(s.target());
        if(!(entity instanceof LivingEntity victim)||!victim.isAlive()||player.distanceToSqr(victim)>25)return;
        ACTIVE.put(player.getUUID(),new Session(s.target(),s.start(),Math.min(50,s.clicks()+1),now));
    }
    public static void tick(ServerTickEvent.Post event){
        for(var entry:List.copyOf(ACTIVE.entrySet())){
            ServerPlayer p=event.getServer().getPlayerList().getPlayer(entry.getKey());
            if(p==null){ACTIVE.remove(entry.getKey());continue;}
            var session=entry.getValue();long now=p.serverLevel().getGameTime();
            var entity=p.serverLevel().getEntity(session.target());
            if(!(entity instanceof LivingEntity npc)||!npc.isAlive()||p.distanceToSqr(npc)>25){
                ACTIVE.remove(entry.getKey());PacketDistributor.sendToPlayer(p,new MurimNetwork.TheftStatus(false));
                MoneyService.message(p,"대상을 놓쳐 소매치기가 중단되었습니다.");continue;
            }
            if(now-session.start()<100){
                if((now-session.start())%20==0)p.displayClientMessage(Component.literal(
                    "[소매치기] 남은 "+Math.max(0,5-(now-session.start())/20)+"초 · 우클릭 "+session.clicks()+"회"),true);
                continue;
            }
            ACTIVE.remove(entry.getKey());PacketDistributor.sendToPlayer(p,new MurimNetwork.TheftStatus(false));
            var data=MurimProgressData.get(p.server);var prof=data.player(p.getUUID(),p.getName().getString());
            int victimRealm=realm(npc);double chance=CurrencyRules.theftChance(
                prof.attributes.getOrDefault("민첩",10.0),prof.attributes.getOrDefault("기교",10.0),
                prof.attributes.getOrDefault("감각",10.0),session.clicks(),prof.realm,victimRealm);
            // Stop a player from earning infinite money from the same persistent NPC.
            npc.getPersistentData().putBoolean("MurimTheftTried",true);
            npc.getPersistentData().putLong("MurimLastTheftDay",now/24000L);
            boolean success=session.clicks()>=5 && p.getRandom().nextDouble()<chance && pocket(npc)>0;
            if(success){
                int wealth=pocket(npc);int acquired=Math.min(wealth,Math.max(1,wealth*(15+Math.min(45,session.clicks()*2))/100));
                npc.getPersistentData().putInt("MurimPocketMoney",wealth-acquired);
                MoneyService.add(p,acquired,"소매치기 성공 (성공 확률 "+Math.round(chance*100)+"%)");
                data.evilDeed(p.getUUID(),prof,2);
            }else{
                data.evilDeed(p.getUUID(),prof,5);
                MoneyService.message(p,"소매치기 실패! 악업 +5, 상대가 경계합니다. 성공 확률 "+Math.round(chance*100)+"%");
                if(npc instanceof MurimHumanoid fighter)fighter.setTarget(p);
            }
        }
    }
}
