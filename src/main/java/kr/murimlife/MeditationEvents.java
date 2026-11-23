package kr.murimlife;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
/** Physical meditation stone interaction. Full separate inner-world dimension not yet implemented. */
@EventBusSubscriber(modid=MurimLife.MOD_ID)
public final class MeditationEvents {
    private MeditationEvents(){}
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        if(event.getLevel().getBlockState(event.getPos()).is(ModBlocks.HERBAL_BREWING_POT.get())){
            if(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND)
                kr.murimlife.medicine.AlchemyService.show(p);
            return;
        }
        if(!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.MEDITATION_STONE.get()))return;
        if(event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        var d=MurimProgressData.get(p.server);var profile=d.player(p.getUUID(),p.getName().getString());
        long day=p.server.overworld().getDayTime()/24000L;
        if(d.practice(profile,"기감",p.server.overworld().getGameTime(),day)) {
            p.sendSystemMessage(Component.literal("[무림] 가부좌를 틀고 기의 흐름을 살핍니다. 기감 상승. (내면세계는 미구현)"));
        } else p.sendSystemMessage(Component.literal("[무림] 심호흡하며 수련의 성과를 되새깁니다."));
    }
}
