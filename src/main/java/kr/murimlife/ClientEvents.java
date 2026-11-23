package kr.murimlife;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Radial is a HUD overlay (not a Screen), therefore WASD keeps working while R is held. */
public final class ClientEvents {
    private ClientEvents() {}
    public static final KeyMapping MENU = new KeyMapping("key.murimlife.menu",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.murimlife");
    public static final KeyMapping WHEEL = new KeyMapping("key.murimlife.wheel",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.murimlife");
    public static final KeyMapping TALK = new KeyMapping("key.murimlife.talk",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, "key.categories.murimlife");
    public static final KeyMapping PARRY = new KeyMapping("key.murimlife.parry",
        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.murimlife");
    private static boolean wasOpen;
    private static boolean theftActive;
    private static long theftClientEnd;
    public static void setTheftActive(boolean active){theftActive=active;var mc=Minecraft.getInstance();
        theftClientEnd=active&&mc.level!=null?mc.level.getGameTime()+100:0;}
    private static int selected;

    /** Client raycast up to six blocks, blocked by solid terrain, nearest NPC wins. */
    private static int aimedNpcId(Minecraft mc){
        if(mc.player==null||mc.level==null)return -1;
        var eye=mc.player.getEyePosition();
        var direction=mc.player.getViewVector(1f);
        var end=eye.add(direction.scale(6.0));
        end=mc.level.clip(new net.minecraft.world.level.ClipContext(eye,end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player)).getLocation();
        var bounds=mc.player.getBoundingBox().expandTowards(direction.scale(6.0)).inflate(1.0);
        double closest=eye.distanceToSqr(end);
        int entityId=-1;
        for(var npc:mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,bounds,
                n->n.isAlive()&&!n.isSpectator()&&kr.murimlife.npc.dialogue.NpcRoles.supported(n))){
            var hit=npc.getBoundingBox().inflate(.2).clip(eye,end);
            if(hit.isEmpty())continue;
            double distance=eye.distanceToSqr(hit.get());
            if(distance<=closest){closest=distance;entityId=npc.getId();}
        }
        return entityId;
    }
    @EventBusSubscriber(modid=MurimLife.MOD_ID, value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
            event.register(MENU); event.register(WHEEL); event.register(PARRY); event.register(TALK);
        }
    }
    @EventBusSubscriber(modid=MurimLife.MOD_ID, value=Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if(mc.player==null)theftActive=false;
            while (MENU.consumeClick()) if (mc.screen == null && mc.player != null) mc.setScreen(new MurimScreen());
            while (TALK.consumeClick()) if (mc.screen == null && mc.player != null && !theftActive) {
                PacketDistributor.sendToServer(new MurimNetwork.TalkRequest(aimedNpcId(mc)));
            }
            while (PARRY.consumeClick()) if (mc.screen == null && mc.player != null) PacketDistributor.sendToServer(new MurimNetwork.Parry());
            boolean held = mc.player != null && mc.screen == null && WHEEL.isDown();
            if (wasOpen && !held && mc.player != null)
                PacketDistributor.sendToServer(new MurimNetwork.SelectSlot(selected));
            wasOpen = held;
        }
        @SubscribeEvent public static void theftClick(InputEvent.MouseButton.Pre event){
            if(!theftActive||event.getButton()!=GLFW.GLFW_MOUSE_BUTTON_RIGHT||event.getAction()!=GLFW.GLFW_PRESS)return;
            if(Minecraft.getInstance().screen!=null)return;
            PacketDistributor.sendToServer(new MurimNetwork.TheftClick());
            event.setCanceled(true);
        }
        @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent event) {
            if (!wasOpen) return;
            if (event.getScrollDeltaY() != 0) {
                selected = Math.floorMod(selected + (event.getScrollDeltaY() > 0 ? -1 : 1), 12);
                event.setCanceled(true); // prevent hotbar changes, while allowing movement
            }
        }
        @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
            if (theftActive){
                Minecraft mc=Minecraft.getInstance();
                int remaining=mc.level==null?0:(int)Math.max(0,(theftClientEnd-mc.level.getGameTime()+19)/20);
                event.getGuiGraphics().drawCenteredString(mc.font,"[소매치기] "+remaining+"초 · 마우스 우클릭 연타",
                    mc.getWindow().getGuiScaledWidth()/2,mc.getWindow().getGuiScaledHeight()/2+35,0xFFFFD080);
            }
            if (!wasOpen) return;
            GuiGraphics g = event.getGuiGraphics();
            Minecraft mc = Minecraft.getInstance();
            int x = mc.getWindow().getGuiScaledWidth()/2;
            int y = mc.getWindow().getGuiScaledHeight()/2;
            // Rectangular seals around a circle; replace with 12 wedge textures during art pass.
            for (int i=0;i<12;i++) {
                double a = -Math.PI/2.0 + 2.0*Math.PI*i/12.0;
                int sx=x+(int)Math.round(Math.cos(a)*70), sy=y+(int)Math.round(Math.sin(a)*70);
                int bg=(i==selected)?0xD6B38A50:0xB51C282D;
                g.fill(sx-17,sy-11,sx+17,sy+11,bg);
                g.drawCenteredString(mc.font, String.valueOf(i+1),sx,sy-4,0xFFE9D7B5);
            }
            g.drawCenteredString(mc.font,"武 功",x,y-7,0xFFDBC394);
            String[] slots=ClientProfileCache.slots.split("\\|",-1);
            String known=selected<slots.length?slots[selected]:"";
            g.drawCenteredString(mc.font,known.isEmpty()?"무공 미배치":known,x,y+6,0xFFDBC394);
        }
    }
}
