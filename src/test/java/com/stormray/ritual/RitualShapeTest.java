package com.stormray.ritual;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RitualShapeTest {
    @Test
    void tShapeHasFourDistinctRedstoneBlocks() {
        for (boolean alongX : new boolean[]{true, false}) {
            int[][] offsets = RitualShape.redstoneOffsets(alongX);
            assertEquals(4, offsets.length);
            Set<String> unique = new HashSet<>();
            for (int[] o : offsets) {
                unique.add(o[0] + "," + o[1] + "," + o[2]);
            }
            assertEquals(4, unique.size());
        }
    }

    @Test
    void headSitsOnTopOfTheCentreBlock() {
        for (boolean alongX : new boolean[]{true, false}) {
            Set<String> unique = new HashSet<>();
            for (int[] o : RitualShape.redstoneOffsets(alongX)) {
                unique.add(o[0] + "," + o[1] + "," + o[2]);
            }
            assertTrue(unique.contains("0,-1,0"), "centre of the bar directly under the head");
            assertTrue(unique.contains("0,-2,0"), "stem under the centre");
        }
    }

    @Test
    void armsRunAlongTheRequestedAxis() {
        for (int[] o : RitualShape.redstoneOffsets(true)) {
            assertEquals(0, o[2]);
        }
        for (int[] o : RitualShape.redstoneOffsets(false)) {
            assertEquals(0, o[0]);
        }
    }

    @Test
    void creatureEmergesFromTheStem() {
        assertArrayEquals(new int[]{0, -2, 0}, RitualShape.baseOffset());
    }
}
