package com.stormray.network;

import com.stormray.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A lightning flash of the given strength (0..1). */
public record StormFlashPacket(float strength) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.strength);
    }

    public static StormFlashPacket decode(FriendlyByteBuf buf) {
        return new StormFlashPacket(buf.readFloat());
    }

    public static void handle(StormFlashPacket message, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleFlash(message));
    }
}
