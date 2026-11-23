package kr.murimlife.npc.client;
import kr.murimlife.npc.MurimNpcTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
/** Client-only registration. Never load this class on a dedicated server. */
public final class MurimNpcClient {
    private MurimNpcClient(){}
    public static void register(IEventBus bus){
        bus.addListener(MurimNpcClient::onRenderers);
        EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(
            e->e.addPatchedEntityRenderer(MurimNpcTypes.HUMAN.get(),
                t->new MurimPatchedRenderer(e.getContext(),t)),"murimlife");
    }
    private static void onRenderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerEntityRenderer(MurimNpcTypes.HUMAN.get(),MurimHumanoidRenderer::new);
    }
}
