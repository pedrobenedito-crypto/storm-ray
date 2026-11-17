package com.stormray.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColosseumShapeTest {
    private static final int[] SEEDS = {1, 42, 777, 12345, -99, 20240607};

    private static int facadeTop(int dx, int dz, int seed) {
        ColosseumShape.Column column = ColosseumShape.column(dx, dz, seed);
        for (int y = ColosseumShape.HEIGHT - 1; y >= 0; y--) {
            if (column.at(y) == ColosseumShape.WALL || column.at(y) == ColosseumShape.CORNICE) {
                return y + 1;
            }
        }
        return 0;
    }

    @Test
    void sigilMarksTheCentre() {
        for (int seed : SEEDS) {
            ColosseumShape.Column centre = ColosseumShape.column(0, 0, seed);
            assertEquals(ColosseumShape.SIGIL_CORE, centre.at(-1));
            assertEquals(ColosseumShape.AIR, centre.at(0), "the boss rises through open air");
        }
    }

    @Test
    void arenaIsMostlyOpenSand() {
        for (int seed : SEEDS) {
            int open = 0;
            int total = 0;
            for (int dx = -20; dx <= 20; dx++) {
                for (int dz = -20; dz <= 20; dz++) {
                    if (dx * dx + dz * dz > 20 * 20 || dx * dx + dz * dz < 16) {
                        continue;
                    }
                    ColosseumShape.Column column = ColosseumShape.column(dx, dz, seed);
                    assertEquals(ColosseumShape.FLOOR, column.at(-1));
                    total++;
                    if (column.at(1) == ColosseumShape.AIR) {
                        open++;
                    }
                }
            }
            assertTrue(open > total * 0.75, "seed " + seed + ": only " + open + " of " + total + " arena columns are open");
        }
    }

    @Test
    void sameSeedGivesSameRuin() {
        for (int dx = -46; dx <= 46; dx += 3) {
            for (int dz = -46; dz <= 46; dz += 3) {
                ColosseumShape.Column a = ColosseumShape.column(dx, dz, 42);
                ColosseumShape.Column b = ColosseumShape.column(dx, dz, 42);
                for (int y = -1; y < ColosseumShape.HEIGHT; y++) {
                    assertEquals(a.at(y), b.at(y));
                }
            }
        }
    }

    @Test
    void nothingOutsideTheFootprint() {
        int r = ColosseumShape.RADIUS;
        for (int seed : SEEDS) {
            for (int[] p : new int[][]{{r + 1, 0}, {0, -r - 1}, {r, r}, {-r, r}, {40, 40}}) {
                ColosseumShape.Column column = ColosseumShape.column(p[0], p[1], seed);
                for (int y = -1; y < ColosseumShape.HEIGHT; y++) {
                    assertEquals(ColosseumShape.AIR, column.at(y), "block at " + p[0] + "," + y + "," + p[1]);
                }
            }
            ColosseumShape.Column above = ColosseumShape.column(30, 0, seed);
            assertEquals(ColosseumShape.AIR, above.at(ColosseumShape.HEIGHT));
            assertEquals(ColosseumShape.AIR, above.at(-2));
        }
    }

    @Test
    void gatesAreNeverWalledUp() {
        for (int seed : SEEDS) {
            for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (int d = 22; d <= 42; d++) {
                    ColosseumShape.Column column = ColosseumShape.column(dir[0] * d, dir[1] * d, seed);
                    for (int y = 0; y < 5; y++) {
                        byte block = column.at(y);
                        assertTrue(block == ColosseumShape.AIR || block == ColosseumShape.RUBBLE,
                                "seed " + seed + ": gate blocked at distance " + d + ", height " + y);
                    }
                }
            }
        }
    }

    @Test
    void partStandsTallAndPartHasCollapsed() {
        for (int seed : SEEDS) {
            int highest = 0;
            int lowest = Integer.MAX_VALUE;
            for (int step = 0; step < 360; step += 3) {
                double angle = Math.toRadians(step);
                int dx = (int) Math.round(40.0D * Math.cos(angle));
                int dz = (int) Math.round(40.0D * Math.sin(angle));
                if (Math.min(Math.abs(dx), Math.abs(dz)) <= 2) {
                    continue; // gates
                }
                int top = facadeTop(dx, dz, seed);
                highest = Math.max(highest, top);
                lowest = Math.min(lowest, top);
            }
            assertTrue(highest >= 20, "seed " + seed + ": facade never stands tall (" + highest + ")");
            assertTrue(lowest <= 6, "seed " + seed + ": no collapsed stretch (" + lowest + ")");
        }
    }

    @Test
    void differentSeedsGiveDifferentRuins() {
        int differences = 0;
        for (int step = 0; step < 360; step += 5) {
            double angle = Math.toRadians(step);
            int dx = (int) Math.round(40.0D * Math.cos(angle));
            int dz = (int) Math.round(40.0D * Math.sin(angle));
            if (facadeTop(dx, dz, 1) != facadeTop(dx, dz, 2)) {
                differences++;
            }
        }
        assertNotEquals(0, differences);
    }
}
