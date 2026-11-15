package com.stormray.network;

import com.stormray.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Petrification progress of one entity. total == 0 clears it; finished keeps the entity
 * fully stone-coloured while it plays its death animation.
 */
public record PetrifyPacket(int entityId, int elapsed, int total, boolean finished) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.elapsed);
        buf.writeVarInt(this.total);
        buf.writeBoolean(this.finished);
    }

    public static PetrifyPacket decode(FriendlyByteBuf buf) {
        return new PetrifyPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(PetrifyPacket message, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handlePetrify(message));
    }
}
