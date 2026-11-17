package com.stormray.structure;

import com.stormray.StormRayMod;
import com.stormray.config.StormRayConfig;
import com.stormray.dimension.ModDimensions;
import com.stormray.entity.PhantomBatEntity;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The colosseum fight. When a survival player walks onto the arena floor, the Storm Ray
 * tears out of the sigil in the middle and a first wave of Phantom X steps out of the ruins.
 * While the boss lives and players stay near, further and larger waves keep coming. After the
 * boss dies the arena sleeps for a configurable time before it can wake again.
 *
 * <p>The arena remembers its guardian by id and only forgets it when the guardian dies or is
 * destroyed. A guardian that is merely unloaded (everyone walked away) is still the guardian,
 * so coming back never summons a second one.</p>
 */
@Mod.EventBusSubscriber(modid = StormRayMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ColosseumEvents {
    /** Players this close to the centre (horizontally) keep the waves coming. */
    private static final double FIGHT_RADIUS = 52.0D;

    private ColosseumEvents() {
    }

    private static boolean hasColosseum(ServerLevel level) {
        return ModDimensions.isMirror(level);
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
        BlockPos centre = data.centre();
        if (centre == null) {
            centre = discover(level, data);
            if (centre == null) {
                return;
            }
        }
        boolean onArena = false;
        boolean nearby = false;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
                continue;
            }
            double dx = player.getX() - (centre.getX() + 0.5D);
            double dz = player.getZ() - (centre.getZ() + 0.5D);
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq <= FIGHT_RADIUS * FIGHT_RADIUS) {
                nearby = true;
            }
            if (distanceSq <= ColosseumShape.ARENA_RADIUS * ColosseumShape.ARENA_RADIUS
                    && player.getY() >= centre.getY() - 3.0D && player.getY() <= centre.getY() + ColosseumShape.HEIGHT) {
                onArena = true;
            }
        }
        if (data.bossId() != null) {
            // A guardian already exists (loaded or not): never summon another, only send its waves.
            if (nearby) {
                tickWaves(level, data, centre);
            }
            return;
        }
        if (onArena) {
            tryAwaken(level, data, centre);
        }
    }

    /** Finds the colosseum the first time a player walks into it and remembers its centre. */
    private static BlockPos discover(ServerLevel level, ColosseumData data) {
        for (ServerPlayer player : level.players()) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), ModStructures.COLOSSEUM_KEY);
            if (start.isValid()) {
                BlockPos centre = ColosseumPiece.centreOf(start.getBoundingBox());
                data.setCentre(centre);
                return centre;
            }
        }
        return null;
    }

    private static void tryAwaken(ServerLevel level, ColosseumData data, BlockPos centre) {
        long now = level.getGameTime();
        Vec3 ground = Vec3.atBottomCenterOf(centre);
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
        ray.setPetrifyEnabled(StormRayConfig.COLOSSEUM_PETRIFY_WAVE.get() <= 1);
        // The guardian has an army, so it gets less health than a ritual Storm Ray.
        AttributeInstance health = ray.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(Math.max(1.0D, health.getBaseValue() * StormRayConfig.COLOSSEUM_BOSS_HEALTH.get()));
            ray.setHealth(ray.getMaxHealth());
        }
        level.addFreshEntity(ray);
        data.bossAwakened(ray.getUUID(), now);

        spawnPhantoms(level, centre, StormRayConfig.COLOSSEUM_GUARDS.get());

        level.playSound(null, ground.x, ground.y, ground.z, ModSounds.RITUAL_IGNITE.get(), SoundSource.HOSTILE, 6.0F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 1.0D, ground.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ModParticles.RITUAL_RUNE.get(), ground.x, ground.y + 1.0D, ground.z, 200, 2.5D, 2.0D, 2.5D, 0.35D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, ground.x, ground.y + 0.5D, ground.z, 80, 3.0D, 0.5D, 3.0D, 0.05D);
        StormEnvironment.spawnVisualLightning(level, ground);
        announce(level, ground, Component.translatable("message.stormray.colosseum.awaken")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        StormRayMod.LOGGER.info("The colosseum at {} in {} wakes", centre, level.dimension().location());
    }

    /**
     * Runs once a second while the guardian exists and players are near: unlocks its
     * petrification at the configured wave and sends the next wave of Phantom X when it is due.
     * Waves grow slowly, and Phantom Bats join them from a later wave on.
     */
    private static void tickWaves(ServerLevel level, ColosseumData data, BlockPos centre) {
        Entity boss = level.getEntity(data.bossId());
        if (!(boss instanceof StormRayEntity ray) || !ray.isAlive()) {
            return; // waves only come while the guardian is actually here
        }
        Vec3 ground = Vec3.atBottomCenterOf(centre);
        boolean petrify = data.wave() >= StormRayConfig.COLOSSEUM_PETRIFY_WAVE.get();
        if (petrify && !ray.isPetrifyEnabled()) {
            announce(level, ground, Component.translatable("message.stormray.colosseum.petrify")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
        ray.setPetrifyEnabled(petrify);

        int interval = StormRayConfig.COLOSSEUM_WAVE_INTERVAL.get();
        long now = level.getGameTime();
        if (interval <= 0 || now - data.lastWaveAt() < interval || ray.getPhase() == StormRayEntity.Phase.SUMMONING) {
            return;
        }
        // Wave sizes grow slowly: the fight should stay winnable in netherite, but never relaxed.
        int next = data.wave() + 1;
        int phantoms = Math.min(StormRayConfig.COLOSSEUM_GUARDS.get() + (next - 1) / 2,
                StormRayConfig.COLOSSEUM_MAX_PHANTOMS.get() - countNear(level, ModEntities.PHANTOM_X.get(), centre));
        int bats = 0;
        int batWave = StormRayConfig.COLOSSEUM_BAT_WAVE.get();
        if (next >= batWave) {
            bats = Math.min(2 + (next - batWave) / 2,
                    StormRayConfig.COLOSSEUM_MAX_BATS.get() - countNear(level, ModEntities.PHANTOM_BAT.get(), centre));
        }
        if (phantoms <= 0 && bats <= 0) {
            return; // the arena is full; the wave comes as soon as some of them fall
        }
        int spawnedPhantoms = spawnPhantoms(level, centre, Math.max(0, phantoms));
        int spawnedBats = spawnBats(level, centre, Math.max(0, bats));
        if (spawnedPhantoms + spawnedBats == 0) {
            return;
        }
        data.waveSent(now);
        StormRayMod.LOGGER.info("Colosseum wave {}: {} Phantom X, {} Phantom Bats", data.wave(), spawnedPhantoms, spawnedBats);
        level.playSound(null, ground.x, ground.y + 2.0D, ground.z, ModSounds.PHANTOM_REVEAL.get(), SoundSource.HOSTILE, 4.0F, 0.6F);
        announce(level, ground, Component.translatable(spawnedBats > 0 ? "message.stormray.colosseum.wave_bats" : "message.stormray.colosseum.wave",
                data.wave()).withStyle(ChatFormatting.RED));
    }

    /** Living mobs of one kind around the arena. */
    private static <T extends Entity> int countNear(ServerLevel level, EntityType<T> type, BlockPos centre) {
        double cx = centre.getX() + 0.5D;
        double cz = centre.getZ() + 0.5D;
        return level.getEntities(type, mob -> {
            double dx = mob.getX() - cx;
            double dz = mob.getZ() - cz;
            return mob.isAlive() && dx * dx + dz * dz <= FIGHT_RADIUS * FIGHT_RADIUS;
        }).size();
    }

    /** Phantom Bats swoop in above the arena. Returns how many appeared. */
    private static int spawnBats(ServerLevel level, BlockPos centre, int count) {
        RandomSource random = level.random;
        int spawned = 0;
        for (int n = 0; n < count; n++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 6.0D + random.nextDouble() * 12.0D;
            double x = centre.getX() + 0.5D + Math.cos(angle) * distance;
            double z = centre.getZ() + 0.5D + Math.sin(angle) * distance;
            double y = centre.getY() + 6.0D + random.nextDouble() * 4.0D;
            PhantomBatEntity bat = ModEntities.PHANTOM_BAT.get().create(level);
            if (bat == null) {
                return spawned;
            }
            bat.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
            bat.finalizeSpawn(level, level.getCurrentDifficultyAt(bat.blockPosition()), MobSpawnType.EVENT, null, null);
            bat.setPersistenceRequired();
            if (!level.addFreshEntity(bat)) {
                continue;
            }
            level.sendParticles(ModParticles.SOOT.get(), x, y + 0.4D, z, 20, 0.3D, 0.3D, 0.3D, 0.03D);
            spawned++;
        }
        return spawned;
    }

    private static void announce(ServerLevel level, Vec3 ground, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(ground) < 96.0D * 96.0D) {
                player.displayClientMessage(message, true);
            }
        }
    }

    /**
     * Phantom X step out onto the arena floor, standing on whatever is there (sand, rubble, a
     * broken pillar). They arrive uncloaked for a few seconds so the wave can be seen coming.
     * Returns how many appeared.
     */
    private static int spawnPhantoms(ServerLevel level, BlockPos centre, int count) {
        RandomSource random = level.random;
        int spawned = 0;
        for (int n = 0; n < count; n++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 8.0D + random.nextDouble() * 11.0D;
            double x = centre.getX() + 0.5D + Math.cos(angle) * distance;
            double z = centre.getZ() + 0.5D + Math.sin(angle) * distance;
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            // never below the arena floor, never up on the stands
            double y = Mth.clamp(surface, centre.getY(), centre.getY() + 10);
            PhantomXEntity phantom = ModEntities.PHANTOM_X.get().create(level);
            if (phantom == null) {
                return spawned;
            }
            phantom.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
            phantom.finalizeSpawn(level, level.getCurrentDifficultyAt(phantom.blockPosition()), MobSpawnType.EVENT, null, null);
            phantom.setPersistenceRequired();
            if (!level.addFreshEntity(phantom)) {
                continue;
            }
            phantom.revealFor(60);
            level.sendParticles(ModParticles.SOOT.get(), x, y + 1.2D, z, 40, 0.4D, 0.8D, 0.4D, 0.03D);
            level.sendParticles(ModParticles.STORM_EMBER.get(), x, y + 1.2D, z, 12, 0.3D, 0.8D, 0.3D, 0.02D);
            spawned++;
        }
        return spawned;
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

    /**
     * The guardian left the world. Unloading with its chunk is not a loss; being destroyed
     * without dying (peaceful mode, a command) is, and frees the arena to wake again.
     */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof StormRayEntity ray) || !(event.getLevel() instanceof ServerLevel level) || !hasColosseum(level)) {
            return;
        }
        Entity.RemovalReason reason = ray.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) {
            return;
        }
        ColosseumData data = ColosseumData.get(level);
        if (ray.getUUID().equals(data.bossId())) {
            if (ray.isDeadOrDying()) {
                data.bossDefeated(level.getGameTime());
            } else {
                data.bossLost();
            }
        }
    }
}
