package com.stormray.client;

import com.stormray.network.DashPacket;
import com.stormray.network.PetrifyPacket;
import com.stormray.network.StormFlashPacket;
import com.stormray.network.StormStatePacket;
import com.stormray.petrify.PetrifyClientData;

/** Client-only packet handling, reached through DistExecutor so servers never load it. */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleStormState(StormStatePacket packet) {
        ClientStormState.receive(packet.intensity(), packet.charge());
    }

    public static void handleFlash(StormFlashPacket packet) {
        ClientStormState.triggerFlash(packet.strength());
    }

    public static void handlePetrify(PetrifyPacket packet) {
        PetrifyClientData.update(packet.entityId(), packet.elapsed(), packet.total(), packet.finished());
    }

    public static void handleDash(DashPacket packet) {
        DashClientData.receive(packet.entityId(), packet.ticks());
    }
}
