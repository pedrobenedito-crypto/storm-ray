package com.stormray.structure;

import com.mojang.serialization.Codec;
import com.stormray.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * The Ruined Colosseum. Its structure set uses concentric-ring placement with a count of one,
 * so, like the End portal's stronghold ring, exactly one exists per world. The Ashen Reflection
 * shares the seed, so the same ruin stands at the same coordinates there.
 */
public class ColosseumStructure extends Structure {
    public static final Codec<ColosseumStructure> CODEC = simpleCodec(ColosseumStructure::new);

    public ColosseumStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();

        // The median of nine samples across the footprint ignores a stray cliff or ravine.
        int[] heights = new int[9];
        int i = 0;
        for (int ox = -30; ox <= 30; ox += 30) {
            for (int oz = -30; oz <= 30; oz += 30) {
                heights[i++] = generator.getFirstOccupiedHeight(x + ox, z + oz, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
            }
        }
        Arrays.sort(heights);
        int floor = Math.max(heights[4], generator.getSeaLevel()) + 1;
        int highest = context.heightAccessor().getMaxBuildHeight() - ColosseumShape.HEIGHT - 2;
        if (floor > highest) {
            floor = highest;
        }
        final int floorY = floor;
        return Optional.of(new Structure.GenerationStub(new BlockPos(x, floorY, z),
                builder -> builder.addPiece(new ColosseumPiece(x, floorY, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.COLOSSEUM.get();
    }
}
