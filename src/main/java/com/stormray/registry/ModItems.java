package com.stormray.registry;

import com.stormray.StormRayMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StormRayMod.MODID);

    public static final RegistryObject<Item> STORM_RAY_SPAWN_EGG = ITEMS.register("storm_ray_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STORM_RAY, 0x120D18, 0xB0102A, new Item.Properties()));

    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(STORM_RAY_SPAWN_EGG);
        }
    }

    private ModItems() {
    }
}
