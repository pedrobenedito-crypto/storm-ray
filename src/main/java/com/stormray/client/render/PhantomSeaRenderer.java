package com.stormray.client.render;

import com.stormray.StormRayMod;
import com.stormray.entity.PhantomSeaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renders the Phantom Sea with its glowing face crack and gill slits. */
public class PhantomSeaRenderer extends GeoEntityRenderer<PhantomSeaEntity> {
    public PhantomSeaRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.4F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    public static class Model extends GeoModel<PhantomSeaEntity> {
        private static final ResourceLocation MODEL = StormRayMod.id("geo/entity/phantom_sea.geo.json");
        private static final ResourceLocation TEXTURE = StormRayMod.id("textures/entity/phantom_sea.png");
        private static final ResourceLocation ANIMATIONS = StormRayMod.id("animations/entity/phantom_sea.animation.json");

        @Override
        public ResourceLocation getModelResource(PhantomSeaEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(PhantomSeaEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(PhantomSeaEntity animatable) {
            return ANIMATIONS;
        }
    }
}
