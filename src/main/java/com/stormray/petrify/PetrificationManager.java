package com.stormray.petrify;

import com.stormray.config.StormRayConfig;
import com.stormray.entity.StormRayEntity;
import com.stormray.network.ModNetwork;
import com.stormray.network.PetrifyPacket;
import com.stormray.registry.ModDamageTypes;
import com.stormray.registry.ModEffects;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative petrification. Entities are frozen (movement attribute zeroed, mobs
 * get NoAI, players are held in place), surrounded by stone particles, and when the timer runs
 * out they die (or take configured damage). No blocks are placed and no inventories are touched.
 */
public final class PetrificationManager {
    public static final String TAG_PREV_NO_AI = "stormray_petrify_prev_noai";
    private static final UUID SPEED_MODIFIER = UUID.fromString("5f3e1b7a-6c2d-4f7e-9a51-2b8d0c4e7a10");
    private static final UUID FLY_MODIFIER = UUID.fromString("9a7c2e41-3b5d-4c8f-8e16-7d0a5b3c1f22");

    private static final class State {
        final ResourceKey<Level> dimension;
        final int total;
        final Vec3 frozenPos;
        final boolean prevNoAi;
        final UUID source;
        int elapsed;

        State(ResourceKey<Level> dimension, int total, Vec3 frozenPos, boolean prevNoAi, UUID source) {
            this.dimension = dimension;
            this.total = total;
            this.frozenPos = frozenPos;
            this.prevNoAi = prevNoAi;
            this.source = source;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private PetrificationManager() {
    }

    /** Works on both sides: the server checks its own state, the client checks synced data. */
    public static boolean isPetrified(Entity entity) {
        if (entity.level().isClientSide) {
            return PetrifyClientData.isPetrified(entity.getId());
        }
        return STATES.containsKey(entity.getUUID());
    }

    /** Petrifies everything in a cylinder under and around the boss. Returns the number of victims. */
    public static int petrifyArea(StormRayEntity source, double radius) {
        if (!(source.level() instanceof ServerLevel level)) {
            return 0;
        }
        Vec3 center = source.getBodyCenter();
        double below = radius + StormRayConfig.HOVER_HEIGHT.get() + StormRayEntity.BODY_OFFSET;
        AABB box = new AABB(center.x - radius, center.y - below, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        int duration = StormRayConfig.PETRIFY_TICKS.get();
        int count = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box, e -> canPetrify(e, source))) {
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (isStoneBreaker(entity)) {
                resist(level, entity); // the pulse shatters harmlessly against Stone Breaker
                continue;
            }
            petrify(entity, source, duration);
            count++;
        }
        return count;
    }

    private static boolean canPetrify(LivingEntity entity, StormRayEntity source) {
        if (entity == source || !entity.isAlive() || entity instanceof StormRayEntity || entity instanceof ArmorStand) {
            return false;
        }
        if (entity.getType().is(Tags.EntityTypes.BOSSES) || STATES.containsKey(entity.getUUID())) {
            return false;
        }
        if (entity instanceof Player player) {
            if (!StormRayConfig.AFFECT_PLAYERS.get() || player.isSpectator()) {
                return false;
            }
            return StormRayConfig.AFFECT_CREATIVE.get() || !player.isCreative();
        }
        return StormRayConfig.AFFECT_MOBS.get();
    }

    /** True while the Stone Breaker effect protects this entity from petrification. */
    public static boolean isStoneBreaker(LivingEntity entity) {
        return entity.hasEffect(ModEffects.STONE_BREAKER.get());
    }

