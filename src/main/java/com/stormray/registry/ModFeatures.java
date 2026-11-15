package com.stormray.registry;

import com.stormray.StormRayMod;
import com.stormray.dimension.AshenRotFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, StormRayMod.MODID);

    /** Decays freshly generated mirror chunks. Added to every overworld biome but does nothing outside the mirror. */
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ASHEN_ROT =
            FEATURES.register("ashen_rot", () -> new AshenRotFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {
    }
}
