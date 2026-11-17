package com.stormray.client;

import com.stormray.StormRayMod;
import com.stormray.client.dimension.AshenFoliageColors;
import com.stormray.client.dimension.AshenSkyEffects;
import com.stormray.client.dimension.DarkWaterExtensions;
import com.stormray.client.particle.StormParticle;
import com.stormray.dimension.ModDimensions;
import com.stormray.client.render.PhantomBatRenderer;
import com.stormray.client.render.PhantomSeaRenderer;
import com.stormray.client.render.PhantomXRenderer;
import com.stormray.client.render.StormOverlays;
import com.stormray.client.render.StormRayRenderer;
import com.stormray.registry.ModEntities;
import com.stormray.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(DarkWaterExtensions::install);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STORM_RAY.get(), StormRayRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_X.get(), PhantomXRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_SEA.get(), PhantomSeaRenderer::new);
        event.registerEntityRenderer(ModEntities.PHANTOM_BAT.get(), PhantomBatRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.STORM_EMBER.get(), StormParticle::ember);
        event.registerSpriteSet(ModParticles.PETRIFY_DUST.get(), StormParticle::petrifyDust);
        event.registerSpriteSet(ModParticles.RITUAL_RUNE.get(), StormParticle::ritualRune);
        event.registerSpriteSet(ModParticles.SOOT.get(), StormParticle::soot);
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ModDimensions.EFFECTS_ID, new AshenSkyEffects());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        AshenFoliageColors.register(event);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("storm_tint", StormOverlays.STORM_TINT);
        event.registerBelowAll("petrify_vignette", StormOverlays.PETRIFY_VIGNETTE);
    }
}
