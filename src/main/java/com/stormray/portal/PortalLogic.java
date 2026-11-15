package com.stormray.portal;

import com.stormray.dimension.ModDimensions;
import com.stormray.dimension.MirrorSync;
import com.stormray.registry.ModBlocks;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import com.stormray.storm.StormEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Finding, lighting, validating and building Ashen portals. */
public final class PortalLogic {
    /** Portal cells are placed without neighbour shape updates so half-built portals don't collapse. */
    private static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private PortalLogic() {
    }

    /** True for a redstone block that is part of an open portal's edge. */
    public static boolean isBoundFrame(BlockGetter level, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.REDSTONE_BLOCK)) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isPortal(level.getBlockState(pos.relative(direction)))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPortal(BlockState state) {
        return state.is(ModBlocks.ASHEN_PORTAL.get());
    }

    // ------------------------------------------------------------------ lighting a new portal

    /**
     * Looks for a redstone ring whose open 4x4 interior contains {@code inside}.
     * @return the interior's minimum corner, or null
     */
    @Nullable
    public static BlockPos findIgnitableFrame(BlockGetter level, BlockPos inside) {
        for (int x0 = inside.getX() - (PortalShape.SIZE - 1); x0 <= inside.getX(); x0++) {
            for (int z0 = inside.getZ() - (PortalShape.SIZE - 1); z0 <= inside.getZ(); z0++) {
                BlockPos min = new BlockPos(x0, inside.getY(), z0);
                if (hasEdges(level, min) && interiorIsOpen(level, min)) {
                    return min;
                }
            }
        }
        return null;
    }

    private static boolean hasEdges(BlockGetter level, BlockPos min) {
        for (int[] o : PortalShape.edges()) {
            if (!level.getBlockState(min.offset(o[0], 0, o[1])).is(Blocks.REDSTONE_BLOCK)) {
                return false;
            }
        }
        return true;
    }

    private static boolean interiorIsOpen(BlockGetter level, BlockPos min) {
        for (int[] o : PortalShape.interior()) {
            BlockState state = level.getBlockState(min.offset(o[0], 0, o[1]));
            if (!(state.isAir() || state.canBeReplaced()) || isPortal(state)) {
                return false;
            }
        }
        return true;
    }

    /** "A bolt strikes and the dimension opens." */
    public static void ignite(ServerLevel level, BlockPos min) {
        Vec3 center = new Vec3(min.getX() + 2.0D, min.getY(), min.getZ() + 2.0D);
        StormEnvironment.spawnVisualLightning(level, center);
        fillInterior(level, min);
        level.playSound(null, center.x, center.y, center.z, ModSounds.PORTAL_OPEN.get(), SoundSource.BLOCKS, 6.0F, 1.0F);
        level.sendParticles(ModParticles.RITUAL_RUNE.get(), center.x, center.y + 0.6D, center.z, 120, 1.6D, 0.3D, 1.6D, 0.25D);
        level.sendParticles(ModParticles.SOOT.get(), center.x, center.y + 1.0D, center.z, 80, 2.0D, 1.0D, 2.0D, 0.02D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5D, center.z, 40, 1.8D, 0.2D, 1.8D, 0.03D);
    }

    private static void fillInterior(Level level, BlockPos min) {
        BlockState portal = ModBlocks.ASHEN_PORTAL.get().defaultBlockState();
        for (int[] o : PortalShape.interior()) {
            level.setBlock(min.offset(o[0], 0, o[1]), portal, QUIET);
        }
    }

    // ------------------------------------------------------------------ validation (frame broken -> portal collapses)

    /** A portal cell stays only while, in every horizontal direction, a run of portal cells ends on redstone. */
    public static boolean isCellSupported(BlockGetter level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos cursor = pos;
            boolean closed = false;
            for (int step = 0; step < PortalShape.SIZE; step++) {
                cursor = cursor.relative(direction);
                BlockState state = level.getBlockState(cursor);
                if (state.is(Blocks.REDSTONE_BLOCK)) {
                    closed = true;
                    break;
                }
                if (!isPortal(state)) {
                    break;
                }
            }
            if (!closed) {
                return false;
            }
        }
        return true;
    }

    /** Minimum corner of the portal containing {@code pos}. */
    public static BlockPos findPortalMin(BlockGetter level, BlockPos pos) {
        BlockPos cursor = pos;
        for (int i = 0; i < PortalShape.SIZE && isPortal(level.getBlockState(cursor.west())); i++) {
            cursor = cursor.west();
        }
        for (int i = 0; i < PortalShape.SIZE && isPortal(level.getBlockState(cursor.north())); i++) {
            cursor = cursor.north();
        }
        return cursor;
    }

    // ------------------------------------------------------------------ the portal on the other side

    public static boolean isCompletePortal(BlockGetter level, BlockPos min) {
        for (int[] o : PortalShape.interior()) {
            if (!isPortal(level.getBlockState(min.offset(o[0], 0, o[1])))) {
                return false;
            }
        }
        return hasEdges(level, min);
    }

    /**
     * Builds a lit portal (frame, floor, headroom) at the same coordinates in the destination world.
     * Corners are left out on purpose: every frame block touches the portal, so it is "bound" and
     * drops nothing when broken (see PortalIgnition#onBreak) - no free redstone from travelling.
     */
    public static void buildPortal(ServerLevel level, BlockPos min) {
        loadAndSync(level, min);
        BlockState redstone = Blocks.REDSTONE_BLOCK.defaultBlockState();
        BlockState floor = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        // headroom above the interior and the ring
        for (int dx = -1; dx <= PortalShape.SIZE; dx++) {
            for (int dz = -1; dz <= PortalShape.SIZE; dz++) {
                for (int dy = 1; dy <= 2; dy++) {
                    level.setBlock(min.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        for (int[] o : PortalShape.interior()) {
            BlockPos below = min.offset(o[0], -1, o[1]);
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                level.setBlock(below, floor, Block.UPDATE_ALL);
            }
            level.setBlock(min.offset(o[0], 0, o[1]), Blocks.AIR.defaultBlockState(), QUIET);
        }
        for (int[] o : PortalShape.edges()) {
            level.setBlock(min.offset(o[0], 0, o[1]), redstone, Block.UPDATE_ALL);
        }
        fillInterior(level, min);
        StormEnvironment.spawnVisualLightning(level, new Vec3(min.getX() + 2.0D, min.getY(), min.getZ() + 2.0D));
    }

    /** Makes the arrival spot safe (floor below, two free blocks of air) and returns where to place the traveller. */
    public static Vec3 prepareLanding(ServerLevel level, BlockPos min) {
        loadAndSync(level, min);
        for (int[] o : PortalShape.landing()) {
            BlockPos feet = min.offset(o[0], 0, o[1]);
            BlockPos below = feet.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                level.setBlock(below, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_ALL);
            }
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos p = feet.above(dy);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        return new Vec3(min.getX() + 2.0D, min.getY(), min.getZ() - 1.5D);
    }

    /** Loads (and generates) the chunks the portal touches; in the mirror, copies overworld builds first. */
    private static void loadAndSync(ServerLevel level, BlockPos min) {
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int dx = -2; dx <= PortalShape.SIZE + 1; dx++) {
            for (int dz = -2; dz <= PortalShape.SIZE + 1; dz++) {
                BlockPos p = min.offset(dx, 0, dz);
                if (!seen.add(net.minecraft.world.level.ChunkPos.asLong(p))) {
                    continue;
                }
                level.getChunk(p.getX() >> 4, p.getZ() >> 4);
                if (ModDimensions.isMirror(level)) {
                    MirrorSync.ensureProcessed(level, p);
                }
            }
        }
    }
}
