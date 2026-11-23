package kr.murimlife.npc.dialogue;

import kr.murimlife.MurimLife;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionResult;

/** ~ is the dialogue key. Bare right-click on supported NPCs cannot open vanilla trading UI. */
@EventBusSubscriber(modid=MurimLife.MOD_ID)
public final class NpcInteractionEvents {
    private NpcInteractionEvents(){}
    @SubscribeEvent public static void entity(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof LivingEntity entity && NpcRoles.supported(entity)){
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
    @SubscribeEvent public static void specific(PlayerInteractEvent.EntityInteractSpecific event){
        if(event.getTarget() instanceof LivingEntity entity && NpcRoles.supported(entity)){
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
