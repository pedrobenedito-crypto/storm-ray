package com.stormray.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * Bleeding: the wound left by a Phantom Bat's bite. Half a heart every 25 ticks, straight
 * through armor, so the usual 5 seconds cost two hearts. Milk cures it like any other effect.
 */
public class BleedingEffect extends MobEffect {
    /** Ticks between two drops of blood. */
    public static final int INTERVAL = 25;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.04F), 1.0F);

    public BleedingEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0A12);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6D, entity.getZ(), 8,
                    entity.getBbWidth() * 0.3D, entity.getBbHeight() * 0.25D, entity.getBbWidth() * 0.3D, 0.0D);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % INTERVAL == 0;
    }
}
