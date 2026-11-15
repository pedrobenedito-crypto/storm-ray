package com.stormray.dimension;

import com.stormray.util.RotNoise;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How the overworld decays in the Ashen Reflection. Works on one block column at a time,
 * reading only the original states, so the result is identical for chunk generation and for
 * the later copy of overworld builds.
 *
 * <ul>
 *   <li>grass, paths and farmland become coarse dirt / podzol / rooted dirt, with patches of crimson nylium</li>
 *   <li>grass, flowers and crops wither into dead bushes or crimson roots, or vanish</li>
 *   <li>almost half of all leaves fall away; red weeping vines hang under the rest</li>
 *   <li>sugar cane, vines and lily pads rot away</li>
 * </ul>
 */
public final class RotRules {
    private static final int SALT_GROUND = 11;
    private static final int SALT_PLANT = 23;
    private static final int SALT_LEAF = 37;
    private static final int SALT_VINE = 41;
    private static final int SALT_PATCH = 53;

    private RotRules() {
    }

    /**
     * @param originals column states from {@code minY} upward (index 0 = minY)
     * @return the decayed column (same array length); unchanged entries are the same instances
     */
    public static BlockState[] rotColumn(BlockState[] originals, int x, int z, int minY) {
        int n = originals.length;
        BlockState[] out = new BlockState[n];
        int vineRemaining = 0;
        for (int i = n - 1; i >= 0; i--) {
            BlockState original = originals[i];
            int y = minY + i;
            BlockState result = original;

            if (original.isAir()) {
                if (vineRemaining > 0) {
                    boolean tip = vineRemaining == 1 || i == 0 || !originals[i - 1].isAir();
                    result = tip ? Blocks.WEEPING_VINES.defaultBlockState() : Blocks.WEEPING_VINES_PLANT.defaultBlockState();
                    vineRemaining = tip ? 0 : vineRemaining - 1;
                }
            } else {
                vineRemaining = 0;
                if (isGround(original)) {
                    result = rotGround(x, y, z);
                } else if (original.is(BlockTags.LEAVES)) {
                    if (RotNoise.unit(x, y, z, SALT_LEAF) < 0.42F) {
                        result = Blocks.AIR.defaultBlockState();
                    } else {
                        float anchor = RotNoise.unit(x, y, z, SALT_VINE);
                        if (anchor < 0.07F) {
                            vineRemaining = 1 + (int) (anchor * 1000.0F) % 4;
                        }
                    }
                } else if (isTallPlant(original) || isRotAway(original)) {
                    result = Blocks.AIR.defaultBlockState();
                } else if (isSmallPlant(original)) {
                    result = witheredPlant(originals, i, x, y, z);
                }
            }
            out[i] = result;
        }
        return out;
    }

    private static boolean isGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) || state.is(Blocks.MYCELIUM);
    }

    private static BlockState rotGround(int x, int y, int z) {
        if (RotNoise.smooth(x, z, 14, SALT_PATCH) > 0.8F) {
            return Blocks.CRIMSON_NYLIUM.defaultBlockState();
        }
        float h = RotNoise.unit(x, y, z, SALT_GROUND);
        if (h < 0.6F) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        return h < 0.85F ? Blocks.PODZOL.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState();
    }

    private static boolean isSmallPlant(BlockState state) {
        return state.is(Blocks.GRASS) || state.is(Blocks.FERN) || state.is(BlockTags.SMALL_FLOWERS)
                || state.is(BlockTags.CROPS) || state.is(Blocks.SWEET_BERRY_BUSH);
    }

    private static boolean isTallPlant(BlockState state) {
        return state.is(Blocks.TALL_GRASS) || state.is(Blocks.LARGE_FERN) || state.is(BlockTags.TALL_FLOWERS);
    }

    private static boolean isRotAway(BlockState state) {
        return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.VINE) || state.is(Blocks.LILY_PAD);
    }

    /** A dead bush or crimson roots if the block below decays into soil that supports it, otherwise air. */
    private static BlockState witheredPlant(BlockState[] originals, int i, int x, int y, int z) {
        if (i == 0 || !isGround(originals[i - 1])) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState below = rotGround(x, y - 1, z);
        float h = RotNoise.unit(x, y, z, SALT_PLANT);
        if (below.is(Blocks.CRIMSON_NYLIUM)) {
            return h < 0.5F ? Blocks.CRIMSON_ROOTS.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (h < 0.2F) {
            return Blocks.DEAD_BUSH.defaultBlockState();
        }
        return h < 0.26F ? Blocks.CRIMSON_ROOTS.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }
}
