package com.stormray.storm;

import com.stormray.config.StormRayConfig;
import com.stormray.entity.StormRayEntity;
import com.stormray.network.ModNetwork;
import com.stormray.network.StormFlashPacket;
import com.stormray.network.StormStatePacket;
import com.stormray.registry.ModSounds;
import com.stormray.util.StormMath;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * Server side of the red-sky event. Nothing in the world is changed: weather, time and
 * biomes are untouched. Each nearby player only receives an intensity value; the client
 * renders the sky from it, and it decays to normal on its own if updates stop.
 */
public final class StormEnvironment {
    private StormEnvironment() {
    }

    public static void tick(StormRayEntity ray) {
        if (!(ray.level() instanceof ServerLevel level)) {
            return;
        }
        float strength = ray.environmentStrength();
        StormRayEntity.Phase phase = ray.getPhase();
        int interval = (phase == StormRayEntity.Phase.CHARGING || phase == StormRayEntity.Phase.SUMMONING || ray.isDeadOrDying()) ? 4 : 10;
        if (ray.tickCount % interval == 0) {
            syncPlayers(ray, level, strength);
        }

        if (StormRayConfig.LIGHTNING_ENABLED.get() && strength > 0.4F && !ray.isDeadOrDying()) {
            RandomSource random = ray.getRandom();
            int average = StormRayConfig.LIGHTNING_INTERVAL.get();
            if (phase == StormRayEntity.Phase.CHARGING) {
                average = Math.max(5, average / 3);
            }
            if (random.nextInt(average) == 0) {
                strikeDistantLightning(ray, level, random);
            } else if (random.nextInt(average) == 0) {
                flash(ray, 0.2F + random.nextFloat() * 0.35F); // in-cloud flicker
            }
        }
    }

    private static void syncPlayers(StormRayEntity ray, ServerLevel level, float strength) {
        double radius = StormRayConfig.ENVIRONMENT_RADIUS.get();
        double fade = Math.min(32.0D, radius * 0.25D);
        float charge = ray.chargeProgress(0.0F);
        Set<UUID> tracked = ray.environmentPlayers();

        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - ray.getX();
            double dz = player.getZ() - ray.getZ();
            float intensity = strength * StormMath.edgeFalloff(Math.sqrt(dx * dx + dz * dz), radius, fade);
            if (intensity > 0.0F) {
                tracked.add(player.getUUID());
                ray.getBossEvent().addPlayer(player);
                ModNetwork.sendTo(player, new StormStatePacket(intensity, charge));
            } else if (tracked.remove(player.getUUID())) {
                ray.getBossEvent().removePlayer(player);
                ModNetwork.sendTo(player, new StormStatePacket(0.0F, 0.0F));
            }
        }

        // Players who changed dimension or logged out.
        MinecraftServer server = level.getServer();
        Iterator<UUID> it = tracked.iterator();
        while (it.hasNext()) {
            UUID id = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null || player.level() != level) {
                it.remove();
                if (player != null) {
                    ray.getBossEvent().removePlayer(player);
                    ModNetwork.sendTo(player, new StormStatePacket(0.0F, 0.0F));
                }
            }
        }
    }

    /** Called when the boss is removed (death, unload, discard): restores every affected player's sky. */
    public static void release(StormRayEntity ray) {
        if (!(ray.level() instanceof ServerLevel level)) {
            return;
        }
        for (UUID id : ray.environmentPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                ModNetwork.sendTo(player, new StormStatePacket(0.0F, 0.0F));
            }
        }
        ray.environmentPlayers().clear();
        ray.getBossEvent().removeAllPlayers();
    }

    /** Sends a lightning flash to every player inside the event. */
    public static void flash(StormRayEntity ray, float strength) {
        if (!(ray.level() instanceof ServerLevel level)) {
            return;
        }
        for (UUID id : ray.environmentPlayers()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                ModNetwork.sendTo(player, new StormFlashPacket(strength));
            }
        }
    }

    private static void strikeDistantLightning(StormRayEntity ray, ServerLevel level, RandomSource random) {
        double maxDistance = Math.min(StormRayConfig.ENVIRONMENT_RADIUS.get(), 96.0D);
        double distance = 20.0D + random.nextDouble() * Math.max(1.0D, maxDistance - 20.0D);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int x = Mth.floor(ray.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(ray.getZ() + Math.sin(angle) * distance);
        BlockPos column = new BlockPos(x, ray.getBlockY(), z);
        if (!level.isLoaded(column)) {
            return; // never load chunks just for decoration
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        spawnVisualLightning(level, new Vec3(x + 0.5D, y, z + 0.5D));
        level.playSound(null, x + 0.5D, y, z + 0.5D, ModSounds.LIGHTNING.get(), SoundSource.WEATHER, 12.0F, 0.7F + random.nextFloat() * 0.4F);
        flash(ray, 0.8F);
    }

    /** Harmless lightning: no fire, no damage, no charged creepers. */
    public static void spawnVisualLightning(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(pos);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }
}
