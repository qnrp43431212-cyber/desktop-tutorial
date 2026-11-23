package kr.murimlife;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.raid.Raider;
import kr.murimlife.economy.MoneyService;
import kr.murimlife.economy.CurrencyRules;
import kr.murimlife.economy.TheftService;
import kr.murimlife.npc.dialogue.NpcRoles;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Runs on server; never trusts client messages as proof of combat achievements. */
@EventBusSubscriber(modid=MurimLife.MOD_ID)
public final class MurimProgressEvents {
    private MurimProgressEvents(){}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){MurimCommands.register(event);}
    @SubscribeEvent public static void onDeath(LivingDeathEvent event) {
        if(!(event.getSource().getEntity() instanceof ServerPlayer attacker) || event.getEntity()==attacker)return;
        var state=MurimProgressData.get(attacker.server);
        var p=state.player(attacker.getUUID(),attacker.getName().getString());
        boolean villain=event.getEntity().getPersistentData().getBoolean("MurimEvil") ||event.getEntity() instanceof Raider;
        boolean civilian=event.getEntity() instanceof Villager || event.getEntity() instanceof WanderingTrader ||
             event.getEntity() instanceof kr.murimlife.npc.MurimHumanoid;
        boolean innocent=civilian&&!villain;
        state.recordVictory(attacker.getUUID(),p,innocent);
        if(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster ||
           event.getEntity().getPersistentData().getBoolean("MurimOpportunityEnemy") ||
           event.getEntity().getPersistentData().getBoolean("MurimCultHostile") ||
           event.getEntity().getPersistentData().getBoolean("MurimArenaFighter"))
            p.counters.merge("combatVictories",1,Integer::sum);
        if(event.getEntity().getPersistentData().getBoolean("MurimOpportunityEnemy")) {
            p.counters.merge("opportunityKills",1,Integer::sum);
            kr.murimlife.economy.SectDiscipline.get(attacker.server).guardianKill(attacker);
        }
        String weapon=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(attacker.getMainHandItem().getItem()).getPath();
        String type=weapon.contains("dao")?"saber_mastery":weapon.contains("spear")?"spear_mastery":(weapon.contains("jian") || weapon.endsWith("sword"))?"sword_mastery":"unarmed_mastery";
        p.counters.merge(type,1,Integer::sum);
        if(villain)state.goodDeed(attacker.getUUID(),p,2+TheftService.realm(event.getEntity()));
        if(innocent && !(event.getEntity() instanceof Villager))state.evilDeed(attacker.getUUID(),p,5);
        if(civilian && !event.getEntity().getPersistentData().getBoolean("MurimLootClaimed") &&
                !event.getEntity().getPersistentData().getBoolean("MurimLifeSpectator") &&
                !event.getEntity().getPersistentData().getBoolean("MurimArenaFighter") &&
                attacker.distanceToSqr(event.getEntity())<=144){
            var victim=event.getEntity();victim.getPersistentData().putBoolean("MurimLootClaimed",true);
            int value=TheftService.pocket(victim);
            int max=CurrencyRules.killLoot(TheftService.realm(victim),NpcRoles.role(victim)==NpcRoles.Role.MERCHANT,
                villain,victim.getUUID().hashCode());
            int loot=Math.min(value,max);
            victim.getPersistentData().putInt("MurimPocketMoney",Math.max(0,value-loot));
            if(loot>0)MoneyService.add(attacker,loot,"상대의 소지금 회수");
        }
        EpithetAwards.evaluate(attacker.getUUID(),p,state);state.setDirty();
        if(innocent)attacker.displayClientMessage(Component.literal("[무림] 무고한 사람을 해침: 악업 증가"),true);
        else if(villain)attacker.displayClientMessage(Component.literal("[무림] 악인 처단: 선업 증가"),true);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var server=event.getServer();long time=server.overworld().getGameTime();
        if(time%24000L!=0)return;
        var state=MurimProgressData.get(server);state.simulateDistantNpcs(server.overworld().getDayTime()/24000L);
    }
}
