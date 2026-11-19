package com.stormray.registry;

import com.stormray.StormRayMod;
import com.stormray.item.ColosseumLocatorItem;
import com.stormray.item.PhantomArmorItem;
import com.stormray.item.PhantomSwordItem;
import com.stormray.item.PhantomTier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StormRayMod.MODID);

    public static final RegistryObject<Item> STORM_RAY_SPAWN_EGG = ITEMS.register("storm_ray_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STORM_RAY, 0x120D18, 0xB0102A, new Item.Properties()));

    public static final RegistryObject<Item> PHANTOM_X_SPAWN_EGG = ITEMS.register("phantom_x_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PHANTOM_X, 0x16151A, 0x9A1420, new Item.Properties()));

    public static final RegistryObject<Item> PHANTOM_SEA_SPAWN_EGG = ITEMS.register("phantom_sea_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PHANTOM_SEA, 0x0E1626, 0x9A1420, new Item.Properties()));

    public static final RegistryObject<Item> PHANTOM_BAT_SPAWN_EGG = ITEMS.register("phantom_bat_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PHANTOM_BAT, 0x1E1622, 0xD0301E, new Item.Properties()));

    /** Points the way to the Ruined Colosseum; found in Sky Island chests. */
    public static final RegistryObject<Item> COLOSSEUM_LOCATOR = ITEMS.register("colosseum_locator",
            () -> new ColosseumLocatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    /** Black scale cut from a slain Storm Ray; the base of every phantom tool. */
    public static final RegistryObject<Item> PHANTOM_SCALE = ITEMS.register("phantom_scale",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));

    /** Two scales bound together: what the sword is forged from. */
    public static final RegistryObject<Item> REINFORCED_PHANTOM_SCALE = ITEMS.register("reinforced_phantom_scale",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    public static final RegistryObject<Item> PHANTOM_HELMET = ITEMS.register("phantom_helmet",
            () -> new PhantomArmorItem(ArmorItem.Type.HELMET, armour()));
    public static final RegistryObject<Item> PHANTOM_CHESTPLATE = ITEMS.register("phantom_chestplate",
            () -> new PhantomArmorItem(ArmorItem.Type.CHESTPLATE, armour()));
    public static final RegistryObject<Item> PHANTOM_LEGGINGS = ITEMS.register("phantom_leggings",
            () -> new PhantomArmorItem(ArmorItem.Type.LEGGINGS, armour()));
    public static final RegistryObject<Item> PHANTOM_BOOTS = ITEMS.register("phantom_boots",
            () -> new PhantomArmorItem(ArmorItem.Type.BOOTS, armour()));

    /** Burns what it cuts, and dashes on right-click. */
    public static final RegistryObject<Item> PHANTOM_SWORD = ITEMS.register("phantom_sword",
            () -> new PhantomSwordItem(PhantomTier.PHANTOM, 3, -2.4F,
                    new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    private static Item.Properties armour() {
        return new Item.Properties().rarity(Rarity.RARE).fireResistant();
    }

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(STORM_RAY_SPAWN_EGG);
            event.accept(PHANTOM_X_SPAWN_EGG);
            event.accept(PHANTOM_SEA_SPAWN_EGG);
            event.accept(PHANTOM_BAT_SPAWN_EGG);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(COLOSSEUM_LOCATOR);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(PHANTOM_SCALE);
            event.accept(REINFORCED_PHANTOM_SCALE);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(PHANTOM_SWORD);
            event.accept(PHANTOM_HELMET);
            event.accept(PHANTOM_CHESTPLATE);
            event.accept(PHANTOM_LEGGINGS);
            event.accept(PHANTOM_BOOTS);
        }
    }

    private ModItems() {
    }
}
