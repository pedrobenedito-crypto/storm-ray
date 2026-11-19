package com.stormray.structure;

import com.mojang.serialization.Codec;
import com.stormray.config.StormRayConfig;
import com.stormray.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Sky Island: a piece of ground ripped out of the earth and left hanging high above the
 * crater it came from. Generates in the Ashen Reflection and (unless disabled in the config) at
 * the same places in the normal world; its chest always holds a Colosseum Locator.
 */
public class SkyIslandStructure extends Structure {
    public static final Codec<SkyIslandStructure> CODEC = simpleCodec(SkyIslandStructure::new);

    public SkyIslandStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        boolean mirror = ModStructures.isMirrorGenerator(generator);
        if (!mirror) {
            if (!StormRayConfig.ISLANDS_IN_OVERWORLD.get() || !ModStructures.isOverworldGenerator(generator)) {
                return Optional.empty();
            }
            // The structure set is shared with the Ashen Reflection, where the islands belong.
            // In the normal world only a fraction of those spots is kept, so they stay a find.
            if (context.random().nextFloat() >= StormRayConfig.ISLANDS_OVERWORLD_CHANCE.get()) {
                return Optional.empty();
            }
        }
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        int ground = Math.max(surface, generator.getSeaLevel());
        int top = ground + 45 + context.random().nextInt(25);
        int highest = context.heightAccessor().getMaxBuildHeight() - SkyIslandShape.TOP - 4;
        if (top > highest) {
            top = highest;
        }
        if (top - SkyIslandShape.DEPTH < ground + 16) {
            return Optional.empty(); // no room between the peaks and the build limit
        }
        final int topY = top;
        return Optional.of(new Structure.GenerationStub(new BlockPos(x, topY, z),
                builder -> builder.addPiece(new SkyIslandPiece(x, topY, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.SKY_ISLAND.get();
    }
}
