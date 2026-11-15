package com.stormray.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.stormray.StormRayMod;
import com.stormray.client.ClientStormState;
import com.stormray.config.StormRayClientConfig;
import com.stormray.petrify.PetrifyClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Screen-space layers: red storm tint, lightning flash, and the stone vignette when you are petrified. */
public final class StormOverlays {
    private static final ResourceLocation CRACKS = StormRayMod.id("textures/misc/petrify_overlay.png");

    public static final IGuiOverlay STORM_TINT = (gui, graphics, partialTick, width, height) ->
            renderStormTint(graphics, partialTick, width, height);
    public static final IGuiOverlay PETRIFY_VIGNETTE = (gui, graphics, partialTick, width, height) ->
            renderPetrify(graphics, partialTick, width, height);

    private StormOverlays() {
    }

    private static int argb(float alpha, int r, int g, int b) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255.0F)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void renderStormTint(GuiGraphics graphics, float partialTick, int width, int height) {
        float strength = StormRayClientConfig.SCREEN_TINT.get().floatValue();
        float intensity = ClientStormState.intensity(partialTick) * strength;
        float flash = ClientStormState.flash(partialTick);
        if (intensity <= 0.002F && flash <= 0.002F) {
            return;
        }
        float charge = ClientStormState.charge(partialTick);
        graphics.fill(0, 0, width, height, argb(intensity * (0.16F + 0.12F * charge), 110, 0, 10));
        int edge = height / 3;
        graphics.fillGradient(0, 0, width, edge, argb(intensity * 0.45F, 25, 0, 4), argb(0.0F, 25, 0, 4));
        graphics.fillGradient(0, height - edge, width, height, argb(0.0F, 25, 0, 4), argb(intensity * 0.5F, 25, 0, 4));
        if (flash > 0.002F) {
            graphics.fill(0, 0, width, height, argb(flash * 0.45F * strength, 255, 215, 215));
        }
    }

    private static void renderPetrify(GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float progress = PetrifyClientData.progress(mc.player.getId(), partialTick);
        if (progress <= 0.0F) {
            return;
        }
        graphics.fill(0, 0, width, height, argb(progress * 0.55F, 88, 88, 92));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1.0F, 1.0F, 1.0F, Math.min(1.0F, progress * 1.2F));
        graphics.blit(CRACKS, 0, 0, 0.0F, 0.0F, width, height, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
