package com.stormray.client;

import com.stormray.config.StormRayClientConfig;
import com.stormray.util.StormMath;
import net.minecraft.util.Mth;

/**
 * Smoothed red-sky state for the local player. If the server stops sending updates
 * (boss gone, player teleported, chunk unloaded) everything fades back to normal by itself.
 */
public final class ClientStormState {
    private static final int STALE_TICKS = 60;

    private static float intensity;
    private static float prevIntensity;
    private static float targetIntensity;
    private static float charge;
    private static float prevCharge;
    private static float targetCharge;
    private static float flash;
    private static float prevFlash;
    private static int ticksSinceUpdate = STALE_TICKS + 1;

    private ClientStormState() {
    }

    public static void receive(float newIntensity, float newCharge) {
        targetIntensity = StormMath.clamp01(newIntensity);
        targetCharge = StormMath.clamp01(newCharge);
        ticksSinceUpdate = 0;
    }

    public static void triggerFlash(float strength) {
        if (StormRayClientConfig.LIGHTNING_FLASHES.get() && intensity > 0.05F) {
            flash = Math.max(flash, StormMath.clamp01(strength));
        }
    }

    public static void tick() {
        prevIntensity = intensity;
        prevCharge = charge;
        prevFlash = flash;
        if (++ticksSinceUpdate > STALE_TICKS) {
            targetIntensity = 0.0F;
            targetCharge = 0.0F;
        }
        intensity = StormMath.approach(intensity, targetIntensity, 0.02F);
        charge = StormMath.approach(charge, targetCharge, 0.08F);
        flash = Math.max(0.0F, flash * 0.75F - 0.01F);
    }

    public static void reset() {
        intensity = prevIntensity = targetIntensity = 0.0F;
        charge = prevCharge = targetCharge = 0.0F;
        flash = prevFlash = 0.0F;
        ticksSinceUpdate = STALE_TICKS + 1;
    }

    public static float intensity(float partialTick) {
        return Mth.lerp(partialTick, prevIntensity, intensity);
    }

    public static float charge(float partialTick) {
        return Mth.lerp(partialTick, prevCharge, charge);
    }

    public static float flash(float partialTick) {
        return Mth.lerp(partialTick, prevFlash, flash);
    }

    public static boolean active() {
        return intensity > 0.001F || prevIntensity > 0.001F;
    }
}
