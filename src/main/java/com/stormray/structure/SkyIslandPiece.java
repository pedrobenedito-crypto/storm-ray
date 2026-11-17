package com.stormray.structure;

import com.stormray.dimension.ModDimensions;
import com.stormray.registry.ModStructures;
import com.stormray.util.RotNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds a Sky Island from {@link SkyIslandShape} and digs the crater it was torn from in
 * the ground below. The top of the bounding box minus {@link SkyIslandShape#TOP} is the
 * island's soil surface.
 */
public class SkyIslandPiece extends StructurePiece {
    private static final int FLAGS = 2;
    private static final int HALF = SkyIslandShape.RADIUS + 1;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public SkyIslandPiece(int centreX, int topY, int centreZ) {
        super(ModStructures.SKY_ISLAND_PIECE.get(), 0, new BoundingBox(
                centreX - HALF, topY - SkyIslandShape.DEPTH, centreZ - HALF,
                centreX + HALF, topY + SkyIslandShape.TOP, centreZ + HALF));
    }

    public SkyIslandPiece(CompoundTag tag) {
        super(ModStructures.SKY_ISLAND_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // the bounding box is all the state there is
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        if (!ModDimensions.isMirror(level.getLevel())) {
            return;
        }
        BoundingBox bounds = this.getBoundingBox();
        int centreX = (bounds.minX() + bounds.maxX()) / 2;
        int centreZ = (bounds.minZ() + bounds.maxZ()) / 2;
        int topY = bounds.maxY() - SkyIslandShape.TOP;
        int seed = (int) RotNoise.hash(centreX, 0, centreZ, 613);
        int seaLevel = generator.getSeaLevel();

        int x0 = Math.max(box.minX(), bounds.minX());
        int x1 = Math.min(box.maxX(), bounds.maxX());
        int z0 = Math.max(box.minZ(), bounds.minZ());
        int z1 = Math.min(box.maxZ(), bounds.maxZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int dx = x - centreX;
                int dz = z - centreZ;
                digCrater(level, x, z, dx, dz, seed, bounds.minY(), seaLevel, pos);
                if (SkyIslandShape.depth(dx, dz, seed) > 0) {
                    buildColumn(level, box, random, x, z, dx, dz, topY, seed, pos);
                }
            }
        }
    }

    /** The scar left in the ground: a shallow bowl a little wider than the island. */
    private static void digCrater(WorldGenLevel level, int x, int z, int dx, int dz, int seed, int islandBottom,
                                  int seaLevel, BlockPos.MutableBlockPos pos) {
        double rim = SkyIslandShape.rimRadius(dx, dz, seed) * 1.15D;
        double rr = Math.sqrt((double) dx * dx + (double) dz * dz) / rim;
        if (rr >= 1.0D) {
            return;
        }
        int cut = (int) Math.round(6.0D * (1.0D - rr * rr));
        if (cut <= 0) {
            return;
        }
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        if (surface >= islandBottom - 4 || surface - cut <= seaLevel) {
            return; // never touch the island itself, and never open a pit below sea level
        }
        if (!level.getFluidState(pos.set(x, surface, z)).isEmpty()) {
            return;
        }
        for (int n = 0; n < cut; n++) {
            level.setBlock(pos.set(x, surface - n, z), AIR, FLAGS);
        }
        pos.set(x, surface - cut, z);
        BlockState bed = level.getBlockState(pos);
        if (!bed.isAir() && bed.getFluidState().isEmpty()) {
            float h = RotNoise.unit(x, 0, z, seed + 30);
            level.setBlock(pos, (h < 0.6F ? Blocks.COARSE_DIRT : h < 0.85F ? Blocks.GRAVEL : Blocks.COBBLESTONE).defaultBlockState(), FLAGS);
        }
    }

    private void buildColumn(WorldGenLevel level, BoundingBox box, RandomSource random, int x, int z, int dx, int dz,
                             int topY, int seed, BlockPos.MutableBlockPos pos) {
        int hanging = 0;
        byte previous = SkyIslandShape.AIR;
        for (int y = SkyIslandShape.TOP; y >= -SkyIslandShape.DEPTH - 7; y--) {
            byte kind = SkyIslandShape.at(dx, y, dz, seed);
            int worldY = topY + y;
            pos.set(x, worldY, z);
            float h = RotNoise.unit(x, worldY, z, seed + 31);
            if (kind == SkyIslandShape.CHEST) {
                this.createChest(level, box, random, pos.immutable(), ModStructures.SKY_ISLAND_LOOT, null);
            } else if (kind != SkyIslandShape.AIR) {
                level.setBlock(pos, blockFor(kind, h), FLAGS);
            } else if (y == 1 && previous == SkyIslandShape.AIR && SkyIslandShape.at(dx, 0, dz, seed) == SkyIslandShape.TOPSOIL
                    && h < 0.07F) {
                level.setBlock(pos, Blocks.DEAD_BUSH.defaultBlockState(), FLAGS);
            } else if (y < 0) {
                // the underside: dangling roots and strands of red vine
                if (hanging > 0) {
                    boolean tip = hanging == 1;
                    level.setBlock(pos, (tip ? Blocks.WEEPING_VINES : Blocks.WEEPING_VINES_PLANT).defaultBlockState(), FLAGS);
                    hanging--;
                } else if (previous != SkyIslandShape.AIR && previous != SkyIslandShape.GLOW) {
                    if (previous == SkyIslandShape.DIRT && h < 0.5F) {
                        level.setBlock(pos, Blocks.HANGING_ROOTS.defaultBlockState(), FLAGS);
                    } else if (h < 0.3F) {
                        int length = (int) (RotNoise.unit(x, worldY, z, seed + 32) * 6.0F);
                        boolean tip = length == 0;
                        level.setBlock(pos, (tip ? Blocks.WEEPING_VINES : Blocks.WEEPING_VINES_PLANT).defaultBlockState(), FLAGS);
                        hanging = length;
                    }
                }
            }
            previous = kind;
        }
    }

    private static BlockState blockFor(byte kind, float h) {
        switch (kind) {
            case SkyIslandShape.TOPSOIL:
                if (h < 0.5F) return Blocks.COARSE_DIRT.defaultBlockState();
                if (h < 0.75F) return Blocks.PODZOL.defaultBlockState();
                if (h < 0.9F) return Blocks.ROOTED_DIRT.defaultBlockState();
                return Blocks.CRIMSON_NYLIUM.defaultBlockState();
            case SkyIslandShape.DIRT:
                if (h < 0.7F) return Blocks.DIRT.defaultBlockState();
                if (h < 0.85F) return Blocks.COARSE_DIRT.defaultBlockState();
                return Blocks.ROOTED_DIRT.defaultBlockState();
            case SkyIslandShape.ORE:
                if (h < 0.40F) return Blocks.COAL_ORE.defaultBlockState();
                if (h < 0.70F) return Blocks.IRON_ORE.defaultBlockState();
                if (h < 0.88F) return Blocks.COPPER_ORE.defaultBlockState();
                if (h < 0.96F) return Blocks.REDSTONE_ORE.defaultBlockState();
                return Blocks.GOLD_ORE.defaultBlockState();
            case SkyIslandShape.BRICK:
                if (h < 0.4F) return Blocks.STONE_BRICKS.defaultBlockState();
                if (h < 0.7F) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case SkyIslandShape.PILLAR:
                if (h < 0.5F) return Blocks.STONE_BRICKS.defaultBlockState();
                if (h < 0.75F) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
            case SkyIslandShape.LOG:
                return Blocks.DARK_OAK_LOG.defaultBlockState();
            case SkyIslandShape.GLOW:
                return Blocks.SHROOMLIGHT.defaultBlockState();
            default:
                if (h < 0.70F) return Blocks.STONE.defaultBlockState();
                if (h < 0.85F) return Blocks.COBBLESTONE.defaultBlockState();
                if (h < 0.93F) return Blocks.ANDESITE.defaultBlockState();
                return Blocks.TUFF.defaultBlockState();
        }
    }
}