    /** Feedback for a protected entity caught in the pulse: the stone cracks and falls away. */
    private static void resist(ServerLevel level, LivingEntity entity) {
        double h = entity.getBbHeight();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                entity.getX(), entity.getY() + h * 0.5D, entity.getZ(), 30, entity.getBbWidth() * 0.5D, h * 0.4D,
                entity.getBbWidth() * 0.5D, 0.12D);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.6F, 0.6F);
    }

    /** Ends a petrification early without harm (the victim drank Stone Breaker in time). */
    private static void release(ServerLevel level, LivingEntity entity, State state) {
        removeModifiers(entity);
        if (entity instanceof Mob mob) {
            restore(mob, state);
        }
        resist(level, entity);
        ModNetwork.sendTracking(entity, new PetrifyPacket(entity.getId(), 0, 0, false));
    }

    public static void petrify(LivingEntity entity, StormRayEntity source, int duration) {
        if (isStoneBreaker(entity)) {
            return;
        }
        boolean prevNoAi = false;
        if (entity instanceof Mob mob) {
            prevNoAi = mob.isNoAi();
            mob.getPersistentData().putBoolean(TAG_PREV_NO_AI, prevNoAi);
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        addModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        addModifier(entity, Attributes.FLYING_SPEED, FLY_MODIFIER);
        entity.stopUsingItem();
        entity.setSprinting(false);
        if (entity instanceof ServerPlayer player) {
            player.stopFallFlying();
        }
        STATES.put(entity.getUUID(), new State(entity.level().dimension(), Math.max(1, duration), entity.position(),
                prevNoAi, source.getUUID()));
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.PETRIFY.get(),
                SoundSource.HOSTILE, 1.5F, 1.2F);
        ModNetwork.sendTracking(entity, new PetrifyPacket(entity.getId(), 0, duration, false));
    }

    /** Ticked once per server tick from {@link PetrifyEvents}. */
    public static void tick() {
        if (STATES.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            STATES.clear();
            return;
        }
        Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> mapEntry = it.next();
            State state = mapEntry.getValue();
            ServerLevel level = server.getLevel(state.dimension);
            Entity raw = level == null ? null : level.getEntity(mapEntry.getKey());
            if (!(raw instanceof LivingEntity entity) || !entity.isAlive()) {
                if (raw instanceof Mob mob) {
                    restore(mob, state);
                }
                it.remove();
                continue;
            }
            if (isStoneBreaker(entity)) {
                it.remove();
                release(level, entity, state);
                continue;
            }
            state.elapsed++;
            holdInPlace(entity, state);
            stoneEffects(level, entity, state);
            if (state.elapsed % 5 == 0) {
                ModNetwork.sendTracking(entity, new PetrifyPacket(entity.getId(), state.elapsed, state.total, false));
            }
            if (state.elapsed >= state.total) {
                it.remove();
                resolve(level, entity, state);
            }
        }
    }

    private static void holdInPlace(LivingEntity entity, State state) {
        Vec3 frozen = state.frozenPos;
        if (entity instanceof ServerPlayer player) {
            if (player.position().distanceToSqr(frozen) > 0.04D) {
                player.connection.teleport(frozen.x, frozen.y, frozen.z, player.getYRot(), player.getXRot());
            }
            player.setDeltaMovement(Vec3.ZERO);
        } else {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(0.0D, Math.min(0.0D, motion.y), 0.0D);
            double dx = entity.getX() - frozen.x;
            double dz = entity.getZ() - frozen.z;
            if (dx * dx + dz * dz > 0.01D) {
                entity.setPos(frozen.x, entity.getY(), frozen.z);
            }
        }
    }

    private static void stoneEffects(ServerLevel level, LivingEntity entity, State state) {
        double w = entity.getBbWidth() * 0.45D;
        double h = entity.getBbHeight();
        if (state.elapsed % 3 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                    entity.getX(), entity.getY() + h * 0.5D, entity.getZ(), 4, w, h * 0.3D, w, 0.05D);
            level.sendParticles(ModParticles.PETRIFY_DUST.get(), entity.getX(), entity.getY() + h * 0.5D, entity.getZ(),
                    3, w, h * 0.35D, w, 0.01D);
        }
        if (state.elapsed % 15 == 0) {
            float progress = state.elapsed / (float) state.total;
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.PETRIFY.get(), SoundSource.HOSTILE,
                    1.0F, 0.8F + progress * 0.6F);
        }
    }

    private static void resolve(ServerLevel level, LivingEntity entity, State state) {
        double h = entity.getBbHeight();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                entity.getX(), entity.getY() + h * 0.5D, entity.getZ(), 40, entity.getBbWidth() * 0.5D, h * 0.4D,
                entity.getBbWidth() * 0.5D, 0.15D);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.PETRIFY.get(), SoundSource.HOSTILE, 2.0F, 0.55F);

        Entity attacker = level.getEntity(state.source);
        float damage = StormRayConfig.INSTANT_KILL.get()
                ? Float.MAX_VALUE
                : (float) (StormRayConfig.PETRIFY_DAMAGE.get() * StormRayConfig.DIFFICULTY.get().damageMultiplier());
        entity.hurt(ModDamageTypes.petrification(level, attacker), damage);

        boolean died = !entity.isAlive();
        removeModifiers(entity);
        if (entity instanceof Mob mob) {
            restore(mob, state);
        }
        // Dead entities stay stone-coloured while they fall over; survivors are cleared right away.
        ModNetwork.sendTracking(entity, died
                ? new PetrifyPacket(entity.getId(), state.total, state.total, true)
                : new PetrifyPacket(entity.getId(), 0, 0, false));
    }

    private static void restore(Mob mob, State state) {
        mob.setNoAi(state.prevNoAi);
        mob.getPersistentData().remove(TAG_PREV_NO_AI);
        removeModifiers(mob);
    }

    /** Repairs mobs saved mid-petrification (e.g. server stopped during the effect). */
    public static void restoreIfOrphaned(Mob mob) {
        if (mob.getPersistentData().contains(TAG_PREV_NO_AI) && !STATES.containsKey(mob.getUUID())) {
            mob.setNoAi(mob.getPersistentData().getBoolean(TAG_PREV_NO_AI));
            mob.getPersistentData().remove(TAG_PREV_NO_AI);
            removeModifiers(mob);
        }
    }

    private static void addModifier(LivingEntity entity, Attribute attribute, UUID id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && instance.getModifier(id) == null) {
            instance.addTransientModifier(new AttributeModifier(id, "stormray petrification", -1.0D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeModifiers(LivingEntity entity) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER);
        }
        AttributeInstance fly = entity.getAttribute(Attributes.FLYING_SPEED);
        if (fly != null) {
            fly.removeModifier(FLY_MODIFIER);
        }
    }

    public static void clear() {
        STATES.clear();
    }
}
