package com.stormray.client.render;

import com.stormray.entity.StormRayEntity;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

/** Custom boss bar: pulsing crimson-to-violet fill with a travelling vein highlight. */
public final class StormBossBar {
    private static final int WIDTH = 182;
    private static final int HEIGHT = 7;

    private StormBossBar() {
    }

    public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents contents)
                || !StormRayEntity.BOSS_BAR_KEY.equals(contents.getKey())) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics graphics = event.getGuiGraphics();
        int x = event.getX();
        int y = event.getY();
        float progress = Mth.clamp(boss.getProgress(), 0.0F, 1.0F);
        long millis = Util.getMillis();
        float pulse = 0.5F + 0.5F * Mth.sin(millis / 260.0F);

        // frame
        graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 2, 0xF0080005);
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF3A0812);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF14040A);

        // fill
        int filled = (int) (WIDTH * progress);
        if (filled > 0) {
            int top = 0xFF000000 | ((int) (200 + 55 * pulse) << 16) | (0x18 << 8) | 0x30;
            graphics.fillGradient(x, y, x + filled, y + HEIGHT, top, 0xFF4A0636);
            int vein = (int) ((millis / 9L) % (WIDTH + 30)) - 15;
            int veinStart = Math.max(x, x + vein);
            int veinEnd = Math.min(x + filled, x + vein + 14);
            if (veinEnd > veinStart) {
                graphics.fill(veinStart, y + 1, veinEnd, y + HEIGHT - 2, 0x66FFB0C0);
            }
        }
        // notches every 10%
        for (int i = 1; i < 10; i++) {
            int nx = x + WIDTH * i / 10;
            graphics.fill(nx, y, nx + 1, y + HEIGHT, 0x99000000);
        }

        Font font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, boss.getName(), x + WIDTH / 2, y - 10, 0xFFFF4A4A);
        event.setIncrement(Math.max(event.getIncrement(), 24));
    }
}
