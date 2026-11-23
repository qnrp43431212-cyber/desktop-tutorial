package kr.murimlife;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModBlocks {
    private ModBlocks(){}
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MurimLife.MOD_ID);
    public static final Supplier<Block> MEDITATION_STONE = BLOCKS.register("meditation_stone", () ->
        new Block(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.STONE)));
    public static final Supplier<Block> HERBAL_BREWING_POT = BLOCKS.register("herbal_brewing_pot", () ->
        new Block(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.STONE)));
}
