package com.stormray.network;

import com.stormray.StormRayMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server -> client synchronisation. All game logic stays on the server; these packets only
 * carry what the client needs to draw: sky intensity, lightning flashes, petrification progress.
 * One packet goes the other way: the key press that asks to use the phantom armour's gaze.
 */
public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            StormRayMod.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.messageBuilder(StormStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StormStatePacket::encode)
                .decoder(StormStatePacket::decode)
                .consumerMainThread(StormStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(StormFlashPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StormFlashPacket::encode)
                .decoder(StormFlashPacket::decode)
                .consumerMainThread(StormFlashPacket::handle)
                .add();
        CHANNEL.messageBuilder(PetrifyPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PetrifyPacket::encode)
                .decoder(PetrifyPacket::decode)
                .consumerMainThread(PetrifyPacket::handle)
                .add();
        CHANNEL.messageBuilder(DashPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DashPacket::encode)
                .decoder(DashPacket::decode)
                .consumerMainThread(DashPacket::handle)
                .add();
        CHANNEL.messageBuilder(PetrifyAbilityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PetrifyAbilityPacket::encode)
                .decoder(PetrifyAbilityPacket::decode)
                .consumerMainThread(PetrifyAbilityPacket::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendTracking(Entity entity, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }
}
