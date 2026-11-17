package com.stormray.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.UUID;

/** Per-world state of the colosseum fight: where the arena is, who guards it, when it last fell. */
public class ColosseumData extends SavedData {
    private static final String NAME = "stormray_colosseum";

    @Nullable
    private BlockPos centre;
    @Nullable
    private UUID bossId;
    private long awakenedAt;
    private long defeatedAt = -1L;

    public static ColosseumData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ColosseumData::load, ColosseumData::new, NAME);
    }

    private static ColosseumData load(CompoundTag tag) {
        ColosseumData data = new ColosseumData();
        if (tag.contains("CentreX")) {
            data.centre = new BlockPos(tag.getInt("CentreX"), tag.getInt("CentreY"), tag.getInt("CentreZ"));
        }
        if (tag.hasUUID("Boss")) {
            data.bossId = tag.getUUID("Boss");
        }
        data.awakenedAt = tag.getLong("AwakenedAt");
        data.defeatedAt = tag.contains("DefeatedAt") ? tag.getLong("DefeatedAt") : -1L;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (this.centre != null) {
            tag.putInt("CentreX", this.centre.getX());
            tag.putInt("CentreY", this.centre.getY());
            tag.putInt("CentreZ", this.centre.getZ());
        }
        if (this.bossId != null) {
            tag.putUUID("Boss", this.bossId);
        }
        tag.putLong("AwakenedAt", this.awakenedAt);
        tag.putLong("DefeatedAt", this.defeatedAt);
        return tag;
    }

    /** Arena centre (y = arena floor), once a player has found the colosseum. */
    @Nullable
    public BlockPos centre() {
        return this.centre;
    }

    public void setCentre(BlockPos centre) {
        this.centre = centre.immutable();
        setDirty();
    }

    @Nullable
    public UUID bossId() {
        return this.bossId;
    }

    public long awakenedAt() {
        return this.awakenedAt;
    }

    /** Game time of the last defeat, or -1 if the guardian has never been beaten. */
    public long defeatedAt() {
        return this.defeatedAt;
    }

    public void bossAwakened(UUID id, long gameTime) {
        this.bossId = id;
        this.awakenedAt = gameTime;
        setDirty();
    }

    public void bossDefeated(long gameTime) {
        this.bossId = null;
        this.defeatedAt = gameTime;
        setDirty();
    }

    /** The guardian vanished without dying (peaceful mode, /kill @e, ...): let it return. */
    public void bossLost() {
        this.bossId = null;
        setDirty();
    }
}
