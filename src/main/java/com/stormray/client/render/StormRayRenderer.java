package com.stormray.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormray.entity.StormRayEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderer. The model is authored at a manageable pixel size and scaled up here;
 * the glow layer uses storm_ray_glowmask.png for the eyes, maw, veins and filament tips.
 */
public class StormRayRenderer extends GeoEntityRenderer<StormRayEntity> {
    public static final float MODEL_SCALE = 3.0F;

    public StormRayRenderer(EntityRendererProvider.Context context) {
        super(context, new StormRayModel());
        this.shadowRadius = 0.0F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(StormRayEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
