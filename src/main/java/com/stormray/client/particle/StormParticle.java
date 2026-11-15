package com.stormray.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Shared particle class for the mod's three particle types. */
public class StormParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final boolean emissive;
    private final float startSize;

    protected StormParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                            SpriteSet sprites, int lifetime, float size, float gravity, float friction, boolean emissive) {
        super(level, x, y, z, dx, dy, dz);
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.sprites = sprites;
        this.lifetime = lifetime + this.random.nextInt(Math.max(1, lifetime / 3));
        this.quadSize = size * (0.75F + this.random.nextFloat() * 0.5F);
        this.startSize = this.quadSize;
        this.gravity = gravity;
        this.friction = friction;
        this.hasPhysics = false;
        this.emissive = emissive;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            float life = this.age / (float) this.lifetime;
            this.alpha = 1.0F - life * life;
            this.quadSize = this.startSize * (1.0F - life * 0.4F);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.emissive ? 0xF000F0 : super.getLightColor(partialTick);
    }

    public static ParticleProvider<SimpleParticleType> ember(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) ->
                new StormParticle(level, x, y, z, dx, dy, dz, sprites, 50, 0.22F, 0.0F, 0.97F, true);
    }

    public static ParticleProvider<SimpleParticleType> petrifyDust(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) ->
                new StormParticle(level, x, y, z, dx, dy, dz, sprites, 35, 0.16F, 0.04F, 0.92F, false);
    }

    public static ParticleProvider<SimpleParticleType> ritualRune(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) ->
                new StormParticle(level, x, y, z, dx, dy, dz, sprites, 40, 0.3F, -0.01F, 0.96F, true);
    }
}
