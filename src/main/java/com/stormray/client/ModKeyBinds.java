package com.stormray.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.stormray.StormRayMod;
import com.stormray.network.ModNetwork;
import com.stormray.network.PetrifyAbilityPacket;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** The mod's key bindings. R by default; the player can rebind it in the controls screen. */
public final class ModKeyBinds {
    public static final String CATEGORY = "key.categories.stormray";

    public static final KeyMapping PETRIFY_GAZE = new KeyMapping("key.stormray.petrify_gaze",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private ModKeyBinds() {
    }

    @Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(PETRIFY_GAZE);
        }
    }

    @Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeEvents {
        private ForgeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            while (PETRIFY_GAZE.consumeClick()) {
                ModNetwork.sendToServer(new PetrifyAbilityPacket());
            }
        }
    }
}
