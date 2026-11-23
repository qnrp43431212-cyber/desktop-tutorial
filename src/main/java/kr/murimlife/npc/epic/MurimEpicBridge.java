package kr.murimlife.npc.epic;
import kr.murimlife.npc.MurimNpcTypes;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.gameasset.Armatures;
/** Bound only once, from the single murimlife mod constructor. Epic Fight is a required external mod. */
public final class MurimEpicBridge {
    private MurimEpicBridge(){}
    public static void register(){
        EpicFightEventHooks.Registry.ENTITY_PATCH.registerEvent(
            event->event.registerEntityPatch(MurimNpcTypes.HUMAN.get(),MurimEpicPatch::new),"murimlife");
        EpicFightEventHooks.Animation.BEGIN.registerEvent(event->{
            if(event.getEntityPatch().getOriginal() instanceof net.minecraft.world.entity.LivingEntity actor
                    && event.getAnimation().get() instanceof yesman.epicfight.api.animation.types.AttackAnimation){
                kr.murimlife.npc.combat.MurimClashEvents.recordAttack(actor);
            }
        },"murimlife");
    }
    public static void registerArmature(){
        Armatures.registerEntityTypeArmature(MurimNpcTypes.HUMAN.get(),Armatures.BIPED);
    }
}
