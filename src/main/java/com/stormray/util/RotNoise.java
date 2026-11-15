package com.stormray.util;

/**
 * Deterministic pseudo-random values derived only from block coordinates. The decay of the
 * mirror world must give the same result whether it is applied while a chunk generates or
 * later, when overworld builds are copied in; that is why nothing here uses a RandomSource.
 * Pure Java so it can be unit tested.
 */
public final class RotNoise {
    private RotNoise() {
    }

    /** Stable hash of a position mixed with a salt. */
    public static long hash(int x, int y, int z, int salt) {
        long h = (long) x * 3129871L ^ (long) z * 116129781L ^ (long) y * 668265263L ^ (long) salt * 0x9E3779B97F4A7C15L;
        h = h * h * 42317861L + h * 11L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        return h;
    }

    /** Uniform value in [0, 1). */
    public static float unit(int x, int y, int z, int salt) {
        return (float) ((hash(x, y, z, salt) >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Smooth 2D value noise in [0, 1) with features roughly {@code scale} blocks wide. */
    public static float smooth(int x, int z, int scale, int salt) {
        double fx = x / (double) scale;
        double fz = z / (double) scale;
        int x0 = (int) Math.floor(fx);
        int z0 = (int) Math.floor(fz);
        double tx = fx - x0;
        double tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        float a = unit(x0, 0, z0, salt);
        float b = unit(x0 + 1, 0, z0, salt);
        float c = unit(x0, 0, z0 + 1, salt);
        float d = unit(x0 + 1, 0, z0 + 1, salt);
        double top = a + (b - a) * tx;
        double bottom = c + (d - c) * tx;
        return (float) (top + (bottom - top) * tz);
    }
}
