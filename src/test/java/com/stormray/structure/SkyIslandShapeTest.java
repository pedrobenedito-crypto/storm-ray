package com.stormray.structure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyIslandShapeTest {
    private static final int[] SEEDS = {1, 42, 777, 12345, -99, 20240607};

    @Test
    void chestSitsOnThePlinth() {
        for (int seed : SEEDS) {
            assertEquals(SkyIslandShape.TOPSOIL, SkyIslandShape.at(0, 0, 0, seed));
            assertEquals(SkyIslandShape.BRICK, SkyIslandShape.at(0, 1, 0, seed));
            assertEquals(SkyIslandShape.CHEST, SkyIslandShape.at(0, 2, 0, seed));
            assertEquals(SkyIslandShape.AIR, SkyIslandShape.at(0, 3, 0, seed), "the chest must be able to open");
        }
    }

    @Test
    void exactlyOneChest() {
        for (int seed : SEEDS) {
            int chests = 0;
            int r = SkyIslandShape.RADIUS;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int y = -SkyIslandShape.DEPTH; y <= SkyIslandShape.TOP; y++) {
                        if (SkyIslandShape.at(dx, y, dz, seed) == SkyIslandShape.CHEST) {
                            chests++;
                        }
                    }
                }
            }
            assertEquals(1, chests);
        }
    }

    @Test
    void staysInsideItsBounds() {
        int r = SkyIslandShape.RADIUS;
        for (int seed : SEEDS) {
            for (int d = -r - 3; d <= r + 3; d++) {
                assertEquals(0, SkyIslandShape.depth(r + 1, d, seed));
                assertEquals(0, SkyIslandShape.depth(d, -r - 1, seed));
            }
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    assertTrue(SkyIslandShape.depth(dx, dz, seed) <= SkyIslandShape.DEPTH);
                    assertEquals(SkyIslandShape.AIR, SkyIslandShape.at(dx, SkyIslandShape.TOP + 1, dz, seed));
                    assertEquals(SkyIslandShape.AIR, SkyIslandShape.at(dx, -SkyIslandShape.DEPTH, dz, seed));
                }
            }
        }
    }

    @Test
    void undersideTapersTowardsTheRim() {
        for (int seed : SEEDS) {
            int middle = SkyIslandShape.depth(0, 0, seed);
            assertTrue(middle >= 9, "seed " + seed + ": island too thin (" + middle + ")");
            int rim = 0;
            for (int d = 6; d <= SkyIslandShape.RADIUS; d++) {
                int depth = SkyIslandShape.depth(d, 0, seed);
                if (depth > 0) {
                    rim = depth; // last column still inside the island
                }
            }
            assertTrue(rim > 0 && rim < middle, "seed " + seed + ": rim " + rim + " vs middle " + middle);
        }
    }
}
