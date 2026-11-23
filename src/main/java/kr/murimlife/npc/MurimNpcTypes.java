package kr.murimlife.npc;
import kr.murimlife.MurimLife;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
/** One new entity type, one mod ID: all resident/spectator/fighter roles share a real humanoid model. */
public final class MurimNpcTypes {
    private MurimNpcTypes(){}
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,MurimLife.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<MurimHumanoid>> HUMAN =
       ENTITIES.register("human",()->EntityType.Builder.of(MurimHumanoid::new,MobCategory.CREATURE)
           .sized(.6f,1.8f).clientTrackingRange(10).updateInterval(2).build("murimlife:human"));
}
