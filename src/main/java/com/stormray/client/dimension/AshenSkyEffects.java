package com.stormray.client.dimension;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Sky, fog and light of the Ashen Reflection. The vanilla sky and clouds are switched off;
 * the red dome and storm clouds are drawn by StormSkyRenderer (the same as the boss event,
 * at full strength), and the lightmap is pushed toward a dim blood-red.
 */
public class AshenSkyEffects extends DimensionSpecialEffects {
    public AshenSkyEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return new Vec3(0.30D, 0.025D, 0.035D);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        return true; // background is cleared to the red fog colour; the dome follows in AFTER_SKY
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY,
                                double camZ, Matrix4f projectionMatrix) {
        return true; // no white vanilla clouds
    }

    @Override
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        float r = Math.min(1.0F, Math.max(colors.x(), 0.11F) * 1.05F + 0.03F);
        float g = Math.min(1.0F, colors.y() * 0.62F + 0.01F);
        float b = Math.min(1.0F, colors.z() * 0.6F + 0.015F);
        colors.set(r, g, b);
    }
}
