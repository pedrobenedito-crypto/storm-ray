package com.stormray.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Which players are mid-dash with the phantom sword, so the renderer can hide them completely -
 * body, armour and blade - instead of leaving the armour floating as plain invisibility does.
 */
public final class DashClientData {
    private static final Int2LongMap UNTIL = new Int2LongOpenHashMap();

    private DashClientData() {
    }

    public static void receive(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (ticks <= 0) {
            UNTIL.remove(entityId);
        } else {
            UNTIL.put(entityId, mc.level.getGameTime() + ticks);
        }
    }

    /** True while this entity is dashing and still invisible. */
    public static boolean isDashing(Entity entity) {
        if (UNTIL.isEmpty() || !entity.isInvisible()) {
            return false;   // milk, or the effect ran out early: show them again
        }
        long until = UNTIL.get(entity.getId());
        if (until == 0L) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() >= until) {
            UNTIL.remove(entity.getId());
            return false;
        }
        return true;
    }

    public static void clear() {
        UNTIL.clear();
    }
}
