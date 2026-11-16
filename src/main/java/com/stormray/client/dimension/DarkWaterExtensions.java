package com.stormray.client.dimension;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.stormray.StormRayMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.lang.reflect.Field;

/**
 * Wraps Forge's client extensions for vanilla water: everything is delegated unchanged,
 * except that inside the Ashen Reflection the water tint becomes a deep, dark blue.
 * Forge has no event to recolour vanilla water in 1.20.1, so the wrapper is installed once
 * at client setup; if that ever fails, water simply keeps its normal colour.
 */
public final class DarkWaterExtensions implements IClientFluidTypeExtensions {
    private final IClientFluidTypeExtensions original;

    private DarkWaterExtensions(IClientFluidTypeExtensions original) {
        this.original = original;
    }

    public static void install() {
        try {
            FluidType water = ForgeMod.WATER_TYPE.get();
            Field field = FluidType.class.getDeclaredField("renderProperties");
            field.setAccessible(true);
            Object current = field.get(water);
            if (current instanceof IClientFluidTypeExtensions extensions && !(current instanceof DarkWaterExtensions)) {
                field.set(water, new DarkWaterExtensions(extensions));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            StormRayMod.LOGGER.warn("Could not install dark water for the Ashen Reflection; water keeps its normal colour", e);
        }
    }

    @Override
    public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        int color = this.original.getTintColor(state, getter, pos);
        return MirrorColors.inMirror() ? MirrorColors.deepWater(color) : color;
    }

    // ------------------------------------------------------------------ plain delegation

    @Override
    public int getTintColor() {
        return this.original.getTintColor();
    }

    @Override
    public ResourceLocation getStillTexture() {
        return this.original.getStillTexture();
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return this.original.getFlowingTexture();
    }

    @Override
    public ResourceLocation getOverlayTexture() {
        return this.original.getOverlayTexture();
    }

    @Override
    public ResourceLocation getRenderOverlayTexture(Minecraft mc) {
        return this.original.getRenderOverlayTexture(mc);
    }

    @Override
    public void renderOverlay(Minecraft mc, PoseStack poseStack) {
        this.original.renderOverlay(mc, poseStack);
    }

    @Override
    public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                                   Vector3f fluidFogColor) {
        return this.original.modifyFogColor(camera, partialTick, level, renderDistance, darkenWorldAmount, fluidFogColor);
    }

    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance,
                                float farDistance, FogShape shape) {
        this.original.modifyFogRender(camera, mode, renderDistance, partialTick, nearDistance, farDistance, shape);
    }

    @Override
    public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.original.getStillTexture(state, getter, pos);
    }

    @Override
    public ResourceLocation getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.original.getFlowingTexture(state, getter, pos);
    }

    @Override
    public ResourceLocation getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return this.original.getOverlayTexture(state, getter, pos);
    }

    @Override
    public int getTintColor(FluidStack stack) {
        return this.original.getTintColor(stack);
    }

    @Override
    public ResourceLocation getStillTexture(FluidStack stack) {
        return this.original.getStillTexture(stack);
    }

    @Override
    public ResourceLocation getFlowingTexture(FluidStack stack) {
        return this.original.getFlowingTexture(stack);
    }

    @Override
    public ResourceLocation getOverlayTexture(FluidStack stack) {
        return this.original.getOverlayTexture(stack);
    }
}
