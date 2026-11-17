package com.stormray.effect;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/**
 * Bleeding: the wound left by a Phantom Bat's bite. Half a heart every 25 ticks, straight
 * through armor, so the usual 5 seconds cost two hearts. Red drops fall from the victim the
 * whole time, so it is obvious who is bleeding. Milk cures it like any other effect.
 */
public class BleedingEffect extends MobEffect {
    /** Ticks between two points of damage. */
    public static final int DAMAGE_INTERVAL = 25;
    /** Ticks between two puffs of blood particles. */
    private static final int PARTICLE_INTERVAL = 4;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.85F, 0.0F, 0.02F), 1.3F);
    private static final DustParticleOptions DARK_BLOOD = new DustParticleOptions(new Vector3f(0.5F, 0.0F, 0.02F), 0.9F);

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0A12);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        MobEffectInstance instance = entity.getEffect(this);
        int duration = instance != null ? instance.getDuration() : 0;
        if (duration % DAMAGE_INTERVAL == 0) {
            entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
            spray(entity, 14);
        } else {
            spray(entity, 5);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % PARTICLE_INTERVAL == 0 || duration % DAMAGE_INTERVAL == 0;
    }

    /** Red drops around the body, spread wide enough that the victim sees them in first person too. */
    public static void spray(LivingEntity entity, int count) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.6D;
        double z = entity.getZ();
        double spread = entity.getBbWidth() * 0.55D;
        level.sendParticles(BLOOD, x, y, z, count, spread, entity.getBbHeight() * 0.3D, spread, 0.0D);
        level.sendParticles(DARK_BLOOD, x, y, z, Math.max(1, count / 2), spread, entity.getBbHeight() * 0.3D, spread, 0.0D);
        // heavier drops that fall to the ground
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                x, y, z, Math.max(1, count / 3), spread, entity.getBbHeight() * 0.2D, spread, 0.02D);
    }
}
