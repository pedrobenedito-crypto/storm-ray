package com.stormray.structure;

import com.stormray.dimension.ModDimensions;
import com.stormray.registry.ModStructures;
import com.stormray.util.RotNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds the Ruined Colosseum from {@link ColosseumShape}, one chunk slice at a time.
 * The bounding box stores everything needed: its centre is the arena centre and its
 * bottom is the arena floor. In the overworld the ruin is overgrown with green vines and
 * moss; in the Ashen Reflection red weeping vines hang from every arch instead.
 */
public class ColosseumPiece extends StructurePiece {
    private static final int FLAGS = 2;
    private static final int MAX_FOUNDATION = 48;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public ColosseumPiece(int centreX, int floorY, int centreZ) {
        super(ModStructures.COLOSSEUM_PIECE.get(), 0, new BoundingBox(
                centreX - ColosseumShape.RADIUS, floorY, centreZ - ColosseumShape.RADIUS,
                centreX + ColosseumShape.RADIUS, floorY + ColosseumShape.HEIGHT, centreZ + ColosseumShape.RADIUS));
    }

    public ColosseumPiece(CompoundTag tag) {
        super(ModStructures.COLOSSEUM_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // the bounding box is all the state there is
    }

    /** Arena centre of a colosseum with this bounding box (y = arena floor). */
    public static BlockPos centreOf(BoundingBox box) {
        return new BlockPos((box.minX() + box.maxX()) / 2, box.minY(), (box.minZ() + box.maxZ()) / 2);
    }

    public static int seedOf(BlockPos centre) {
        return (int) RotNoise.hash(centre.getX(), 0, centre.getZ(), 977);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BoundingBox bounds = this.getBoundingBox();
        BlockPos centre = centreOf(bounds);
        int floor = centre.getY();
        int seed = seedOf(centre);
        boolean mirror = ModDimensions.isMirror(level.getLevel());

        int x0 = Math.max(box.minX(), bounds.minX());
        int x1 = Math.min(box.maxX(), bounds.maxX());
        int z0 = Math.max(box.minZ(), bounds.minZ());
        int z1 = Math.min(box.maxZ(), bounds.maxZ());
        if (x0 > x1 || z0 > z1) {
            return;
        }
        // Columns of this slice plus a one-block border, so vines can ask their neighbours.
        int width = x1 - x0 + 3;
        int depth = z1 - z0 + 3;
        ColosseumShape.Column[][] columns = new ColosseumShape.Column[width][depth];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < depth; j++) {
                columns[i][j] = ColosseumShape.column(x0 - 1 + i - centre.getX(), z0 - 1 + j - centre.getZ(), seed);
            }
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int i = x - x0 + 1;
                int j = z - z0 + 1;
                ColosseumShape.Column column = columns[i][j];
                if (!column.inFootprint()) {
                    continue;
                }
                prepareGround(level, x, z, floor, seed, pos);
                buildColumn(level, columns, i, j, x, z, floor, seed, mirror, pos);
            }
        }
    }

    /** Clears terrain above the floor and lays a foundation down to solid ground. */
    private static void prepareGround(WorldGenLevel level, int x, int z, int floor, int seed, BlockPos.MutableBlockPos pos) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int clearTop = Math.min(surface, floor + ColosseumShape.HEIGHT + 24);
        for (int y = floor; y < clearTop; y++) {
            pos.set(x, y, z);
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, AIR, FLAGS);
            }
        }
        int bottom = level.getMinBuildHeight();
        for (int y = floor - 2, n = 0; n < MAX_FOUNDATION && y > bottom; y--, n++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced()) {
                break;
            }
            level.setBlock(pos, wall(RotNoise.unit(x, y, z, seed + 20)), FLAGS);
        }
    }

    private static void buildColumn(WorldGenLevel level, ColosseumShape.Column[][] columns, int i, int j, int x, int z,
                                    int floor, int seed, boolean mirror, BlockPos.MutableBlockPos pos) {
        ColosseumShape.Column column = columns[i][j];
        int hanging = 0;
        BlockState hangingState = AIR;
        for (int y = ColosseumShape.HEIGHT - 1; y >= -1; y--) {
            byte kind = column.at(y);
            int worldY = floor + y;
            pos.set(x, worldY, z);
            if (kind != ColosseumShape.AIR) {
                level.setBlock(pos, blockFor(kind, RotNoise.unit(x, worldY, z, seed + 20)), FLAGS);
                hanging = 0;
                continue;
            }
            if (y < 0) {
                continue;
            }
            boolean floorBelow = column.solid(y - 1);
            if (hanging > 0) {
                // continue a strand started higher up
                if (mirror) {
                    boolean tip = hanging == 1 || floorBelow;
                    level.setBlock(pos, (tip ? Blocks.WEEPING_VINES : Blocks.WEEPING_VINES_PLANT).defaultBlockState(), FLAGS);
                    hanging = tip ? 0 : hanging - 1;
                } else {
                    level.setBlock(pos, hangingState, FLAGS);
                    hanging = floorBelow ? 0 : hanging - 1;
                }
                continue;
            }
            float chance = RotNoise.unit(x, worldY, z, seed + 21);
            int length = (int) (RotNoise.unit(x, worldY, z, seed + 22) * 6.0F);
            if (mirror) {
                // red weeping vines under arches, cornices and vaults
                byte above = column.at(y + 1);
                if (above != ColosseumShape.AIR && above != ColosseumShape.RUBBLE && chance < 0.3F) {
                    boolean tip = length == 0 || floorBelow;
                    level.setBlock(pos, (tip ? Blocks.WEEPING_VINES : Blocks.WEEPING_VINES_PLANT).defaultBlockState(), FLAGS);
                    hanging = tip ? 0 : length;
                }
                continue;
            }
            if (chance < 0.24F) {
                BlockState vine = vineAgainst(columns, i, j, y);
                if (vine != null) {
                    level.setBlock(pos, vine, FLAGS);
                    hangingState = vine;
                    hanging = floorBelow ? 0 : length;
                    continue;
                }
            }
            if (floorBelow && y > 0 && chance > 0.86F) {
                level.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
            } else if (floorBelow && y == 0 && chance > 0.9F && column.at(-1) == ColosseumShape.FOUNDATION) {
                level.setBlock(pos, Blocks.MOSS_CARPET.defaultBlockState(), FLAGS);
            }
        }
    }

    /** A vine clinging to every solid horizontal neighbour, or null if there is none. */
    private static BlockState vineAgainst(ColosseumShape.Column[][] columns, int i, int j, int y) {
        BlockState vine = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (columns[i + direction.getStepX()][j + direction.getStepZ()].solid(y)) {
                if (vine == null) {
                    vine = Blocks.VINE.defaultBlockState();
                }
                vine = vine.setValue(VineBlock.getPropertyForFace(direction), Boolean.TRUE);
            }
        }
        return vine;
    }

    private static BlockState blockFor(byte kind, float h) {
        switch (kind) {
            case ColosseumShape.SEAT:
                if (h < 0.40F) return Blocks.STONE_BRICKS.defaultBlockState();
                if (h < 0.55F) return Blocks.SMOOTH_STONE.defaultBlockState();
                if (h < 0.72F) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                if (h < 0.86F) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                if (h < 0.94F) return Blocks.ANDESITE.defaultBlockState();
                return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case ColosseumShape.CORNICE:
                if (h < 0.5F) return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
                if (h < 0.8F) return Blocks.STONE_BRICKS.defaultBlockState();
                return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            case ColosseumShape.PILLAR:
                if (h < 0.5F) return Blocks.POLISHED_ANDESITE.defaultBlockState();
                if (h < 0.8F) return Blocks.STONE_BRICKS.defaultBlockState();
                return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case ColosseumShape.RUBBLE:
                if (h < 0.38F) return Blocks.COBBLESTONE.defaultBlockState();
                if (h < 0.64F) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
                if (h < 0.82F) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
                if (h < 0.92F) return Blocks.ANDESITE.defaultBlockState();
                return Blocks.STONE_BRICKS.defaultBlockState();
            case ColosseumShape.FLOOR:
                if (h < 0.58F) return Blocks.SAND.defaultBlockState();
                if (h < 0.78F) return Blocks.COARSE_DIRT.defaultBlockState();
                if (h < 0.90F) return Blocks.GRAVEL.defaultBlockState();
                return Blocks.SANDSTONE.defaultBlockState();
            case ColosseumShape.SIGIL_DARK:
                return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            case ColosseumShape.SIGIL_LINE:
                return Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            case ColosseumShape.SIGIL_CORE:
                return Blocks.REDSTONE_BLOCK.defaultBlockState();
            default:
                return wall(h);
        }
    }

    private static BlockState wall(float h) {
        if (h < 0.42F) return Blocks.STONE_BRICKS.defaultBlockState();
        if (h < 0.62F) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (h < 0.84F) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        if (h < 0.92F) return Blocks.COBBLESTONE.defaultBlockState();
        if (h < 0.97F) return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        return Blocks.ANDESITE.defaultBlockState();
    }
}
