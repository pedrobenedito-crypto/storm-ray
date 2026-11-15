package com.stormray.dimension;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
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
 * Copies player-made changes from the overworld into the mirror, once per chunk, the first
 * time the mirror chunk is loaded. Natural terrain is already identical (same seed), so only
 * differences are written: decayed through {@link RotRules}, with fresh (empty) block entities,
 * so chest contents are never duplicated.
 */
public final class MirrorSync {
    private static final ConcurrentLinkedQueue<Long> QUEUE = new ConcurrentLinkedQueue<>();
    private static final Set<Long> QUEUED = ConcurrentHashMap.newKeySet();
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

    /** Called every server tick: works through the queue within the configured time budget. */
    public static void tick(MinecraftServer server) {
        if (QUEUE.isEmpty()) {
            return;
        }
        ServerLevel mirror = server.getLevel(ModDimensions.ASHEN_LEVEL);
        if (mirror == null) {
            QUEUE.clear();
            QUEUED.clear();
            return;
        }
        long deadline = System.nanoTime() + StormRayConfig.SYNC_MILLIS_PER_TICK.get() * 1_000_000L;
        int budget = StormRayConfig.SYNC_CHUNKS_PER_TICK.get();
        while (budget-- > 0 && System.nanoTime() < deadline) {
            Long key = QUEUE.poll();
            if (key == null) {
                return;
            }
            QUEUED.remove(key);
            process(mirror, new ChunkPos(key));
        }
    }

    /** Synchronises a chunk right now if it is loaded and not done yet (used before players edit it). */
    public static void ensureProcessed(ServerLevel mirror, BlockPos pos) {
        process(mirror, new ChunkPos(pos));
    }

    public static void clear() {
        QUEUE.clear();
        QUEUED.clear();
    }

    private static void process(ServerLevel mirror, ChunkPos pos) {
        MirrorChunkData data = MirrorChunkData.get(mirror);
        if (data.isDone(pos)) {
            return;
        }
        LevelChunk mirrorChunk = mirror.getChunkSource().getChunkNow(pos.x, pos.z);
        if (mirrorChunk == null) {
            return; // not fully loaded; it will be queued again on its next load
        }
        if (StormRayConfig.COPY_BUILDS.get()) {
            ServerLevel overworld = mirror.getServer().overworld();
            ChunkAccess source = overworld.getChunkSource().getChunkNow(pos.x, pos.z);
            if (source == null) {
                // Reads the chunk from disk if it exists; a never-generated chunk comes back EMPTY and is not saved.
                source = overworld.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.EMPTY, true);
            }
            if (source != null && source.getStatus().isOrAfter(ChunkStatus.FULL)) {
                try {
                    copy(mirror, mirrorChunk, source);
                } catch (RuntimeException e) {
                    StormRayMod.LOGGER.error("Could not mirror chunk {}", pos, e);
                }
            }
        }
        data.markDone(pos);
    }

    private static void copy(ServerLevel mirror, LevelChunk target, ChunkAccess source) {
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
        BlockState[] column = new BlockState[height];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int i = 0; i < height; i++) {
                    column[i] = source.getSection(i >> 4).getBlockState(lx, i & 15, lz);
                }
                BlockState[] wanted = RotRules.rotColumn(column, minX + lx, minZ + lz, minY);
                for (int i = 0; i < height; i++) {
                    BlockState current = target.getSection(i >> 4).getBlockState(lx, i & 15, lz);
                    if (current != wanted[i]) {
                        mirror.setBlock(pos.set(minX + lx, minY + i, minZ + lz), wanted[i], SET_FLAGS);
                    }
                }
            }
        }
    }
}
