package com.stormray.dimension;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Remembers which mirror chunks have already been synchronised with the overworld. */
public class MirrorChunkData extends SavedData {
    private static final String NAME = "stormray_mirror_chunks";
    private final LongOpenHashSet done = new LongOpenHashSet();

    public static MirrorChunkData get(ServerLevel mirror) {
        return mirror.getDataStorage().computeIfAbsent(MirrorChunkData::load, MirrorChunkData::new, NAME);
    }

    private static MirrorChunkData load(CompoundTag tag) {
        MirrorChunkData data = new MirrorChunkData();
        for (long pos : tag.getLongArray("done")) {
            data.done.add(pos);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("done", this.done.toLongArray());
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
}
