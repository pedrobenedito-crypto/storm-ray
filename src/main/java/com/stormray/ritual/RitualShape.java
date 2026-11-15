package com.stormray.ritual;

/**
 * Geometry of the summoning ritual. Pure Java so it can be unit tested.
 *
 * <pre>
 *        [head]
 *   [R]   [R]   [R]    &lt;- three redstone blocks in a row
 *         [R]          &lt;- one redstone block under the centre
 * </pre>
 */
public final class RitualShape {
    private RitualShape() {
    }

    /**
     * Offsets (dx, dy, dz) from the head position to every redstone block.
     *
     * @param armsAlongX true when the horizontal bar runs along the X axis, false for the Z axis
     */
    public static int[][] redstoneOffsets(boolean armsAlongX) {
        if (armsAlongX) {
            return new int[][]{{0, -1, 0}, {-1, -1, 0}, {1, -1, 0}, {0, -2, 0}};
        }
        return new int[][]{{0, -1, 0}, {0, -1, -1}, {0, -1, 1}, {0, -2, 0}};
    }

    /** Offset from the head to the bottom block of the T, where the creature emerges. */
    public static int[] baseOffset() {
        return new int[]{0, -2, 0};
    }
}
