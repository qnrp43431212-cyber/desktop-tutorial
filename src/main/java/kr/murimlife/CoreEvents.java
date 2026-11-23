package kr.murimlife;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = MurimLife.MOD_ID)
public final class CoreEvents {
    private CoreEvents() {}
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Rolls once when first accessed; SavedData is persisted to the world save.
        CultWorldData.get(event.getServer());
    }
    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().getServer() == null || event.getEntity().level().isClientSide()) return;
        CultWorldData data = CultWorldData.get(event.getEntity().getServer());
        String state = data.demonExists() && data.bloodExists() ? "마교·혈교 공존"
            : data.demonExists() ? "마교 존재 / 혈교 미창설" : "혈교 존재 / 마교 미창설";
        event.getEntity().displayClientMessage(Component.literal("[무림] 이 강호의 세력: " + state), false);
    }
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Direct or projectile PvP from the same vanilla scoreboard team is cancelled.
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        // Only the logical server may authoritatively update per-world player state.
        var server = target.getServer();
        if (server == null) return;
        if (attacker.getTeam() != null && attacker.getTeam() == target.getTeam()) {
            event.setCanceled(true);
            return;
        }
        // Timed parry. Server checks both players' current profiles, not client claims.
        var state = MurimProgressData.get(server);
        var profile = state.player(target.getUUID(), target.getName().getString());
        long now = target.serverLevel().getGameTime();
        if (now <= profile.parryUntil && profile.fatigue < 90) {
            event.setCanceled(true);
            profile.parryUntil = 0;
            var level = target.serverLevel();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(), 25, .4, .5, .4, .13);
            attacker.push(attacker.getX() - target.getX(), .12, attacker.getZ() - target.getZ());
            profile.counters.merge("perfect_parry", 1, Integer::sum);
            EpithetAwards.evaluate(target.getUUID(), profile, state);
            state.setDirty();
            target.displayClientMessage(Component.literal("[무림] 패링 성공! 무기 충돌!"), true);
        }
    }
}
