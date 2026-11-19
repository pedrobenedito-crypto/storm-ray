package com.stormray.network;

import com.stormray.ability.PetrifyGaze;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: the player pressed the petrifying-gaze key. It carries nothing; the server
 * decides whether the player may use it, what they are looking at, and what happens.
 */
public record PetrifyAbilityPacket() {
    public static void encode(PetrifyAbilityPacket packet, FriendlyByteBuf buffer) {
    }

    public static PetrifyAbilityPacket decode(FriendlyByteBuf buffer) {
        return new PetrifyAbilityPacket();
    }

    public static void handle(PetrifyAbilityPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                PetrifyGaze.cast(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
