package com.stormray.util;

/**
 * Small math helpers shared by server and client. Pure Java so it can be unit tested.
 */
public final class StormMath {
    private StormMath() {
    }

    /**
     * 1 inside {@code radius - fade}, fading linearly to 0 at {@code radius}, 0 beyond.
     */
    public static float edgeFalloff(double distance, double radius, double fade) {
        if (distance >= radius) {
            return 0.0F;
        }
        double inner = radius - Math.max(fade, 1.0E-6D);
        if (distance <= inner) {
            return 1.0F;
        }
        return (float) ((radius - distance) / (radius - inner));
    }

    /** Smoothstep easing for 0..1 input. */
    public static float easeInOut(float t) {
        float x = clamp01(t);
        return x * x * (3.0F - 2.0F * x);
    }

    /** Moves {@code current} toward {@code target} by at most {@code step}. */
    public static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(current + step, target);
        }
        return Math.max(current - step, target);
    }

    public static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
    }

    /**
     * Health actually given to the entity (vanilla caps max health at 1024) and the
     * damage scale that emulates the rest. Returns {cappedHealth, damageTakenScale}.
     */
    public static double[] capHealth(double desiredHealth, double cap) {
        double capped = Math.min(desiredHealth, cap);
        double scale = desiredHealth <= 0.0D ? 1.0D : capped / desiredHealth;
        return new double[]{capped, scale};
    }
}
