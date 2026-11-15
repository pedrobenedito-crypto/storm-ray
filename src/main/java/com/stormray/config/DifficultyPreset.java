package com.stormray.config;

/**
 * Global difficulty presets for the Storm Ray. Pure Java so it can be unit tested.
 */
public enum DifficultyPreset {
    CASUAL(0.6D, 0.6D, 1.5D),
    NORMAL(1.0D, 1.0D, 1.0D),
    HARD(1.5D, 1.35D, 0.8D),
    NIGHTMARE(2.5D, 1.8D, 0.6D);

    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double cooldownMultiplier;

    DifficultyPreset(double healthMultiplier, double damageMultiplier, double cooldownMultiplier) {
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.cooldownMultiplier = cooldownMultiplier;
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    /** Multiplies ability cooldowns; lower means the boss attacks more often. */
    public double cooldownMultiplier() {
        return cooldownMultiplier;
    }

    /** Applies the cooldown multiplier to a tick count, never returning less than one tick. */
    public int scaleCooldown(int ticks) {
        return Math.max(1, (int) Math.round(ticks * cooldownMultiplier));
    }
}
