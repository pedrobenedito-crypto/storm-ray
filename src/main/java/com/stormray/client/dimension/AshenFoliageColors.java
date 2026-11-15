package com.stormray.client.dimension;

import com.stormray.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/**
 * Re-registers the vanilla grass and foliage tints so that, only inside the Ashen Reflection,
 * leaves and grass turn a dead brown-grey. Everywhere else the vanilla colour is returned.
 */
public final class AshenFoliageColors {
    private AshenFoliageColors() {
    }

    public static void register(RegisterColorHandlersEvent.Block event) {
        event.register(wither((state, level, pos, tint) -> level != null && pos != null
                        ? BiomeColors.getAverageGrassColor(level, state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos)
                        : GrassColor.getDefaultColor()),
                Blocks.LARGE_FERN, Blocks.TALL_GRASS);
        event.register(wither((state, level, pos, tint) -> level != null && pos != null
                        ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor()),
                Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.GRASS, Blocks.POTTED_FERN);
        event.register(wither((state, level, pos, tint) -> FoliageColor.getEvergreenColor()), Blocks.SPRUCE_LEAVES);
        event.register(wither((state, level, pos, tint) -> FoliageColor.getBirchColor()), Blocks.BIRCH_LEAVES);
        event.register(wither((state, level, pos, tint) -> level != null && pos != null
                        ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor()),
                Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.VINE, Blocks.MANGROVE_LEAVES);
    }

    private static BlockColor wither(BlockColor vanilla) {
        return (state, level, pos, tint) -> {
            int color = vanilla.getColor(state, level, pos, tint);
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !ModDimensions.isMirror(mc.level)) {
                return color;
            }
            return witherColor(color);
        };
    }

    /** Drains the colour and pulls it toward a dark ashen brown. */
    static int witherColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        float luma = 0.3F * r + 0.59F * g + 0.11F * b;
        int nr = clamp(luma * 0.55F + 34);
        int ng = clamp(luma * 0.42F + 14);
        int nb = clamp(luma * 0.40F + 16);
        return (nr << 16) | (ng << 8) | nb;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }
}
