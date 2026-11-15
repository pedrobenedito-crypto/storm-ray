package com.stormray;

import com.mojang.logging.LogUtils;
import com.stormray.config.StormRayClientConfig;
import com.stormray.config.StormRayConfig;
import com.stormray.network.ModNetwork;
import com.stormray.registry.ModBlocks;
import com.stormray.registry.ModEntities;
import com.stormray.registry.ModFeatures;
import com.stormray.registry.ModItems;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Storm Ray: a colossal sky creature summoned with a redstone ritual.
 * Event handlers in this mod self-register through {@link Mod.EventBusSubscriber}.
 */
@Mod(StormRayMod.MODID)
public final class StormRayMod {
    public static final String MODID = "stormray";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StormRayMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModParticles.PARTICLE_TYPES.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModItems::addToCreativeTabs);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StormRayConfig.SPEC, MODID + "-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, StormRayClientConfig.SPEC, MODID + "-client.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("Storm Ray ready: build the redstone T and crown it with a Steve head.");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }
}
