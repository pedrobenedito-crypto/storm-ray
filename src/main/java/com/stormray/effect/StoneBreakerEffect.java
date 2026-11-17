package com.stormray.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Stone Breaker: while it lasts, the Storm Ray's petrification pulse cannot turn the holder
 * to stone, and a petrification already under way crumbles off. The immunity itself is
 * applied in {@link com.stormray.petrify.PetrificationManager}.
 */
public class StoneBreakerEffect extends MobEffect {
    /** Near-black: this is also the colour of the potion's liquid. */
    public static final int COLOR = 0x0A0A0C;

    public StoneBreakerEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }
}
