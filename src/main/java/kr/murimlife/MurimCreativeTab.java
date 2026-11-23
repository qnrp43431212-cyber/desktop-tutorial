package kr.murimlife;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Creative-mode item catalog to inspect registered prototype equipment. */
public final class MurimCreativeTab {
    private MurimCreativeTab() {}
    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MurimLife.MOD_ID);
    static {
        TABS.register("murim", () -> CreativeModeTab.builder()
            .title(Component.literal("무림 도복과 무기"))
            .icon(() -> new ItemStack(Items.IRON_SWORD))
            .displayItems((parameters, output) -> {
                for (var item : ModItems.all()) output.accept(item.get());
            }).build());
    }
}
