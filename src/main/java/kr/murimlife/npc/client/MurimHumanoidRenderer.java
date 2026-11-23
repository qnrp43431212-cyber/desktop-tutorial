package kr.murimlife.npc.client;
import kr.murimlife.npc.MurimHumanoid;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
/** Uses standard 64x64 humanoid skin atlas. Real Epic Fight patched render is separate. */
public final class MurimHumanoidRenderer extends HumanoidMobRenderer<MurimHumanoid,HumanoidModel<MurimHumanoid>> {
    private static final ResourceLocation[] SKINS=new ResourceLocation[966];
    static {for(int i=0;i<SKINS.length;i++)SKINS[i]=ResourceLocation.fromNamespaceAndPath("murimlife",
        "textures/entity/wuxia/skin_"+String.format(java.util.Locale.ROOT,"%03d",i)+".png");}
    public MurimHumanoidRenderer(EntityRendererProvider.Context ctx){super(ctx,new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)),.5f);}
    @Override public ResourceLocation getTextureLocation(MurimHumanoid mob){return SKINS[Math.clamp(mob.getSkin(),0,SKINS.length-1)];}
}
