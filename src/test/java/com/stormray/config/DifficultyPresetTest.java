package com.stormray.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifficultyPresetTest {
    @Test
    void presetsGetHarderInOrder() {
        DifficultyPreset[] presets = DifficultyPreset.values();
        for (int i = 1; i < presets.length; i++) {
            assertTrue(presets[i].healthMultiplier() > presets[i - 1].healthMultiplier());
            assertTrue(presets[i].damageMultiplier() > presets[i - 1].damageMultiplier());
            assertTrue(presets[i].cooldownMultiplier() < presets[i - 1].cooldownMultiplier());
        }
    }

    @Test
    void normalIsNeutral() {
        assertEquals(1.0, DifficultyPreset.NORMAL.healthMultiplier());
        assertEquals(600, DifficultyPreset.NORMAL.scaleCooldown(600));
    }

    @Test
    void cooldownNeverDropsBelowOneTick() {
        assertEquals(1, DifficultyPreset.NIGHTMARE.scaleCooldown(0));
    }
}
