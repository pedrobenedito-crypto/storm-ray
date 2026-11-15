package com.stormray.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.stormray.StormRayMod;
import com.stormray.client.ClientStormState;
import com.stormray.config.StormRayClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws the red sky right after vanilla's sky: a gradient dome around the camera plus two
 * scrolling layers of storm clouds. Purely visual; the world's weather and time are untouched.
 */
public final class StormSkyRenderer {
    private static final ResourceLocation CLOUDS = StormRayMod.id("textures/environment/storm_clouds.png");
    private static final int LAT_STEPS = 14;
    private static final int LON_STEPS = 32;
    private static final float DOME_RADIUS = 120.0F;
    private static final int CLOUD_GRID = 12;
    private static final float CLOUD_EXTENT = 320.0F;
    private static final double CLOUD_TILE = 220.0D;

    private StormSkyRenderer() {
    }

    public static void render(PoseStack poseStack, float partialTick) {
        float strength = StormRayClientConfig.SKY_TINT.get().floatValue();
        float intensity = ClientStormState.intensity(partialTick) * strength;
        if (intensity <= 0.002F) {
            return;
        }
        float charge = ClientStormState.charge(partialTick);
        float flash = ClientStormState.flash(partialTick);
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        float time = (mc.level != null ? (mc.level.getGameTime() % 240000L) : 0L) + partialTick;

        Matrix4f matrix = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        drawDome(matrix, intensity, charge, flash);
        drawCloudLayer(matrix, camera, time, 62.0F, 0.00035F, 0.00012F, intensity * 0.9F, charge, flash, 0.07F);
        drawCloudLayer(matrix, camera, time, 96.0F, -0.00018F, 0.00025F, intensity * 0.75F, charge, flash, 0.13F);

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawDome(Matrix4f matrix, float intensity, float charge, float flash) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float minLat = -0.45F;
        float maxLat = Mth.HALF_PI;
        for (int i = 0; i < LAT_STEPS; i++) {
            float lat0 = Mth.lerp(i / (float) LAT_STEPS, minLat, maxLat);
            float lat1 = Mth.lerp((i + 1) / (float) LAT_STEPS, minLat, maxLat);
            float[] c0 = domeColor(lat0, intensity, charge, flash);
            float[] c1 = domeColor(lat1, intensity, charge, flash);
            for (int j = 0; j < LON_STEPS; j++) {
                float lon0 = j / (float) LON_STEPS * Mth.TWO_PI;
                float lon1 = (j + 1) / (float) LON_STEPS * Mth.TWO_PI;
                domeVertex(buffer, matrix, lat0, lon0, c0);
                domeVertex(buffer, matrix, lat0, lon1, c0);
                domeVertex(buffer, matrix, lat1, lon1, c1);
                domeVertex(buffer, matrix, lat1, lon0, c1);
            }
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void domeVertex(BufferBuilder buffer, Matrix4f matrix, float lat, float lon, float[] color) {
        float horizontal = Mth.cos(lat) * DOME_RADIUS;
        buffer.vertex(matrix, horizontal * Mth.cos(lon), Mth.sin(lat) * DOME_RADIUS, horizontal * Mth.sin(lon))
                .color(color[0], color[1], color[2], color[3])
                .endVertex();
    }

    /** Bright crimson glow at the horizon, near-black blood red at the zenith; charging darkens and saturates it. */
    private static float[] domeColor(float lat, float intensity, float charge, float flash) {
        float t = Mth.clamp(lat / Mth.HALF_PI, 0.0F, 1.0F);
        float below = lat < 0.0F ? Mth.clamp(-lat / 0.45F, 0.0F, 1.0F) : 0.0F;
        float darken = 1.0F - 0.45F * charge;
        float r = Mth.lerp(t, 0.66F, 0.17F) * darken;
        float g = Mth.lerp(t, 0.08F, 0.0F) * (1.0F - charge);
        float b = Mth.lerp(t, 0.06F, 0.035F) * darken;
        r = Mth.lerp(below, r, 0.1F);
        g = Mth.lerp(below, g, 0.0F);
        b = Mth.lerp(below, b, 0.015F);
        r = Math.min(1.0F, r + flash * 0.55F);
        g = Math.min(1.0F, g + flash * 0.3F);
        b = Math.min(1.0F, b + flash * 0.32F);
        float a = Mth.lerp(t, 0.82F, 0.94F) * intensity;
        return new float[]{r, g, b, a};
    }

    private static void drawCloudLayer(Matrix4f matrix, Vec3 camera, float time, float height, float speedU, float speedV,
                                       float alpha, float charge, float flash, float baseBrightness) {
        if (alpha <= 0.002F) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, CLOUDS);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float swirl = 1.0F + charge * 3.0F;
        float baseU = (float) (((camera.x / CLOUD_TILE) % 1.0D + 1.0D) % 1.0D) + (time * speedU * swirl) % 1.0F;
        float baseV = (float) (((camera.z / CLOUD_TILE) % 1.0D + 1.0D) % 1.0D) + (time * speedV * swirl) % 1.0F;
        float r = Math.min(1.0F, baseBrightness * 1.6F + flash * 0.9F);
        float g = Math.min(1.0F, baseBrightness * 0.25F + flash * 0.5F);
        float b = Math.min(1.0F, baseBrightness * 0.35F + flash * 0.55F);
        float step = CLOUD_EXTENT * 2.0F / CLOUD_GRID;
        for (int i = 0; i < CLOUD_GRID; i++) {
            for (int j = 0; j < CLOUD_GRID; j++) {
                float x0 = -CLOUD_EXTENT + i * step;
                float z0 = -CLOUD_EXTENT + j * step;
                float x1 = x0 + step;
                float z1 = z0 + step;
                cloudVertex(buffer, matrix, x0, height, z0, baseU, baseV, r, g, b, alpha);
                cloudVertex(buffer, matrix, x0, height, z1, baseU, baseV, r, g, b, alpha);
                cloudVertex(buffer, matrix, x1, height, z1, baseU, baseV, r, g, b, alpha);
                cloudVertex(buffer, matrix, x1, height, z0, baseU, baseV, r, g, b, alpha);
            }
        }
        BufferUploader.drawWithShader(buffer.end());
    }

    private static void cloudVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float baseU, float baseV,
                                    float r, float g, float b, float alpha) {
        float distance = Mth.sqrt(x * x + z * z) / CLOUD_EXTENT;
        float edge = Mth.clamp(1.0F - distance * distance, 0.0F, 1.0F);
        buffer.vertex(matrix, x, y, z)
                .uv(baseU + (float) (x / CLOUD_TILE), baseV + (float) (z / CLOUD_TILE))
                .color(r, g, b, alpha * edge)
                .endVertex();
    }
}
