package com.stormray.entity;

import com.stormray.registry.ModParticles;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client-side particles driven by the synced boss phase. Uses only common API
 * (Level#addAlwaysVisibleParticle), so the class is safe to load on a dedicated server.
 * "Always visible" bypasses the 32-block particle cull, which matters for a creature this size.
 */
public final class StormRayEffects {
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.85F, 0.04F, 0.08F), 3.0F);
    private static final DustParticleOptions DARK_DUST = new DustParticleOptions(new Vector3f(0.18F, 0.02F, 0.06F), 4.0F);

    private StormRayEffects() {
    }

    public static void clientTick(StormRayEntity ray) {
        Level level = ray.level();
        RandomSource random = ray.getRandom();
        Vec3 center = ray.getBodyCenter();

        if (ray.isDeadOrDying()) {
            deathParticles(ray, level, random, center);
            return;
        }

        switch (ray.getPhase()) {
            case SUMMONING -> summonParticles(ray, level, random, center);
            case CHARGING -> chargeParticles(ray, level, random, center);
            case PULSE -> pulseParticles(ray, level, random, center);
            default -> ambientParticles(ray, level, random, center);
        }
    }

    private static void ambientParticles(StormRayEntity ray, Level level, RandomSource random, Vec3 center) {
        float yaw = ray.getYRot() * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        for (int i = 0; i < 3; i++) {
            // a point on the wing surface in local space, rotated to world space
            double lx = (random.nextDouble() - 0.5D) * 44.0D;
            double lz = (random.nextDouble() - 0.5D) * 12.0D;
            double x = center.x + lx * cos - lz * sin;
            double z = center.z + lx * sin + lz * cos;
            level.addAlwaysVisibleParticle(ModParticles.STORM_EMBER.get(), true, x, center.y + random.nextGaussian() * 0.6D, z,
                    random.nextGaussian() * 0.02D, -0.03D - random.nextDouble() * 0.05D, random.nextGaussian() * 0.02D);
        }
        if (random.nextInt(3) == 0) {
            // glowing drip from the hanging filaments
            level.addAlwaysVisibleParticle(ModParticles.STORM_EMBER.get(), true,
                    ray.getX() + random.nextGaussian() * 3.0D, ray.getY() + random.nextDouble() * 2.0D, ray.getZ() + random.nextGaussian() * 3.0D,
                    0.0D, -0.12D, 0.0D);
        }
    }

    private static void summonParticles(StormRayEntity ray, Level level, RandomSource random, Vec3 center) {
        float progress = ray.summonProgress(0.0F);
        double ground = center.y - StormRayEntity.BODY_OFFSET;
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = 2.0D + random.nextDouble() * (16.0D * (1.0D - progress) + 4.0D);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = ground + random.nextDouble() * 14.0D;
            double swirl = 0.35D;
            level.addAlwaysVisibleParticle(ModParticles.RITUAL_RUNE.get(), true, x, y, z,
                    -Math.sin(angle) * swirl + (center.x - x) * 0.02D, 0.12D + random.nextDouble() * 0.2D,
                    Math.cos(angle) * swirl + (center.z - z) * 0.02D);
        }
        for (int i = 0; i < 4; i++) {
            level.addAlwaysVisibleParticle(RED_DUST, true, center.x + random.nextGaussian() * 1.5D,
                    ground + random.nextDouble() * 30.0D, center.z + random.nextGaussian() * 1.5D, 0.0D, 0.0D, 0.0D);
        }
        if (random.nextInt(2) == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, center.x + random.nextGaussian() * 4.0D, ground + 0.5D,
                    center.z + random.nextGaussian() * 4.0D, 0.0D, 0.08D, 0.0D);
        }
    }

    private static void chargeParticles(StormRayEntity ray, Level level, RandomSource random, Vec3 center) {
        float charge = ray.chargeProgress(0.0F);
        int count = 6 + (int) (charge * 22.0F);
        for (int i = 0; i < count; i++) {
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6D, random.nextGaussian()).normalize();
            double distance = 10.0D + random.nextDouble() * 10.0D;
            Vec3 start = center.add(dir.scale(distance));
            Vec3 velocity = dir.scale(-distance * 0.055D);
            level.addAlwaysVisibleParticle(ModParticles.STORM_EMBER.get(), true, start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
        }
        if (charge > 0.5F && random.nextInt(2) == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.ELECTRIC_SPARK, true, center.x + random.nextGaussian() * 3.0D,
                    center.y + random.nextGaussian() * 1.5D, center.z + random.nextGaussian() * 3.0D,
                    random.nextGaussian() * 0.4D, random.nextGaussian() * 0.4D, random.nextGaussian() * 0.4D);
        }
        level.addAlwaysVisibleParticle(DARK_DUST, true, center.x + random.nextGaussian() * 2.0D, center.y + random.nextGaussian(),
                center.z + random.nextGaussian() * 2.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void pulseParticles(StormRayEntity ray, Level level, RandomSource random, Vec3 center) {
        int ticks = ray.getPhaseTicks();
        if (ticks > 3) {
            return;
        }
        // expanding shockwave ring at body height and a second one at ground level
        double ground = center.y - StormRayEntity.BODY_OFFSET;
        for (int i = 0; i < 90; i++) {
            double angle = (i / 90.0D) * Math.PI * 2.0D + random.nextDouble() * 0.05D;
            double speed = 1.4D + random.nextDouble() * 0.4D;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            level.addAlwaysVisibleParticle(ModParticles.STORM_EMBER.get(), true, center.x, center.y, center.z, vx, 0.0D, vz);
            level.addAlwaysVisibleParticle(ModParticles.PETRIFY_DUST.get(), true, center.x + Math.cos(angle) * 2.0D, ground + 0.5D,
                    center.z + Math.sin(angle) * 2.0D, vx * 0.8D, 0.05D, vz * 0.8D);
        }
        level.addAlwaysVisibleParticle(ParticleTypes.EXPLOSION_EMITTER, true, center.x, center.y, center.z, 0.0D, 0.0D, 0.0D);
    }

    private static void deathParticles(StormRayEntity ray, Level level, RandomSource random, Vec3 center) {
        for (int i = 0; i < 6; i++) {
            double x = center.x + random.nextGaussian() * 10.0D;
            double z = center.z + random.nextGaussian() * 10.0D;
            level.addAlwaysVisibleParticle(ModParticles.PETRIFY_DUST.get(), true, x, center.y + random.nextGaussian(), z,
                    0.0D, -0.1D, 0.0D);
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, x, center.y, z, 0.0D, 0.02D, 0.0D);
        }
        if (ray.getDyingTicks() % 10 == 0) {
            level.addAlwaysVisibleParticle(ParticleTypes.EXPLOSION, true, center.x + random.nextGaussian() * 6.0D,
                    center.y + random.nextGaussian() * 2.0D, center.z + random.nextGaussian() * 6.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
