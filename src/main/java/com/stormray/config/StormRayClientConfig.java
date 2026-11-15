package com.stormray.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Per-player visual settings, written to config/stormray-client.toml.
 */
public final class StormRayClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue SKY_TINT;
    public static final ForgeConfigSpec.DoubleValue SCREEN_TINT;
    public static final ForgeConfigSpec.BooleanValue THICK_FOG;
    public static final ForgeConfigSpec.BooleanValue LIGHTNING_FLASHES;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_PARTICLES;
    public static final ForgeConfigSpec.DoubleValue AMBIENCE_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Red sky visuals").push("visuals");
        SKY_TINT = b.comment("Strength of the red sky dome and storm clouds (0 disables).")
                .defineInRange("skyTintStrength", 1.0D, 0.0D, 1.0D);
        SCREEN_TINT = b.comment("Strength of the red screen tint (0 disables).")
                .defineInRange("screenTintStrength", 1.0D, 0.0D, 1.0D);
        THICK_FOG = b.comment("Use thick red fog during the event.")
                .define("thickFog", true);
        LIGHTNING_FLASHES = b.comment("Full-screen lightning flashes. Turn off if flashing lights bother you.")
                .define("lightningFlashes", true);
        AMBIENT_PARTICLES = b.comment("Multiplier for floating red ash particles.")
                .defineInRange("ambientParticles", 1.0D, 0.0D, 3.0D);
        AMBIENCE_VOLUME = b.comment("Volume of the storm ambience loop.")
                .defineInRange("ambienceVolume", 0.8D, 0.0D, 1.0D);
        b.pop();
        SPEC = b.build();
    }

    private StormRayClientConfig() {
    }
}
