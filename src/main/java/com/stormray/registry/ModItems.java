package com.stormray.registry;

import com.stormray.StormRayMod;
import com.stormray.item.ColosseumLocatorItem;
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
    }

    private ModItems() {
    }
}
