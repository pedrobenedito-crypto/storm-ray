package com.stormray.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StormMathTest {
    @Test
    void edgeFalloffIsFullInsideAndZeroOutside() {
        assertEquals(1.0F, StormMath.edgeFalloff(10, 160, 32), 1.0E-6F);
        assertEquals(1.0F, StormMath.edgeFalloff(128, 160, 32), 1.0E-6F);
        assertEquals(0.5F, StormMath.edgeFalloff(144, 160, 32), 1.0E-6F);
        assertEquals(0.0F, StormMath.edgeFalloff(160, 160, 32), 1.0E-6F);
        assertEquals(0.0F, StormMath.edgeFalloff(500, 160, 32), 1.0E-6F);
    }

    @Test
    void easingStaysInRangeAndIsMonotonic() {
        float previous = -1.0F;
        for (int i = -5; i <= 25; i++) {
            float value = StormMath.easeInOut(i / 20.0F);
            assertTrue(value >= 0.0F && value <= 1.0F);
            assertTrue(value >= previous);
            previous = value;
        }
        assertEquals(0.5F, StormMath.easeInOut(0.5F), 1.0E-6F);
    }

    @Test
    void approachNeverOvershoots() {
        assertEquals(0.3F, StormMath.approach(0.2F, 1.0F, 0.1F), 1.0E-6F);
        assertEquals(1.0F, StormMath.approach(0.95F, 1.0F, 0.1F), 1.0E-6F);
        assertEquals(0.0F, StormMath.approach(0.05F, 0.0F, 0.1F), 1.0E-6F);
    }

    @Test
    void healthAboveVanillaCapBecomesDamageReduction() {
        double[] normal = StormMath.capHealth(900, 1024);
        assertEquals(900, normal[0], 1.0E-9);
        assertEquals(1.0, normal[1], 1.0E-9);

        double[] huge = StormMath.capHealth(2048, 1024);
        assertEquals(1024, huge[0], 1.0E-9);
        assertEquals(0.5, huge[1], 1.0E-9);
        // effective health is preserved: 1024 hp taking half damage == 2048 hp
        assertEquals(2048, huge[0] / huge[1], 1.0E-9);
    }
}
