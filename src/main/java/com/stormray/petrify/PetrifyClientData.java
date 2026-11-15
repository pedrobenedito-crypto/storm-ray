package com.stormray.petrify;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side mirror of petrification progress, keyed by entity network id.
 * Contains no client-only imports so common event handlers may query it safely.
 * Only touched on the client thread.
 */
public final class PetrifyClientData {
    private static final class Entry {
        int elapsed;
        int total;
        boolean finished;
        int age;
    }

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    private PetrifyClientData() {
    }

    public static void update(int entityId, int elapsed, int total, boolean finished) {
        if (total <= 0 && !finished) {
            ENTRIES.remove(entityId);
            return;
        }
        Entry entry = ENTRIES.computeIfAbsent(entityId, id -> new Entry());
        entry.total = Math.max(1, total);
        entry.elapsed = finished ? entry.total : Math.min(elapsed, entry.total);
        entry.finished = finished;
        entry.age = 0;
    }

    public static void tick() {
        Iterator<Map.Entry<Integer, Entry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Entry entry = it.next().getValue();
            entry.age++;
            if (!entry.finished && entry.elapsed < entry.total) {
                entry.elapsed++;
            }
            // Finished statues linger briefly for the death animation; stale entries expire.
            if (entry.age > (entry.finished ? 60 : 40)) {
                it.remove();
            }
        }
    }

    /** 0 = untouched, 1 = fully stone. */
    public static float progress(int entityId, float partialTick) {
        Entry entry = ENTRIES.get(entityId);
        if (entry == null) {
            return 0.0F;
        }
        if (entry.finished) {
            return 1.0F;
        }
        return Mth.clamp((entry.elapsed + partialTick) / entry.total, 0.0F, 1.0F);
    }

    public static boolean isPetrified(int entityId) {
        return ENTRIES.containsKey(entityId);
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
