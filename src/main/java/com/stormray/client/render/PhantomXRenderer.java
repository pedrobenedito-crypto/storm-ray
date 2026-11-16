package com.stormray.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.stormray.entity.PhantomXEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Renders the Phantom X. While cloaked it is drawn almost transparent and its shape trembles
 * (a slightly wobbling scale and offset), so the player sees a distorted silhouette instead of
 * nothing. The glowing face crack fades together with the body.
 */
public class PhantomXRenderer extends GeoEntityRenderer<PhantomXEntity> {
    public PhantomXRenderer(EntityRendererProvider.Context context) {
        super(context, new PhantomXModel());
        this.shadowRadius = 0.5F;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(PhantomXEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float visibility = entity.getCloakVisibility(partialTick);
        float distortion = 1.0F - visibility;
        this.shadowRadius = 0.5F * visibility;

        poseStack.pushPose();
        if (distortion > 0.01F) {
            float t = (entity.tickCount + partialTick) * 0.45F + entity.getId() * 7.0F;
            poseStack.translate(Mth.sin(t * 2.9F) * 0.05F * distortion, Mth.sin(t * 1.7F) * 0.04F * distortion,
                    Mth.cos(t * 2.3F) * 0.05F * distortion);
            poseStack.scale(1.0F + Mth.sin(t * 3.7F) * 0.06F * distortion, 1.0F + Mth.cos(t * 2.1F) * 0.04F * distortion,
                    1.0F + Mth.sin(t * 3.1F) * 0.06F * distortion);
        }
        MultiBufferSource buffers = visibility >= 0.999F
                ? bufferSource
                : type -> new TintingVertexConsumer(bufferSource.getBuffer(type), visibility, visibility, visibility, visibility, 1.0F);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        poseStack.popPose();
    }
}
