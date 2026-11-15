package com.stormray.client.render;

import com.stormray.StormRayMod;
import com.stormray.entity.StormRayEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StormRayModel extends GeoModel<StormRayEntity> {
    private static final ResourceLocation MODEL = StormRayMod.id("geo/entity/storm_ray.geo.json");
    private static final ResourceLocation TEXTURE = StormRayMod.id("textures/entity/storm_ray.png");
    private static final ResourceLocation ANIMATIONS = StormRayMod.id("animations/entity/storm_ray.animation.json");

    @Override
    public ResourceLocation getModelResource(StormRayEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(StormRayEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(StormRayEntity animatable) {
        return ANIMATIONS;
    }
}
