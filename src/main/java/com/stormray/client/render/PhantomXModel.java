package com.stormray.client.render;

import com.stormray.StormRayMod;
import com.stormray.entity.PhantomXEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class PhantomXModel extends GeoModel<PhantomXEntity> {
    private static final ResourceLocation MODEL = StormRayMod.id("geo/entity/phantom_x.geo.json");
    private static final ResourceLocation TEXTURE = StormRayMod.id("textures/entity/phantom_x.png");
    private static final ResourceLocation ANIMATIONS = StormRayMod.id("animations/entity/phantom_x.animation.json");

    @Override
    public ResourceLocation getModelResource(PhantomXEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(PhantomXEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(PhantomXEntity animatable) {
        return ANIMATIONS;
    }

    /** Translucent so the cloak can fade the body out. */
    @Override
    public RenderType getRenderType(PhantomXEntity animatable, ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }
}
