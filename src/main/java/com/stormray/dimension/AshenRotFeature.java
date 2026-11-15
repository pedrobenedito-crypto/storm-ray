package com.stormray.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Last decoration step of every overworld biome. Outside the Ashen Reflection it returns
 * immediately; inside, it decays the whole chunk with {@link RotRules} while it generates,
 * so players never see healthy terrain there.
 */
public class AshenRotFeature extends Feature<NoneFeatureConfiguration> {
    public AshenRotFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!ModDimensions.isMirror(level.getLevel())) {
            return false;
        }
        BlockPos origin = context.origin();
        ChunkAccess chunk = level.getChunk(origin);
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int columnHeight = columnHeight(chunk);
        if (columnHeight <= 0) {
            return false;
        }
        BlockState[] column = new BlockState[columnHeight];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int i = 0; i < columnHeight; i++) {
                    column[i] = chunk.getSection(i >> 4).getBlockState(lx, i & 15, lz);
                }
                BlockState[] rotted = RotRules.rotColumn(column, minX + lx, minZ + lz, minY);
                for (int i = 0; i < columnHeight; i++) {
                    if (rotted[i] != column[i]) {
                        level.setBlock(pos.set(minX + lx, minY + i, minZ + lz), rotted[i], 2);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    /** Height (in blocks from the bottom) up to the top of the highest non-empty section. */
    static int columnHeight(ChunkAccess chunk) {
        LevelChunkSection[] sections = chunk.getSections();
        for (int s = sections.length - 1; s >= 0; s--) {
            if (!sections[s].hasOnlyAir()) {
                return (s + 1) * 16;
            }
        }
        return 0;
    }
}
