package com.stormray.dimension;

import com.stormray.StormRayMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * The Ashen Reflection: a decayed 1:1 mirror of the overworld. It is defined by data
 * (data/stormray/dimension and dimension_type); this class only holds the keys.
 * It shares the world seed and the overworld's noise settings, so terrain, biomes and
 * structures generate identically.
 */
public final class ModDimensions {
    public static final ResourceKey<Level> ASHEN_LEVEL =
            ResourceKey.create(Registries.DIMENSION, StormRayMod.id("ashen_reflection"));
    /** Key of the client-side sky/fog/light effects registered for the dimension type. */
    public static final ResourceLocation EFFECTS_ID = StormRayMod.id("ashen_reflection");

    private ModDimensions() {
    }

    public static boolean isMirror(LevelAccessor level) {
        return level instanceof Level l && ASHEN_LEVEL.equals(l.dimension());
    }
}
