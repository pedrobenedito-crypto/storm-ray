package com.stormray.network;

import com.stormray.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A player has dashed with the phantom sword. Mob effects are not synced to other clients, so
 * this is how everyone else learns that this player is vanishing - armour and all - and for
 * how long.
 */
public record DashPacket(int entityId, int ticks) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.ticks);
    }

    public static DashPacket decode(FriendlyByteBuf buf) {
        return new DashPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DashPacket message, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleDash(message));
        context.get().setPacketHandled(true);
    }
}
