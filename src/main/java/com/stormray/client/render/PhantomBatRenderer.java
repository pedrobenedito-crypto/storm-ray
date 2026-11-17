package com.stormray.client.render;

import com.stormray.StormRayMod;
import com.stormray.entity.PhantomBatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renders the Phantom Bat with its glowing eyes and chest crack. */
public class PhantomBatRenderer extends GeoEntityRenderer<PhantomBatEntity> {
    public PhantomBatRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.25F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    public static class Model extends GeoModel<PhantomBatEntity> {
        private static final ResourceLocation MODEL = StormRayMod.id("geo/entity/phantom_bat.geo.json");
        private static final ResourceLocation TEXTURE = StormRayMod.id("textures/entity/phantom_bat.png");
        private static final ResourceLocation ANIMATIONS = StormRayMod.id("animations/entity/phantom_bat.animation.json");

        @Override
        public ResourceLocation getModelResource(PhantomBatEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(PhantomBatEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(PhantomBatEntity animatable) {
            return ANIMATIONS;
        }
    }
}
