package com.stormray.dimension;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.entity.StormRayEntity;
import com.stormray.network.ModNetwork;
import com.stormray.network.StormFlashPacket;
import com.stormray.registry.ModSounds;
import com.stormray.storm.StormEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side rules of the Ashen Reflection: chunk mirroring, hostile-only life, red lightning. */
@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AshenDimensionEvents {
    /** Set on mobs that a player deliberately brought into the mirror (spawn egg, command, bucket...). */
    public static final String ALLOWED_TAG = "stormray_mirror_allowed";

    private AshenDimensionEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level && ModDimensions.isMirror(level)) {
            MirrorSync.enqueue(chunk.getPos());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MirrorSync.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MirrorSync.clear();
    }

    // ------------------------------------------------------------------ live sync: overworld -> mirror

    private static boolean isOverworld(net.minecraft.world.level.LevelAccessor level) {
        return level instanceof ServerLevel serverLevel && serverLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }

    /** Fired for practically every block change (placing, breaking, pistons, fluids, growth, commands). */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (isOverworld(event.getLevel())) {
            MirrorSync.markDirty(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onOverworldPlace(BlockEvent.EntityPlaceEvent event) {
        if (isOverworld(event.getLevel())) {
            MirrorSync.markDirty(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onOverworldBreak(BlockEvent.BreakEvent event) {
        if (isOverworld(event.getLevel())) {
            MirrorSync.markDirty(event.getPos());
        }
    }

    @SubscribeEvent
    public static void onExplosion(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (isOverworld(event.getLevel())) {
            for (BlockPos pos : event.getAffectedBlocks()) {
                MirrorSync.markDirty(pos);
            }
        }
    }

    // Make sure a chunk shows its copied builds before a player changes it, so nothing gets overwritten.
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && ModDimensions.isMirror(level)) {
            MirrorSync.ensureProcessed(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && ModDimensions.isMirror(level)) {
            MirrorSync.ensureProcessed(level, event.getPos());
            MirrorSync.ensureProcessed(level, event.getPos().relative(event.getFace() != null ? event.getFace() : net.minecraft.core.Direction.UP));
        }
    }

    // ------------------------------------------------------------------ empty and hostile

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        MobSpawnType type = event.getSpawnType();
        if (type == MobSpawnType.SPAWN_EGG || type == MobSpawnType.COMMAND || type == MobSpawnType.BUCKET
                || type == MobSpawnType.DISPENSER || type == MobSpawnType.BREEDING) {
            event.getEntity().getPersistentData().putBoolean(ALLOWED_TAG, true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !ModDimensions.isMirror(event.getLevel()) || !StormRayConfig.HOSTILE_ONLY.get()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || mob instanceof StormRayEntity) {
            return;
        }
        if (mob.getType().getCategory() == MobCategory.MONSTER) {
            return;
        }
        boolean broughtByPlayer = mob.getPersistentData().getBoolean(ALLOWED_TAG) || mob.hasCustomName() || mob.isLeashed()
                || (mob instanceof TamableAnimal tamable && tamable.isTame());
        if (!broughtByPlayer) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ red lightning

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || !ModDimensions.isMirror(level)) {
            return;
        }
        int average = StormRayConfig.MIRROR_LIGHTNING_INTERVAL.get();
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (random.nextInt(average) == 0) {
                strikeNear(level, player, random);
            } else if (random.nextInt(average) == 0) {
                ModNetwork.sendTo(player, new StormFlashPacket(0.2F + random.nextFloat() * 0.3F));
            }
        }
    }

    private static void strikeNear(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 30.0D + random.nextDouble() * 60.0D;
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) {
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        StormEnvironment.spawnVisualLightning(level, new Vec3(x + 0.5D, y, z + 0.5D));
        level.playSound(null, x + 0.5D, y, z + 0.5D, ModSounds.LIGHTNING.get(), SoundSource.WEATHER, 10.0F,
                0.7F + random.nextFloat() * 0.4F);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(x, y, z) < 160.0D * 160.0D) {
                ModNetwork.sendTo(other, new StormFlashPacket(0.7F));
            }
        }
    }
}
