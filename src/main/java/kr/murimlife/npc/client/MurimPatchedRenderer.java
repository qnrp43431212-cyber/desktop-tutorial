package kr.murimlife.npc.client;
import kr.murimlife.npc.MurimHumanoid;
import kr.murimlife.npc.epic.MurimEpicPatch;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.api.client.model.Meshes;
/** Epic Fight skinned-mesh render bridge using the same character texture. */
public final class MurimPatchedRenderer extends PHumanoidRenderer<MurimHumanoid,MurimEpicPatch,HumanoidModel<MurimHumanoid>,MurimHumanoidRenderer,HumanoidMesh> {
    public MurimPatchedRenderer(EntityRendererProvider.Context ctx,EntityType<?> type){super(Meshes.BIPED,ctx,type);}
}
