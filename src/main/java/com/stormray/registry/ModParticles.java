package com.stormray.registry;

import com.stormray.StormRayMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, StormRayMod.MODID);

    /** Glowing red ember: storm atmosphere, charging energy, pulse shockwave. */
    public static final RegistryObject<SimpleParticleType> STORM_EMBER =
            PARTICLE_TYPES.register("storm_ember", () -> new SimpleParticleType(true));
    /** Grey grit that surrounds petrified entities. */
    public static final RegistryObject<SimpleParticleType> PETRIFY_DUST =
            PARTICLE_TYPES.register("petrify_dust", () -> new SimpleParticleType(true));
    /** Rising crimson sparks of the summoning ritual. */
    public static final RegistryObject<SimpleParticleType> RITUAL_RUNE =
            PARTICLE_TYPES.register("ritual_rune", () -> new SimpleParticleType(true));

    /** Dark flakes of soot drifting through the Ashen Reflection. */
    public static final RegistryObject<SimpleParticleType> SOOT =
            PARTICLE_TYPES.register("soot", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
