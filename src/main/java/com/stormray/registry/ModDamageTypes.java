package com.stormray.registry;

import com.stormray.StormRayMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Damage types are data driven in 1.20.1; the JSON lives in data/stormray/damage_type.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> PETRIFICATION =
            ResourceKey.create(Registries.DAMAGE_TYPE, StormRayMod.id("petrification"));

    public static DamageSource petrification(Level level, @Nullable Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PETRIFICATION), attacker);
    }

    private ModDamageTypes() {
    }
}
