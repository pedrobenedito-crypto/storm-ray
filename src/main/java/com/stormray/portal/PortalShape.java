package com.stormray.portal;

/**
 * Geometry of the floor portal, relative to the interior's minimum corner (x0, y, z0).
 * Pure Java so it can be unit tested.
 *
 * <pre>
 *     R R R R          R = redstone block (corners optional)
 *   R . . . . R        . = open 4x4 interior, at the same height as the ring
 *   R . . . . R
 *   R . . . . R
 *   R . . . . R
 *     R R R R
 * </pre>
 */
public final class PortalShape {
    public static final int SIZE = 4;

    private PortalShape() {
    }

    /** The 16 open cells. */
    public static int[][] interior() {
        int[][] cells = new int[SIZE * SIZE][];
        int i = 0;
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                cells[i++] = new int[]{dx, dz};
            }
        }
        return cells;
    }

    /** The 16 required redstone blocks along the four edges. */
    public static int[][] edges() {
        int[][] cells = new int[SIZE * 4][];
        int i = 0;
        for (int k = 0; k < SIZE; k++) {
            cells[i++] = new int[]{-1, k};
            cells[i++] = new int[]{SIZE, k};
            cells[i++] = new int[]{k, -1};
            cells[i++] = new int[]{k, SIZE};
        }
        return cells;
    }

    /** The 4 corners: optional, never required and never built by the game. */
    public static int[][] corners() {
        return new int[][]{{-1, -1}, {SIZE, -1}, {-1, SIZE}, {SIZE, SIZE}};
    }

    /** Where a traveller arrives: just outside the -Z edge, facing the portal. Returns {x, z} offsets of the 2 cells. */
    public static int[][] landing() {
        return new int[][]{{1, -2}, {2, -2}};
    }
}
