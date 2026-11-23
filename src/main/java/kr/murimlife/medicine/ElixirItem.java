package kr.murimlife.medicine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative medicine use; consuming one physical item is required. */
public final class ElixirItem extends Item {
    private final String formulaId;
    public ElixirItem(String id,Properties properties){super(properties);this.formulaId=id;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack held=player.getItemInHand(hand);
        if(!(player instanceof ServerPlayer serverPlayer))return InteractionResultHolder.success(held);
        if(MedicineService.take(serverPlayer,formulaId)){
            if(!player.getAbilities().instabuild)held.shrink(1);
            return InteractionResultHolder.success(held);
        }
        return InteractionResultHolder.fail(held);
    }
}
