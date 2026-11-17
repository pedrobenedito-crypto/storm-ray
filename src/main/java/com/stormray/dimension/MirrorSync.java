package com.stormray.dimension;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.registry.ModBlocks;
import com.stormray.registry.ModStructures;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Keeps the mirror in step with the overworld.
 *
 * <ul>
 *   <li><b>First load:</b> the first time a mirror chunk is loaded, everything players changed in
 *   that overworld chunk is copied in. Natural terrain is already identical (same seed), so only
 *   differences are written.</li>
 *   <li><b>Live sync:</b> after that, every block change in the overworld marks its column dirty.
 *   Dirty columns are copied a few per tick if the mirror chunk is loaded; otherwise they are
 *   remembered on disk and copied the next time that mirror chunk loads.</li>
 * </ul>
 *
 * Everything goes through {@link RotRules}, so the mirror stays decayed, and block entities are
 * created fresh (empty), so chest contents are never duplicated. The overworld is the source of
 * truth: a synced column replaces whatever was changed in that column on the mirror side.
 */
public final class MirrorSync {
    private static final ConcurrentLinkedQueue<Long> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<Long> QUEUED = ConcurrentHashMap.newKeySet();
    /** Overworld columns (packed x, z) changed since they were last copied. Server thread only. */
    private static final LongLinkedOpenHashSet DIRTY_COLUMNS = new LongLinkedOpenHashSet();
    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private MirrorSync() {
    }

    /** May be called from chunk-loading threads: only enqueues. */
    public static void enqueue(ChunkPos pos) {
        long key = pos.toLong();
        if (QUEUED.add(key)) {
            QUEUE.add(key);
        }
    }

    /** A block changed in the overworld at this position: its column must be copied again. */
    public static void markDirty(BlockPos pos) {
        DIRTY_COLUMNS.add(ChunkPos.asLong(pos.getX(), pos.getZ()));
    }

    /** Called every server tick: works through both queues within the configured time budget. */
    public static void tick(MinecraftServer server) {
        if (QUEUE.isEmpty() && DIRTY_COLUMNS.isEmpty()) {
            return;
        }
        ServerLevel mirror = server.getLevel(ModDimensions.ASHEN_LEVEL);
        if (mirror == null) {
            clear();
            return;
        }
        long deadline = System.nanoTime() + StormRayConfig.SYNC_MILLIS_PER_TICK.get() * 1_000_000L;
        int budget = StormRayConfig.SYNC_CHUNKS_PER_TICK.get();
        while (budget-- > 0 && System.nanoTime() < deadline) {
            Long key = QUEUE.poll();
            if (key == null) {
                break;
            }
            QUEUED.remove(key);
            process(mirror, new ChunkPos(key));
        }
        if (!DIRTY_COLUMNS.isEmpty()) {
            tickDirtyColumns(mirror, deadline);
        }
    }

    private static void tickDirtyColumns(ServerLevel mirror, long deadline) {
        if (!StormRayConfig.COPY_BUILDS.get() || !StormRayConfig.LIVE_SYNC.get()) {
            DIRTY_COLUMNS.clear();
            return;
        }
        MirrorChunkData data = MirrorChunkData.get(mirror);
        ServerLevel overworld = mirror.getServer().overworld();
        int budget = StormRayConfig.SYNC_COLUMNS_PER_TICK.get();
        LongIterator it = DIRTY_COLUMNS.iterator();
        while (it.hasNext() && budget-- > 0 && System.nanoTime() < deadline) {
            long key = it.nextLong();
            it.remove();
            int x = ChunkPos.getX(key);
            int z = ChunkPos.getZ(key);
            ChunkPos chunkPos = new ChunkPos(x >> 4, z >> 4);
            if (!data.isDone(chunkPos)) {
                continue; // never visited: the first load copies the whole chunk anyway
            }
            LevelChunk target = mirror.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            ChunkAccess source = overworld.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (target == null || source == null) {
                data.markColumnDirty(chunkPos, x & 15, z & 15); // copied when the mirror chunk next loads
                continue;
            }
            if (ModStructures.touchesModStructure(mirror, target)) {
                continue;
            }
            try {
                copyColumns(mirror, target, source, null, x & 15, z & 15);
            } catch (RuntimeException e) {
                StormRayMod.LOGGER.error("Could not mirror column {}, {}", x, z, e);
            }
        }
    }

