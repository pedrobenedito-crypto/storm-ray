package com.stormray.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotNoiseTest {
    @Test
    void sameCoordinatesAlwaysGiveSameValue() {
        // generation-time decay and the later copy of overworld builds must agree exactly
        for (int i = 0; i < 1000; i++) {
            int x = i * 37 - 5000;
            int y = (i * 13) % 384 - 64;
            int z = i * -91 + 1234;
            assertEquals(RotNoise.unit(x, y, z, 7), RotNoise.unit(x, y, z, 7));
            assertEquals(RotNoise.smooth(x, z, 14, 3), RotNoise.smooth(x, z, 14, 3));
        }
    }

    @Test
    void valuesStayInUnitRangeAndAreSpread() {
        int low = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            float v = RotNoise.unit(i, i % 300, -i * 3, 11);
            assertTrue(v >= 0.0F && v < 1.0F);
            if (v < 0.42F) {
                low++;
            }
            float s = RotNoise.smooth(i, -i, 14, 53);
            assertTrue(s >= 0.0F && s < 1.0F);
        }
        double fraction = low / (double) n;
        assertTrue(fraction > 0.38 && fraction < 0.46, "leaf-fall fraction " + fraction);
    }

    @Test
    void saltsDecorrelate() {
        int same = 0;
        for (int i = 0; i < 2000; i++) {
            if ((RotNoise.unit(i, 64, i, 1) < 0.5F) == (RotNoise.unit(i, 64, i, 2) < 0.5F)) {
                same++;
            }
        }
        assertTrue(same > 850 && same < 1150);
    }
}
