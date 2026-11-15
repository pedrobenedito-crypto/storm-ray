package com.stormray.client.sound;

import com.stormray.client.ClientStormState;
import com.stormray.config.StormRayClientConfig;
import com.stormray.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Non-positional storm loop whose volume follows the red-sky intensity. */
public class StormAmbienceSound extends AbstractTickableSoundInstance {
    private static StormAmbienceSound current;
    private static int retryCooldown;
    private int silentTicks;

    private StormAmbienceSound() {
        super(ModSounds.STORM_LOOP.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        float intensity = ClientStormState.intensity(1.0F);
        if (intensity <= 0.01F) {
            if (++this.silentTicks > 40) {
                this.stop();
                return;
            }
        } else {
            this.silentTicks = 0;
        }
        this.volume = Math.max(0.01F, intensity * StormRayClientConfig.AMBIENCE_VOLUME.get().floatValue());
    }

    /** Starts the loop when the storm begins; the instance stops itself once it fades out. */
    public static void tickManager(Minecraft mc) {
        if (retryCooldown > 0) {
            retryCooldown--;
        }
        if (ClientStormState.intensity(1.0F) < 0.05F || retryCooldown > 0) {
            return;
        }
        if (current == null || current.isStopped() || !mc.getSoundManager().isActive(current)) {
            if (current != null && !current.isStopped()) {
                mc.getSoundManager().stop(current);
            }
            current = new StormAmbienceSound();
            mc.getSoundManager().play(current);
            retryCooldown = 40;
        }
    }
}
