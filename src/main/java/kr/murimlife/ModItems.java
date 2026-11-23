package kr.murimlife;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {}
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, MurimLife.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MurimLife.MOD_ID);
    private static final List<Supplier<? extends Item>> ALL = new ArrayList<>();
    private static Holder<ArmorMaterial> armor(String id) {
        return ARMOR_MATERIALS.register("robe_"+id, () -> new ArmorMaterial(
            new EnumMap<>(Map.of(
                ArmorItem.Type.BOOTS, 2,
                ArmorItem.Type.LEGGINGS, 5,
                ArmorItem.Type.CHESTPLATE, 6,
                ArmorItem.Type.HELMET, 2,
                ArmorItem.Type.BODY, 6)),
            9, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MurimLife.MOD_ID, "robe_"+id))),
            0.0F, 0.0F));
    }
    private static void add(String id, Supplier<? extends Item> item) { ALL.add(ITEMS.register(id, item)); }
    private static void outfit(String id) {
        Holder<ArmorMaterial> material=armor(id);
        // No durability component: equipable iron-tier armour without item breakage.
        add(id+"_chestplate", () -> new ArmorItem(material, ArmorItem.Type.CHESTPLATE, new Item.Properties().stacksTo(1)));
        add(id+"_leggings", () -> new ArmorItem(material, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
        add(id+"_boots", () -> new ArmorItem(material, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));
    }
    // Initialize weapon family, grade and durability arrays BEFORE the item
    // registration static block calls gradedWeapons(). Otherwise launching
    // Minecraft crashes with a NullPointerException during mod loading.
    public static final String[] WEAPON_FAMILIES = {"jian","dao","great_jian","great_dao","dagger","spear","staff","glaive","fan","whip","chain","claw","gauntlet","needles","throwing_knife","hammer"};
    public static final String[] WEAPON_GRADES = {"common","refined","steel","folded","black_iron","master","divine"};
    private static final int[] GRADE_DURABILITY = {200,350,500,750,1100,1400,1900};
    static {
        add("meditation_stone", () -> new BlockItem(ModBlocks.MEDITATION_STONE.get(), new Item.Properties()));
        add("herbal_brewing_pot", () -> new BlockItem(ModBlocks.HERBAL_BREWING_POT.get(), new Item.Properties()));
        for(var formula:kr.murimlife.medicine.ElixirCatalog.all()){
            add(formula.id(),()->new kr.murimlife.medicine.ElixirItem(formula.id(),new Item.Properties().stacksTo(16)));
        }
        outfit("shaolin");
        outfit("wudang");
        outfit("huashan");
        outfit("emei");
        outfit("qingcheng");
        outfit("kunlun");
        outfit("kongtong");
        outfit("diancang");
        outfit("zhongnan");
        outfit("gaibang");
        outfit("namgung");
        outfit("tangmun");
        outfit("paengga");
        outfit("jegal");
        outfit("hwangsan");
        outfit("sapa_01");
        outfit("sapa_02");
        outfit("sapa_03");
        outfit("sapa_04");
        outfit("sapa_05");
        outfit("sapa_06");
        outfit("sapa_07");
        outfit("sapa_08");
        outfit("sapa_09");
        outfit("sapa_10");
        outfit("wander_01");
        outfit("wander_02");
        outfit("wander_03");
        outfit("wander_04");
        outfit("wander_05");
        outfit("wander_06");
        outfit("wander_07");
        outfit("wander_08");
        outfit("founder_01");
        outfit("founder_02");
        outfit("founder_03");
        outfit("demon_cult");
        outfit("blood_cult");

        // 5 clans * 4 genuinely different costume roles (two family outfits, two staff).
        outfit("namgung_heir1");
        outfit("namgung_heir2");
        outfit("namgung_staff1");
        outfit("namgung_staff2");
        outfit("tangmun_heir1");
        outfit("tangmun_heir2");
        outfit("tangmun_staff1");
        outfit("tangmun_staff2");
        outfit("paengga_heir1");
        outfit("paengga_heir2");
        outfit("paengga_staff1");
        outfit("paengga_staff2");
        outfit("jegal_heir1");
        outfit("jegal_heir2");
        outfit("jegal_staff1");
        outfit("jegal_staff2");
        outfit("mourong_heir1");
        outfit("mourong_heir2");
        outfit("mourong_staff1");
        outfit("mourong_staff2");
        outfit("orth_white_crane");
        outfit("orth_azure_stream");
        outfit("orth_jade_flower");
        outfit("orth_pine_shadow");
        outfit("orth_bright_star");
        outfit("orth_golden_bell");
        outfit("orth_river_moon");
        outfit("orth_sword_bamboo");
        outfit("orth_silver_cloud");
        outfit("orth_eight_rivers");

        // All three disguise items share iron-tier armour stats and use durability.
        Holder<ArmorMaterial> iron = net.minecraft.world.item.ArmorMaterials.IRON;
        add("mask", () -> new ArmorItem(iron, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
        add("full_hood", () -> new ArmorItem(iron, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
        add("veil", () -> new ArmorItem(iron, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(15))));
        add("heavenly_succession_token", () -> new Item(new Item.Properties().stacksTo(1)));
        add("blood_succession_seal", () -> new Item(new Item.Properties().stacksTo(1)));
        // Prototype weapon items currently use a vanilla iron-sword attack behaviour.
        // Weapon-specific moves, reach and real skill validation are still unimplemented.
        add("weapon_jian", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_dao", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_great_jian", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_great_dao", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_dagger", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_spear", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_staff", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_glaive", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_fan", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_whip", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_chain", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_claw", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_gauntlet", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_needles", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_throwing_knife", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        add("weapon_hammer", () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));
        gradedWeapons();
    }
    private static void gradedWeapons() {
        for (int g=0;g<WEAPON_GRADES.length;g++) {
            final int grade=g;
            for (String weapon:WEAPON_FAMILIES) {
                String id="weapon_"+weapon+"_"+WEAPON_GRADES[grade];
                add(id,()->new SwordItem(Tiers.IRON,new Item.Properties()
                    .durability(GRADE_DURABILITY[grade])
                    .attributes(SwordItem.createAttributes(Tiers.IRON,3+grade,-2.4F))));
            }
        }
    }
    public static List<Supplier<? extends Item>> all() { return Collections.unmodifiableList(ALL); }
}
