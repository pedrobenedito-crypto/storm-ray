package com.stormray.structure;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.dimension.ModDimensions;
import com.stormray.entity.PhantomXEntity;
import com.stormray.entity.StormRayEntity;
import com.stormray.registry.ModEntities;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import com.stormray.registry.ModStructures;
import com.stormray.storm.StormEnvironment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The colosseum fight. When a survival player walks onto the arena floor, the Storm Ray
 * tears out of the sigil in the middle and Phantom X guards step out of the ruins. After the
 * boss dies the arena sleeps for a configurable time before it can wake again.
 */
@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ColosseumEvents {
    /** Seconds a missing (not dead) guardian is waited for before the arena may wake again. */
    private static final long LOST_AFTER_TICKS = 1200L;

    private ColosseumEvents() {
    }

    private static boolean hasColosseum(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD || ModDimensions.isMirror(level);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % 20L != 7L || level.players().isEmpty() || !hasColosseum(level)) {
            return;
        }
        if (!StormRayConfig.COLOSSEUM_BOSS.get() || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        ColosseumData data = ColosseumData.get(level);
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }
            BlockPos centre = arenaUnder(level, data, player);
            if (centre != null) {
                tryAwaken(level, data, centre);
                return;
            }
        }
    }

    /** The arena centre if the player stands on the arena floor, otherwise null. */
    private static BlockPos arenaUnder(ServerLevel level, ColosseumData data, ServerPlayer player) {
        BlockPos centre = data.centre();
        if (centre == null) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), ModStructures.COLOSSEUM_KEY);
            if (!start.isValid()) {
                return null;
            }
            centre = ColosseumPiece.centreOf(start.getBoundingBox());
            data.setCentre(centre);
        }
        double dx = player.getX() - (centre.getX() + 0.5D);
        double dz = player.getZ() - (centre.getZ() + 0.5D);
        double reach = ColosseumShape.ARENA_RADIUS;
        boolean inside = dx * dx + dz * dz <= reach * reach
                && player.getY() >= centre.getY() - 3.0D && player.getY() <= centre.getY() + ColosseumShape.HEIGHT;
        return inside ? centre : null;
    }

    private static void tryAwaken(ServerLevel level, ColosseumData data, BlockPos centre) {
        long now = level.getGameTime();
        Vec3 ground = Vec3.atBottomCenterOf(centre);
        if (data.bossId() != null) {
            if (level.getEntity(data.bossId()) != null || StormRayEntity.anyAliveNear(level, ground, 256.0D)
                    || now - data.awakenedAt() < LOST_AFTER_TICKS) {
                return; // the guardian is still out there
            }
            data.bossLost();
        }
        if (data.defeatedAt() >= 0L) {
            int rest = StormRayConfig.COLOSSEUM_RESPAWN_TICKS.get();
            if (rest <= 0 || now - data.defeatedAt() < rest) {
                return;
            }
        }
        if (!StormRayConfig.ALLOW_MULTIPLE.get()
                && StormRayEntity.anyAliveNear(level, ground, StormRayConfig.MIN_BOSS_DISTANCE.get())) {
            return;
        }
        StormRayEntity ray = ModEntities.STORM_RAY.get().create(level);
        if (ray == null) {
            return;
        }
        ray.moveTo(ground.x, ground.y, ground.z, level.random.nextFloat() * 360.0F, 0.0F);
        ray.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(ray);
        data.bossAwakened(ray.getUUID(), now);

        spawnGuards(level, centre);

        level.playSound(null, ground.x, ground.y, ground.z, ModSounds.RITUAL_IGNITE.get(), SoundSource.HOSTILE, 6.0F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 1.0D, ground.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ModParticles.RITUAL_RUNE.get(), ground.x, ground.y + 1.0D, ground.z, 200, 2.5D, 2.0D, 2.5D, 0.35D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, ground.x, ground.y + 0.5D, ground.z, 80, 3.0D, 0.5D, 3.0D, 0.05D);
        StormEnvironment.spawnVisualLightning(level, ground);
        Component message = Component.translatable("message.stormray.colosseum.awaken")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(ground) < 96.0D * 96.0D) {
                player.displayClientMessage(message, true);
            }
        }
        StormRayMod.LOGGER.info("The colosseum at {} in {} wakes", centre, level.dimension().location());
    }

    /** Phantom X guards appear around the edge of the arena. */
    private static void spawnGuards(ServerLevel level, BlockPos centre) {
        int count = StormRayConfig.COLOSSEUM_GUARDS.get();
        RandomSource random = level.random;
        for (int n = 0; n < count; n++) {
            for (int attempt = 0; attempt < 6; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double distance = 9.0D + random.nextDouble() * 10.0D;
                double x = centre.getX() + 0.5D + Math.cos(angle) * distance;
                double z = centre.getZ() + 0.5D + Math.sin(angle) * distance;
                PhantomXEntity guard = ModEntities.PHANTOM_X.get().create(level);
                if (guard == null) {
                    return;
                }
                guard.moveTo(x, centre.getY(), z, random.nextFloat() * 360.0F, 0.0F);
                if (!level.noCollision(guard)) {
                    continue; // inside rubble or a pillar: try another spot
                }
                guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), MobSpawnType.EVENT, null, null);
                level.addFreshEntity(guard);
                level.sendParticles(ModParticles.SOOT.get(), x, centre.getY() + 1.2D, z, 30, 0.4D, 0.8D, 0.4D, 0.03D);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof StormRayEntity ray) || !(ray.level() instanceof ServerLevel level) || !hasColosseum(level)) {
            return;
        }
        ColosseumData data = ColosseumData.get(level);
        if (ray.getUUID().equals(data.bossId())) {
            data.bossDefeated(level.getGameTime());
        }
    }
}
