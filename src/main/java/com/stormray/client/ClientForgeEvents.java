package com.stormray.client;

import com.stormray.StormRayMod;
import com.stormray.client.render.PetrifiedEntityRenderer;
import com.stormray.client.render.StormBossBar;
import com.stormray.client.render.StormSkyRenderer;
import com.stormray.client.sound.StormAmbienceSound;
import com.stormray.config.StormRayClientConfig;
import com.stormray.petrify.PetrifyClientData;
import com.stormray.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side rendering of the red-sky event: fog colour, fog distance, sky dome, ash,
 * ambience, petrified entities and the custom boss bar.
 */
@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private ClientForgeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            ClientStormState.reset();
            PetrifyClientData.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        ClientStormState.tick();
        PetrifyClientData.tick();
        StormAmbienceSound.tickManager(mc);
        spawnAmbientAsh(mc, mc.player);
    }

    private static void spawnAmbientAsh(Minecraft mc, LocalPlayer player) {
        float intensity = ClientStormState.intensity(1.0F);
        if (intensity < 0.05F || mc.level == null) {
            return;
        }
        RandomSource random = player.getRandom();
        double amount = StormRayClientConfig.AMBIENT_PARTICLES.get() * intensity * 4.0D;
        int count = (int) amount + (random.nextDouble() < amount - Math.floor(amount) ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double x = player.getX() + random.nextGaussian() * 14.0D;
            double y = player.getY() + random.nextDouble() * 16.0D - 3.0D;
            double z = player.getZ() + random.nextGaussian() * 14.0D;
            mc.level.addParticle(ModParticles.STORM_EMBER.get(), x, y, z, 0.04D + random.nextDouble() * 0.03D,
                    -0.02D - random.nextDouble() * 0.03D, 0.015D);
        }
        if (ClientStormState.inMirror()) {
            // the mirror is thick with drifting soot
            int soot = (int) (StormRayClientConfig.AMBIENT_PARTICLES.get() * 7.0D);
            for (int i = 0; i < soot; i++) {
                double x = player.getX() + random.nextGaussian() * 12.0D;
                double y = player.getY() + random.nextDouble() * 14.0D - 2.0D;
                double z = player.getZ() + random.nextGaussian() * 12.0D;
                mc.level.addParticle(ModParticles.SOOT.get(), x, y, z, 0.01D + random.nextGaussian() * 0.01D,
                        -0.01D - random.nextDouble() * 0.015D, random.nextGaussian() * 0.01D);
            }
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float partialTick = (float) event.getPartialTick();
        if (event.getCamera().getFluidInCamera() == FogType.WATER && ClientStormState.inMirror()) {
            // the mirror's ocean is a deep, almost black blue
            event.setRed(0.015F);
            event.setGreen(0.035F);
            event.setBlue(0.11F);
            return;
        }
        float intensity = ClientStormState.intensity(partialTick) * StormRayClientConfig.SKY_TINT.get().floatValue();
        if (intensity <= 0.001F || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float charge = ClientStormState.charge(partialTick);
        float flash = ClientStormState.flash(partialTick);
        float red = Mth.lerp(charge, 0.36F, 0.24F) + flash * 0.55F;
        float green = Mth.lerp(charge, 0.035F, 0.0F) + flash * 0.25F;
        float blue = Mth.lerp(charge, 0.045F, 0.015F) + flash * 0.25F;
        event.setRed(Mth.lerp(intensity, event.getRed(), Math.min(1.0F, red)));
        event.setGreen(Mth.lerp(intensity, event.getGreen(), Math.min(1.0F, green)));
        event.setBlue(Mth.lerp(intensity, event.getBlue(), Math.min(1.0F, blue)));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getType() == FogType.WATER && ClientStormState.inMirror()) {
            event.setNearPlaneDistance(-4.0F);
            event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), 22.0F));
            event.setCanceled(true);
            return;
        }
        if (!StormRayClientConfig.THICK_FOG.get() || event.getType() != FogType.NONE) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float intensity = ClientStormState.intensity(partialTick);
        if (intensity <= 0.001F) {
            return;
        }
        float charge = ClientStormState.charge(partialTick);
        float far = event.getFarPlaneDistance();
        float targetFar = Math.max(48.0F, far * Mth.lerp(charge, 0.45F, 0.3F));
        float targetNear = event.getMode() == FogRenderer.FogMode.FOG_SKY ? 0.0F : targetFar * 0.05F;
        event.setFarPlaneDistance(Mth.lerp(intensity, far, Math.min(far, targetFar)));
        event.setNearPlaneDistance(Mth.lerp(intensity, event.getNearPlaneDistance(), Math.min(event.getNearPlaneDistance(), targetNear)));
        event.setCanceled(true); // Forge applies the new distances only when the event is cancelled
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            StormSkyRenderer.render(event.getPoseStack(), event.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        PetrifiedEntityRenderer.onRenderLiving(event);
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        StormBossBar.render(event);
    }
}