    /** Synchronises a chunk right now if it is loaded and not done yet (used before players edit it). */
    public static void ensureProcessed(ServerLevel mirror, BlockPos pos) {
        process(mirror, new ChunkPos(pos));
    }

    public static void clear() {
        QUEUE.clear();
        QUEUED.clear();
        DIRTY_COLUMNS.clear();
    }

    private static void process(ServerLevel mirror, ChunkPos pos) {
        MirrorChunkData data = MirrorChunkData.get(mirror);
        boolean firstLoad = !data.isDone(pos);
        long[] dirty = firstLoad ? null : data.dirtyColumns(pos);
        if (!firstLoad && dirty == null) {
            return; // already copied and nothing changed since
        }
        LevelChunk mirrorChunk = mirror.getChunkSource().getChunkNow(pos.x, pos.z);
        if (mirrorChunk == null) {
            return; // not fully loaded; it will be queued again on its next load
        }
        // Chunks holding a Sky Island or the Colosseum keep what the mirror generated: the overworld
        // has no island there, and copying it over would erase the structure.
        if (StormRayConfig.COPY_BUILDS.get() && !ModStructures.touchesModStructure(mirror, mirrorChunk)) {
            ServerLevel overworld = mirror.getServer().overworld();
            ChunkAccess source = overworld.getChunkSource().getChunkNow(pos.x, pos.z);
            if (source == null) {
                // Reads the chunk from disk if it exists; a never-generated chunk comes back EMPTY and is not saved.
                source = overworld.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.EMPTY, true);
            }
            if (source != null && source.getStatus().isOrAfter(ChunkStatus.FULL)) {
                try {
                    copyColumns(mirror, mirrorChunk, source, dirty, -1, -1);
                } catch (RuntimeException e) {
                    StormRayMod.LOGGER.error("Could not mirror chunk {}", pos, e);
                }
            }
        }
        data.markDone(pos);
        data.clearDirtyColumns(pos);
    }

    /**
     * Copies columns of a chunk from the overworld into the mirror.
     *
     * @param mask    256-bit set of columns to copy (bit = lx + lz * 16), or null for all of them
     * @param onlyX   if not negative, copy just this one column (with {@code onlyZ}); the mask is ignored
     */
    private static void copyColumns(ServerLevel mirror, LevelChunk target, ChunkAccess source, long[] mask, int onlyX, int onlyZ) {
        if (target.getMinBuildHeight() != source.getMinBuildHeight() || target.getHeight() != source.getHeight()) {
            return;
        }
        int height = Math.max(AshenRotFeature.columnHeight(target), AshenRotFeature.columnHeight(source));
        if (height <= 0) {
            return;
        }
        int minX = target.getPos().getMinBlockX();
        int minZ = target.getPos().getMinBlockZ();
        int minY = target.getMinBuildHeight();
        Block portal = ModBlocks.ASHEN_PORTAL.get();
        BlockState[] column = new BlockState[height];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                if (onlyX >= 0) {
                    if (lx != onlyX || lz != onlyZ) {
                        continue;
                    }
                } else if (mask != null) {
                    int bit = lx + lz * 16;
                    if ((mask[bit >> 6] & (1L << (bit & 63))) == 0L) {
                        continue;
                    }
                }
                for (int i = 0; i < height; i++) {
                    column[i] = source.getSection(i >> 4).getBlockState(lx, i & 15, lz);
                }
                BlockState[] wanted = RotRules.rotColumn(column, minX + lx, minZ + lz, minY);
                for (int i = 0; i < height; i++) {
                    BlockState current = target.getSection(i >> 4).getBlockState(lx, i & 15, lz);
                    if (current != wanted[i] && !current.is(portal)) {
                        // an open portal on the mirror side is the way home: never overwrite it
                        mirror.setBlock(pos.set(minX + lx, minY + i, minZ + lz), wanted[i], SET_FLAGS);
                    }
                }
            }
        }
    }
}
