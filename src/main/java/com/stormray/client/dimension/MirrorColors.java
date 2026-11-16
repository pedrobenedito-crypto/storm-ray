package com.stormray.client.dimension;

import com.stormray.dimension.ModDimensions;
import net.minecraft.client.Minecraft;

/** Colour helpers for the Ashen Reflection (client only). */
public final class MirrorColors {
    private MirrorColors() {
    }

    public static boolean inMirror() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && ModDimensions.isMirror(mc.level);
    }

    /** Pushes any biome water colour toward a deep, dark ocean blue. Keeps the alpha byte. */
    public static int deepWater(int argb) {
        int a = argb & 0xFF000000;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int nr = (int) (r * 0.28F + 4);
        int ng = (int) (g * 0.36F + 10);
        int nb = (int) (b * 0.52F + 22);
        return a | (Math.min(255, nr) << 16) | (Math.min(255, ng) << 8) | Math.min(255, nb);
    }
}
