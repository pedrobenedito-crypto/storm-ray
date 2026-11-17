package com.stormray.dimension;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

/**
 * Remembers which mirror chunks have already been synchronised with the overworld, and which
 * columns of those chunks changed in the overworld while the mirror chunk was not loaded.
 */
public class MirrorChunkData extends SavedData {
    private static final String NAME = "stormray_mirror_chunks";
    private final LongOpenHashSet done = new LongOpenHashSet();
    /** Per chunk, a 256-bit set of columns waiting to be copied (bit = lx + lz * 16). */
    private final Long2ObjectOpenHashMap<long[]> dirty = new Long2ObjectOpenHashMap<>();

    public static MirrorChunkData get(ServerLevel mirror) {
        return mirror.getDataStorage().computeIfAbsent(MirrorChunkData::load, MirrorChunkData::new, NAME);
    }

    private static MirrorChunkData load(CompoundTag tag) {
        MirrorChunkData data = new MirrorChunkData();
        for (long pos : tag.getLongArray("done")) {
            data.done.add(pos);
        }
        ListTag list = tag.getList("dirty", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long[] mask = entry.getLongArray("mask");
            if (mask.length == 4) {
                data.dirty.put(entry.getLong("pos"), mask);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("done", this.done.toLongArray());
        ListTag list = new ListTag();
        for (Long2ObjectMap.Entry<long[]> entry : this.dirty.long2ObjectEntrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong("pos", entry.getLongKey());
            item.putLongArray("mask", entry.getValue());
            list.add(item);
        }
        tag.put("dirty", list);
        return tag;
    }

    public boolean isDone(ChunkPos pos) {
        return this.done.contains(pos.toLong());
    }

    public void markDone(ChunkPos pos) {
        if (this.done.add(pos.toLong())) {
            setDirty();
        }
    }

    /** Remembers that a column (local 0..15 coordinates) must be copied when the chunk next loads. */
    public void markColumnDirty(ChunkPos pos, int localX, int localZ) {
        long[] mask = this.dirty.get(pos.toLong());
        if (mask == null) {
            mask = new long[4];
            this.dirty.put(pos.toLong(), mask);
        }
        int bit = localX + localZ * 16;
        long flag = 1L << (bit & 63);
        if ((mask[bit >> 6] & flag) == 0L) {
            mask[bit >> 6] |= flag;
            setDirty();
        }
    }

    /** The columns waiting in this chunk, or null if there are none. */
    @Nullable
    public long[] dirtyColumns(ChunkPos pos) {
        return this.dirty.get(pos.toLong());
    }

    public void clearDirtyColumns(ChunkPos pos) {
        if (this.dirty.remove(pos.toLong()) != null) {
            setDirty();
        }
    }
}
