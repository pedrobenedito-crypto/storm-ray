package com.stormray.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.stormray.StormRayMod;
import com.stormray.petrify.PetrifyClientData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderLivingEvent;

/**
 * Turns petrified entities to stone without touching their own renderer: the entity is drawn
 * once darkened and desaturated (vertex tint), then again with a tiled stone texture whose
 * opacity follows the petrification progress. Works for players, vanilla and modded mobs.
 */
public final class PetrifiedEntityRenderer {
    private static final ResourceLocation STONE_TEXTURE = StormRayMod.id("textures/misc/petrified_stone.png");
    private static final RenderType STONE_TYPE = RenderType.entityTranslucent(STONE_TEXTURE);
    private static boolean rendering;

    private PetrifiedEntityRenderer() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (rendering) {
            return;
        }
        LivingEntity entity = event.getEntity();
        float partialTick = event.getPartialTick();
        float progress = PetrifyClientData.progress(entity.getId(), partialTick);
        if (progress <= 0.0F) {
            return;
        }
        event.setCanceled(true);
        rendering = true;
        try {
            LivingEntityRenderer renderer = event.getRenderer();
            PoseStack poseStack = event.getPoseStack();
            MultiBufferSource buffers = event.getMultiBufferSource();
            int light = event.getPackedLight();
            float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());

            float shade = 1.0F - 0.45F * progress;
            MultiBufferSource tinted = type -> new TintingVertexConsumer(buffers.getBuffer(type), shade, shade * 0.97F, shade * 0.97F, 1.0F, 1.0F);
            renderer.render(entity, yaw, partialTick, poseStack, tinted, light);

            float stoneAlpha = Mth.clamp(progress * 1.15F, 0.0F, 1.0F);
            MultiBufferSource stone = type -> type.format() == DefaultVertexFormat.NEW_ENTITY
                    ? new TintingVertexConsumer(buffers.getBuffer(STONE_TYPE), 0.95F, 0.95F, 0.98F, stoneAlpha, 3.0F)
                    : NoopVertexConsumer.INSTANCE;
            renderer.render(entity, yaw, partialTick, poseStack, stone, light);
        } finally {
            rendering = false;
        }
    }
}
