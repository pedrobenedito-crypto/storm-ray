package com.stormray.structure;

import com.stormray.util.RotNoise;

/**
 * Pure geometry of a Sky Island: a lump of ground torn out of the earth, with a ragged rim, a
 * tapering underside of stone and ore, and a small ruined shrine holding a chest on top.
 * y = 0 is the soil surface. No Minecraft classes, so it can be unit tested.
 */
public final class SkyIslandShape {
    public static final int RADIUS = 13;
    /** Lowest y the underside can reach. */
    public static final int DEPTH = 24;
    /** Highest y used by the shrine and the dead tree. */
    public static final int TOP = 7;

    public static final byte AIR = 0;
    public static final byte TOPSOIL = 1;
    public static final byte DIRT = 2;
    public static final byte STONE = 3;
    public static final byte ORE = 4;
    public static final byte BRICK = 5;
    public static final byte PILLAR = 6;
    public static final byte LOG = 7;
    public static final byte CHEST = 8;
    public static final byte GLOW = 9;

    private SkyIslandShape() {
    }

    /** Rim radius in the direction of (dx, dz): between about 8.5 and 12 blocks. */
    public static double rimRadius(int dx, int dz, int seed) {
        double turn = Math.atan2(dz, dx) / (Math.PI * 2.0D);
        turn -= Math.floor(turn);
        double p = turn * 9;
        int i0 = (int) Math.floor(p);
        double t = p - i0;
        t = t * t * (3.0D - 2.0D * t);
        float a = RotNoise.unit(Math.floorMod(i0, 9), 0, 0, seed + 1);
        float b = RotNoise.unit(Math.floorMod(i0 + 1, 9), 0, 0, seed + 1);
        return 8.5D + 3.5D * (a + (b - a) * t);
    }

    /** How far below the surface the island reaches in this column; 0 outside the island. */
    public static int depth(int dx, int dz, int seed) {
        double rr = Math.sqrt((double) dx * dx + (double) dz * dz) / rimRadius(dx, dz, seed);
        if (rr >= 1.0D) {
            return 0;
        }
        double depth = 2.0D + 15.0D * Math.pow(1.0D - rr * rr, 0.9D) * (0.7D + 0.6D * RotNoise.smooth(dx, dz, 4, seed + 2));
        float spike = RotNoise.unit(dx, 0, dz, seed + 3);
        if (spike < 0.07F) {
            depth += 2.0D + spike * 40.0D; // hanging stone teeth
        }
        return Math.min(DEPTH, (int) Math.round(depth));
    }

    public static byte at(int dx, int y, int dz, int seed) {
        if (y > TOP || y < -DEPTH) {
            return AIR;
        }
        int depth = depth(dx, dz, seed);
        if (depth == 0) {
            return AIR;
        }
        if (y <= 0) {
            if (y <= -depth) {
                return AIR;
            }
            if (y == 0) {
                return TOPSOIL;
            }
            if (y >= -3) {
                return DIRT;
            }
            if (y == -depth + 1 && depth > 9 && RotNoise.unit(dx, y, dz, seed + 4) < 0.12F) {
                return GLOW;
            }
            return RotNoise.unit(dx, y, dz, seed + 5) < 0.06F ? ORE : STONE;
        }
        return decoration(dx, y, dz, seed);
    }

    private static byte decoration(int dx, int y, int dz, int seed) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        // shrine: a 3x3 plinth with the chest on top
        if (ax <= 1 && az <= 1) {
            if (y == 1) {
                return BRICK;
            }
            return y == 2 && dx == 0 && dz == 0 ? CHEST : AIR;
        }
        // four broken pillars around it
        if (ax == 3 && az == 3) {
            int height = 2 + (int) (RotNoise.unit(dx, 0, dz, seed + 6) * 5.0F);
            return y <= height ? PILLAR : AIR;
        }
        // a dead tree
        if (dx == 5 && dz == -2) {
            return y <= 5 ? LOG : AIR;
        }
        if ((dx == 6 && dz == -2 && y == 4) || (dx == 5 && dz == -3 && y == 3) || (dx == 4 && dz == -2 && y == 5)) {
            return LOG;
        }
        return AIR;
    }
}
