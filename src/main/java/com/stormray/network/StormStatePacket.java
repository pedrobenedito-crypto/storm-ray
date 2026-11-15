package com.stormray.network;

import com.stormray.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Red-sky intensity (0..1) and charge progress (0..1) for one player. */
public record StormStatePacket(float intensity, float charge) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.intensity);
        buf.writeFloat(this.charge);
    }

    public static StormStatePacket decode(FriendlyByteBuf buf) {
        return new StormStatePacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(StormStatePacket message, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleStormState(message));
    }
}
