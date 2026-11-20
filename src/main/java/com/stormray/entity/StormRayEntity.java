package com.stormray.entity;

import com.stormray.config.DifficultyPreset;
import com.stormray.config.StormRayConfig;
import com.stormray.petrify.PetrificationManager;
import com.stormray.registry.ModEntities;
import com.stormray.registry.ModSounds;
import com.stormray.storm.StormEnvironment;
import com.stormray.util.StormMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Storm Ray world boss.
 *
 * <p>Server authority: movement, targeting, attacks and petrification all run in
 * {@link #customServerAiStep()}. Clients only receive the synced phase and render it
 * (GeckoLib animations, particles, sky effects).</p>
 */
public class StormRayEntity extends Monster implements GeoEntity {
    public static final String BOSS_BAR_KEY = "boss.stormray.storm_ray";
    /** Body centre height above the entity origin (model is rendered 3x; body sits ~50px up). */
    public static final double BODY_OFFSET = 9.4D;
    public static final int DEATH_TICKS = 100;
    private static final int PULSE_TICKS = 30;
    private static final int SWOOP_MAX_TICKS = 100;
    /** Eye lasers: a short aiming glare, then the beams burn across the ground. */
    public static final int LASER_WARMUP_TICKS = 20;
    private static final int LASER_TICKS = 90;
    private static final double LASER_SWEEP_SPEED = 0.3D;
    private static final double LASER_RANGE = 64.0D;
    private static final String CONTROLLER = "main";

    public enum Phase {
        SUMMONING, HOVER, SWOOP, CHARGING, PULSE, LASER;

        public static Phase byId(int id) {
            Phase[] values = values();
            return id >= 0 && id < values.length ? values[id] : HOVER;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE_LENGTH =
            SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> DATA_LASER_X =
            SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LASER_Y =
            SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LASER_Z =
            SynchedEntityData.defineId(StormRayEntity.class, EntityDataSerializers.FLOAT);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.storm_ray.idle");
    private static final RawAnimation ANIM_SUMMON = RawAnimation.begin()
            .thenPlay("animation.storm_ray.summon").thenLoop("animation.storm_ray.idle");
    private static final RawAnimation ANIM_SWOOP = RawAnimation.begin().thenLoop("animation.storm_ray.swoop");
    private static final RawAnimation ANIM_CHARGE = RawAnimation.begin().thenLoop("animation.storm_ray.charge");
    private static final RawAnimation ANIM_RELEASE = RawAnimation.begin()
            .thenPlay("animation.storm_ray.release").thenLoop("animation.storm_ray.idle");
    private static final RawAnimation ANIM_LASH = RawAnimation.begin().thenPlay("animation.storm_ray.lash");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("animation.storm_ray.death");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable(BOSS_BAR_KEY), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10)
            .setDarkenScreen(true)
            .setCreateWorldFog(true);
    private final Set<UUID> environmentPlayers = new HashSet<>();

    @Nullable
    private Vec3 anchor;
    private int phaseTicks;
    private int clientPhaseTicks;
    private int petrifyCooldown = 160;
    private int tentacleCooldown = 60;
    private int lashDelay = -1;
    private float orbitAngle;
    private float damageTakenScale = 1.0F;
    private int dyingTicks;
    /** False while a colosseum guardian is still holding back its petrification pulse. */
    private boolean petrifyEnabled = true;
    /** Where the eye lasers are being dragged to on the ground (server side). */
    private Vec3 laserAim = Vec3.ZERO;

    public StormRayEntity(EntityType<? extends StormRayEntity> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.noCulling = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.orbitAngle = this.random.nextFloat() * Mth.TWO_PI;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0D)
                .add(Attributes.ARMOR, 14.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FLYING_SPEED, 0.6D);
    }

    // ------------------------------------------------------------------ setup

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, Phase.HOVER.ordinal());
        this.entityData.define(DATA_PHASE_LENGTH, 0);
        this.entityData.define(DATA_LASER_X, 0.0F);
        this.entityData.define(DATA_LASER_Y, 0.0F);
        this.entityData.define(DATA_LASER_Z, 0.0F);
    }

    @Override
    protected void registerGoals() {
        // Players are found by acquireTarget(): vanilla target goals only search +-4 blocks vertically.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, StormRayEntity.class));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        applyConfiguredStats();
        startSummon(position());
        return data;
    }

    /** Reads health/armor/damage from the config and difficulty preset. */
    public void applyConfiguredStats() {
        DifficultyPreset preset = preset();
        double[] health = StormMath.capHealth(StormRayConfig.BOSS_HEALTH.get() * preset.healthMultiplier(), 1024.0D);
        this.damageTakenScale = (float) health[1];
        setBase(Attributes.MAX_HEALTH, health[0]);
        setBase(Attributes.ARMOR, StormRayConfig.BOSS_ARMOR.get());
        setBase(Attributes.ATTACK_DAMAGE, StormRayConfig.TENTACLE_DAMAGE.get() * preset.damageMultiplier());
        setBase(Attributes.FOLLOW_RANGE, Math.min(2048.0D, StormRayConfig.DETECTION_RADIUS.get()));
        this.setHealth(this.getMaxHealth());
    }

    private void setBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static DifficultyPreset preset() {
        return StormRayConfig.DIFFICULTY.get();
    }

    /** Starts the emergence sequence at the given ground position. */
    public void startSummon(Vec3 groundPos) {
        this.anchor = groundPos;
        this.setPos(groundPos.x, groundPos.y - 8.0D, groundPos.z);
        setPhase(Phase.SUMMONING, StormRayConfig.SUMMON_DURATION.get());
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        this.noPhysics = true; // a sky creature this size ignores terrain collision
        super.tick();
        this.noPhysics = false;
        this.setNoGravity(true);
        if (this.level().isClientSide) {
            this.clientPhaseTicks++;
            StormRayEffects.clientTick(this);
        } else {
            updateBossBar();
            StormEnvironment.tick(this);
        }
    }

    /**
     * The boss bar. Kept here, on its own, and run every single tick: it must not depend on the
     * phase, on the red sky, or on anything that can be switched off, and it has to keep working
     * while the boss is rising out of the ground and while it is dying.
     */
    private void updateBossBar() {
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double radius = StormRayConfig.BOSS_BAR_RADIUS.get();
        double leave = radius + 24.0D;        // hysteresis, so the edge cannot make it flicker
        for (ServerPlayer player : level.players()) {
            boolean shown = this.bossEvent.getPlayers().contains(player);
            double limit = shown ? leave : radius;
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            if (dx * dx + dz * dz <= limit * limit) {
                this.bossEvent.addPlayer(player);      // a no-op if they already have it
            } else if (shown) {
                this.bossEvent.removePlayer(player);
            }
        }
        // players who left this level, logged out, or respawned into a new entity object
        for (ServerPlayer player : List.copyOf(this.bossEvent.getPlayers())) {
            if (player.isRemoved() || player.level() != level) {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.anchor == null) {
            this.anchor = position();
        }
        this.phaseTicks++;
        if (this.lashDelay > 0 && --this.lashDelay == 0) {
            applyLashDamage();
        }
        if (this.tickCount % 20 == 0) {
            acquireTarget();
        }
        LivingEntity target = validTarget();
        switch (getPhase()) {
            case SUMMONING -> tickSummoning();
            case HOVER -> tickHover(target);
            case SWOOP -> tickSwoop(target);
            case CHARGING -> tickCharging();
            case PULSE -> tickPulse();
            case LASER -> tickLaser(target);
        }
    }

    private void acquireTarget() {
        if (getPhase() == Phase.SUMMONING || validTarget() != null) {
            return;
        }
        double range = StormRayConfig.DETECTION_RADIUS.get();
        TargetingConditions conditions = TargetingConditions.forCombat().range(range).ignoreLineOfSight().ignoreInvisibilityTesting();
        Player player = this.level().getNearestPlayer(conditions, this, this.getX(), this.getY(), this.getZ());
        if (player != null && !PetrificationManager.isPetrified(player)) {
            this.setTarget(player);
        }
    }

    @Nullable
    private LivingEntity validTarget() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            return null;
        }
        double range = StormRayConfig.DETECTION_RADIUS.get();
        boolean invalid = !target.isAlive()
                || target.isSpectator()
                || (target instanceof Player player && player.isCreative())
                || target.level() != this.level()
                || this.distanceToSqr(target) > range * range
                || PetrificationManager.isPetrified(target);
        if (invalid) {
            this.setTarget(null);
            return null;
        }
        return target;
    }

    private void tickSummoning() {
        int duration = Math.max(1, getPhaseLength());
        float progress = StormMath.easeInOut(this.phaseTicks / (float) duration);
        Vec3 base = this.anchor != null ? this.anchor : position();
        double startY = base.y - 8.0D;
        double endY = base.y + StormRayConfig.HOVER_HEIGHT.get();
        Vec3 goal = new Vec3(base.x, Mth.lerp(progress, startY, endY), base.z);
        this.setDeltaMovement(goal.subtract(position()));
        float yaw = this.getYRot() + 0.8F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        if (this.phaseTicks == 1) {
            playBossSound(ModSounds.SUMMON.get(), 10.0F, 1.0F);
        }
        if (this.phaseTicks == duration / 2 && this.level() instanceof ServerLevel serverLevel) {
            playBossSound(ModSounds.AMBIENT.get(), 10.0F, 0.6F);
            StormEnvironment.spawnVisualLightning(serverLevel, base);
            StormEnvironment.flash(this, 1.0F);
        }
        if (this.phaseTicks >= duration) {
            setPhase(Phase.HOVER, 0);
            this.petrifyCooldown = 160;
            this.tentacleCooldown = 60;
            playBossSound(ModSounds.AMBIENT.get(), 10.0F, 0.8F);
        }
    }

    private void tickHover(@Nullable LivingEntity target) {
        this.tentacleCooldown--;
        this.petrifyCooldown--;
        double hover = StormRayConfig.HOVER_HEIGHT.get();
        Vec3 center;
        double radius;
        if (target != null) {
            center = target.position();
            radius = 22.0D;
            this.orbitAngle += 0.016F;
        } else {
            center = this.anchor != null ? this.anchor : position();
            radius = 26.0D;
            this.orbitAngle += 0.009F;
        }
        double gx = center.x + Math.cos(this.orbitAngle) * radius;
        double gz = center.z + Math.sin(this.orbitAngle) * radius;
        double gy = Math.max(center.y + hover, groundHeight() + 6.0D);
        moveToward(new Vec3(gx, gy, gz), 0.55D);
        faceDirection(this.getDeltaMovement());

        if (target == null) {
            return;
        }
        double horizontalSq = horizontalDistanceSqr(target);
        double petrifyRange = StormRayConfig.PETRIFY_RADIUS.get() * 1.6D;
        if (this.petrifyEnabled && this.petrifyCooldown <= 0 && horizontalSq < petrifyRange * petrifyRange) {
            startCharging();
        } else if (this.tentacleCooldown <= 0 && horizontalSq < 48.0D * 48.0D) {
            // Most attacks are the tentacle swoop; now and then it burns the ground with its eyes instead.
            if (this.random.nextDouble() < StormRayConfig.LASER_CHANCE.get()) {
                startLaser(target);
            } else {
                setPhase(Phase.SWOOP, SWOOP_MAX_TICKS);
                playBossSound(ModSounds.TENTACLE.get(), 5.0F, 0.7F);
            }
        }
    }

    private void tickSwoop(@Nullable LivingEntity target) {
        this.petrifyCooldown--;
        if (target == null) {
            setPhase(Phase.HOVER, 0);
            return;
        }
        Vec3 goal = target.position().add(0.0D, 1.5D, 0.0D);
        moveToward(goal, 0.95D);
        faceDirection(goal.subtract(position()));
        boolean close = horizontalDistanceSqr(target) < 36.0D && Math.abs(this.getY() - target.getY()) < 6.0D;
        if (close || this.phaseTicks >= getPhaseLength()) {
            this.triggerAnim(CONTROLLER, "lash");
            this.lashDelay = 6;
            playBossSound(ModSounds.TENTACLE.get(), 6.0F, 0.9F + this.random.nextFloat() * 0.2F);
            this.tentacleCooldown = preset().scaleCooldown(StormRayConfig.TENTACLE_COOLDOWN.get());
            setPhase(Phase.HOVER, 0);
        }
    }

    private void startCharging() {
        setPhase(Phase.CHARGING, StormRayConfig.CHARGE_TICKS.get());
        playBossSound(ModSounds.CHARGE.get(), 10.0F, 1.0F);
    }

    private void tickCharging() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.85D).add(0.0D, 0.01D, 0.0D));
        if (this.phaseTicks % 20 == 0) {
            StormEnvironment.flash(this, 0.3F);
        }
        if (this.phaseTicks >= getPhaseLength()) {
            releasePulse();
        }
    }

    private void releasePulse() {
        setPhase(Phase.PULSE, PULSE_TICKS);
        playBossSound(ModSounds.PULSE.get(), 12.0F, 0.8F);
        PetrificationManager.petrifyArea(this, StormRayConfig.PETRIFY_RADIUS.get());
        StormEnvironment.flash(this, 1.0F);
        this.petrifyCooldown = preset().scaleCooldown(StormRayConfig.COOLDOWN_TICKS.get());
    }

    private void tickPulse() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        if (this.phaseTicks >= getPhaseLength()) {
            setPhase(Phase.HOVER, 0);
        }
    }

    // ------------------------------------------------------------------ eye lasers

    private void startLaser(LivingEntity target) {
        // Start short of the target, on the boss's side, so the beams visibly sweep towards it.
        Vec3 toBoss = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 offset = toBoss.lengthSqr() > 1.0E-4D ? toBoss.normalize().scale(7.0D) : Vec3.ZERO;
        this.laserAim = target.position().add(offset);
        syncLaserEnd(traceLaser());
        setPhase(Phase.LASER, LASER_TICKS);
        playBossSound(ModSounds.CHARGE.get(), 8.0F, 1.7F);
    }

    private void tickLaser(@Nullable LivingEntity target) {
        this.petrifyCooldown--;
        this.setDeltaMovement(this.getDeltaMovement().scale(0.88D));
        if (target != null) {
            Vec3 delta = target.position().subtract(this.laserAim);
            double distance = delta.length();
            if (distance > LASER_SWEEP_SPEED) {
                delta = delta.scale(LASER_SWEEP_SPEED / distance);
            }
            this.laserAim = this.laserAim.add(delta);
        }
        faceDirection(this.laserAim.subtract(position()));
        Vec3 end = traceLaser();
        syncLaserEnd(end);

        if (this.phaseTicks >= LASER_WARMUP_TICKS && this.level() instanceof ServerLevel serverLevel) {
            if (this.phaseTicks == LASER_WARMUP_TICKS) {
                playBossSound(SoundEvents.BLAZE_SHOOT, 8.0F, 0.5F);
                StormEnvironment.flash(this, 0.4F);
            }
            if (this.phaseTicks % 2 == 0) {
                burnAt(serverLevel, end);
            }
            if (this.phaseTicks % 8 == 0) {
                serverLevel.playSound(null, end.x, end.y, end.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 3.0F,
                        0.6F + this.random.nextFloat() * 0.3F);
            }
        }
        if (this.phaseTicks >= getPhaseLength() || target == null) {
            this.tentacleCooldown = preset().scaleCooldown(StormRayConfig.TENTACLE_COOLDOWN.get());
            setPhase(Phase.HOVER, 0);
        }
    }

    /** Where the beams end: the first block between the eyes and the aim point. */
    private Vec3 traceLaser() {
        Vec3 from = getEyeCenter();
        Vec3 direction = this.laserAim.add(0.0D, 0.1D, 0.0D).subtract(from);
        if (direction.lengthSqr() < 1.0E-4D) {
            return this.laserAim;
        }
        Vec3 to = from.add(direction.normalize().scale(LASER_RANGE));
        BlockHitResult hit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        return hit.getLocation();
    }

    private void syncLaserEnd(Vec3 end) {
        this.entityData.set(DATA_LASER_X, (float) end.x);
        this.entityData.set(DATA_LASER_Y, (float) end.y);
        this.entityData.set(DATA_LASER_Z, (float) end.z);
    }

    /** Scorches whoever stands where the beams land and sets the ground there alight. */
    private void burnAt(ServerLevel level, Vec3 end) {
        float damage = (float) (StormRayConfig.LASER_DAMAGE.get() * preset().damageMultiplier());
        AABB area = new AABB(end, end).inflate(1.8D, 2.2D, 1.8D);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, this::canLash)) {
            victim.setSecondsOnFire(5);
            victim.hurt(this.damageSources().mobAttack(this), damage);
        }
        if (!StormRayConfig.LASER_FIRE.get() || !ForgeEventFactory.getMobGriefingEvent(level, this)) {
            return;
        }
        BlockPos centre = BlockPos.containing(end.x, end.y + 0.3D, end.z);
        for (int n = 0; n < 3; n++) {
            // the first flame lands exactly under the beam, the others scatter around it
            int spread = n == 0 ? 0 : 2;
            BlockPos pos = centre.offset(spread == 0 ? 0 : this.random.nextInt(5) - 2, 0, spread == 0 ? 0 : this.random.nextInt(5) - 2);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos candidate = pos.above(dy);
                if (level.getBlockState(candidate).isAir() && BaseFireBlock.canBePlacedAt(level, candidate, Direction.UP)) {
                    level.setBlockAndUpdate(candidate, BaseFireBlock.getState(level, candidate));
                    break;
                }
            }
        }
    }

    /** Point between the eyes, at the front of the head. */
    public Vec3 getEyeCenter() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return getBodyCenter().add(-Mth.sin(yaw) * 6.5D, -0.6D, Mth.cos(yaw) * 6.5D);
    }

    /** One of the two eyes: side -1 (left) or +1 (right). */
    public Vec3 getEyePosition(int side) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return getEyeCenter().add(Mth.cos(yaw) * 1.7D * side, 0.0D, Mth.sin(yaw) * 1.7D * side);
    }

    /** Where the eye lasers currently end (synced to clients). */
    public Vec3 getLaserEnd() {
        return new Vec3(this.entityData.get(DATA_LASER_X), this.entityData.get(DATA_LASER_Y), this.entityData.get(DATA_LASER_Z));
    }

    /** True once the aiming glare is over and the beams are burning. */
    public boolean isLaserFiring() {
        return getPhase() == Phase.LASER && getPhaseTicks() >= LASER_WARMUP_TICKS;
    }

    // ------------------------------------------------------------------ petrification gate

    public boolean isPetrifyEnabled() {
        return this.petrifyEnabled;
    }

    /** The colosseum keeps its guardian from petrifying until a later wave. */
    public void setPetrifyEnabled(boolean enabled) {
        if (enabled && !this.petrifyEnabled) {
            this.petrifyCooldown = Math.min(this.petrifyCooldown, 100); // the first pulse follows soon after
        }
        this.petrifyEnabled = enabled;
    }

    private void applyLashDamage() {
        double reach = StormRayConfig.TENTACLE_REACH.get();
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 origin = position().add(0.0D, 2.0D, 0.0D);
        AABB area = new AABB(origin, origin).inflate(reach, reach * 0.75D + 4.0D, reach);
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area, this::canLash)) {
            if (victim.position().distanceToSqr(origin) > reach * reach * 1.5D) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 push = victim.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                victim.push(push.x * 1.6D, 0.6D, push.z * 1.6D);
                victim.hurtMarked = true;
            }
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, origin.x, origin.y, origin.z, 14, reach * 0.5D, 1.0D, reach * 0.5D, 0.0D);
        }
    }

    private boolean canLash(LivingEntity entity) {
        if (entity == this || !entity.isAlive() || entity instanceof StormRayEntity) {
            return false;
        }
        if (entity instanceof PhantomXEntity || entity instanceof PhantomSeaEntity || entity instanceof PhantomBatEntity) {
            return false; // the phantoms fight at its side
        }
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return !PetrificationManager.isPetrified(entity);
    }

    // ------------------------------------------------------------------ movement helpers

    private void moveToward(Vec3 goal, double maxSpeed) {
        Vec3 delta = goal.subtract(position());
        double distance = delta.length();
        Vec3 direction = distance > 1.0E-4D ? delta.scale(1.0D / distance) : Vec3.ZERO;
        double accel = Math.min(1.0D, distance / 12.0D) * maxSpeed * 0.09D;
        Vec3 velocity = this.getDeltaMovement().scale(0.93D).add(direction.scale(accel));
        if (velocity.length() > maxSpeed) {
            velocity = velocity.normalize().scale(maxSpeed);
        }
        this.setDeltaMovement(velocity);
    }

    private void faceDirection(Vec3 direction) {
        if (direction.horizontalDistanceSqr() < 1.0E-4D) {
            return;
        }
        float targetYaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.getYRot(), targetYaw, 3.0F);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private double groundHeight() {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(this.getX()), Mth.floor(this.getZ()));
    }

    private double horizontalDistanceSqr(LivingEntity other) {
        double dx = other.getX() - this.getX();
        double dz = other.getZ() - this.getZ();
        return dx * dx + dz * dz;
    }

    private void playBossSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY() + BODY_OFFSET, this.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    // ------------------------------------------------------------------ phases & sync

    private void setPhase(Phase phase, int length) {
        this.phaseTicks = 0;
        this.entityData.set(DATA_PHASE_LENGTH, length);
        this.entityData.set(DATA_PHASE, phase.ordinal());
    }

    public Phase getPhase() {
        return Phase.byId(this.entityData.get(DATA_PHASE));
    }

    public int getPhaseLength() {
        return this.entityData.get(DATA_PHASE_LENGTH);
    }

    /** Ticks spent in the current phase, on whichever side this is called. */
    public int getPhaseTicks() {
        return this.level().isClientSide ? this.clientPhaseTicks : this.phaseTicks;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PHASE.equals(key)) {
            this.clientPhaseTicks = 0;
        }
    }

    /** 0..1 progress of the petrification charge, 0 outside the charging phase. */
    public float chargeProgress(float partialTick) {
        if (getPhase() != Phase.CHARGING) {
            return 0.0F;
        }
        return StormMath.clamp01((getPhaseTicks() + partialTick) / Math.max(1, getPhaseLength()));
    }

    /** 0..1 progress of the summoning sequence, 1 once finished. */
    public float summonProgress(float partialTick) {
        if (getPhase() != Phase.SUMMONING) {
            return 1.0F;
        }
        return StormMath.clamp01((getPhaseTicks() + partialTick) / Math.max(1, getPhaseLength()));
    }

    /** How strongly the red sky should apply: ramps in while summoning, fades out while dying. */
    public float environmentStrength() {
        float strength = StormMath.clamp01(summonProgress(0.0F) * 1.6F);
        if (this.isDeadOrDying()) {
            strength *= 1.0F - StormMath.clamp01(this.dyingTicks / (float) DEATH_TICKS);
        }
        return strength;
    }

    public int getDyingTicks() {
        return this.dyingTicks;
    }

    public Vec3 getBodyCenter() {
        return position().add(0.0D, BODY_OFFSET, 0.0D);
    }

    public ServerBossEvent getBossEvent() {
        return this.bossEvent;
    }

    public Set<UUID> environmentPlayers() {
        return this.environmentPlayers;
    }

    public static boolean anyAliveNear(ServerLevel level, Vec3 center, double distance) {
        double distanceSq = distance * distance;
        return !level.getEntities(ModEntities.STORM_RAY.get(),
                ray -> ray.isAlive() && ray.position().distanceToSqr(center) <= distanceSq).isEmpty();
    }

    // ------------------------------------------------------------------ damage & death

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (getPhase() == Phase.SUMMONING && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof StormRayEntity || source.is(DamageTypeTags.IS_LIGHTNING)) {
            return false;
        }
        return super.hurt(source, amount * this.damageTakenScale);
    }

    @Override
    protected void tickDeath() {
        this.dyingTicks++;
        if (this.level().isClientSide) {
            return;
        }
        this.setDeltaMovement(Vec3.ZERO);
        this.move(MoverType.SELF, new Vec3(0.0D, -0.06D, 0.0D));
        if (this.dyingTicks % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
            Vec3 c = getBodyCenter();
            StormEnvironment.spawnVisualLightning(serverLevel, new Vec3(
                    c.x + (this.random.nextDouble() - 0.5D) * 24.0D, groundHeight(), c.z + (this.random.nextDouble() - 0.5D) * 24.0D));
            StormEnvironment.flash(this, 0.7F);
        }
        if (this.dyingTicks >= DEATH_TICKS && !this.isRemoved()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                Vec3 c = getBodyCenter();
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 4.0D, 2.0D, 4.0D, 0.0D);
            }
            playBossSound(ModSounds.PULSE.get(), 10.0F, 0.5F);
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!this.level().isClientSide) {
            StormEnvironment.release(this);
        }
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 6.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // ------------------------------------------------------------------ vanilla behaviour overrides

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 640.0D * 640.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(40.0D, 12.0D, 40.0D);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RayPhase", getPhase().ordinal());
        tag.putInt("RayPhaseTicks", this.phaseTicks);
        tag.putInt("RayPhaseLength", getPhaseLength());
        tag.putInt("PetrifyCooldown", this.petrifyCooldown);
        tag.putInt("TentacleCooldown", this.tentacleCooldown);
        tag.putFloat("DamageTakenScale", this.damageTakenScale);
        tag.putBoolean("PetrifyEnabled", this.petrifyEnabled);
        if (this.anchor != null) {
            tag.putDouble("AnchorX", this.anchor.x);
            tag.putDouble("AnchorY", this.anchor.y);
            tag.putDouble("AnchorZ", this.anchor.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Phase phase = Phase.byId(tag.getInt("RayPhase"));
        if (phase == Phase.CHARGING || phase == Phase.PULSE || phase == Phase.SWOOP || phase == Phase.LASER) {
            phase = Phase.HOVER;
        }
        setPhase(phase, tag.getInt("RayPhaseLength"));
        this.phaseTicks = tag.getInt("RayPhaseTicks");
        this.petrifyCooldown = tag.getInt("PetrifyCooldown");
        this.tentacleCooldown = tag.getInt("TentacleCooldown");
        this.petrifyEnabled = !tag.contains("PetrifyEnabled") || tag.getBoolean("PetrifyEnabled");
        if (tag.contains("DamageTakenScale")) {
            this.damageTakenScale = tag.getFloat("DamageTakenScale");
        }
        if (tag.contains("AnchorX")) {
            this.anchor = new Vec3(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
        }
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 8, this::animationPredicate)
                .triggerableAnim("lash", ANIM_LASH));
    }

    private PlayState animationPredicate(AnimationState<StormRayEntity> state) {
        if (this.isDeadOrDying()) {
            return state.setAndContinue(ANIM_DEATH);
        }
        return switch (getPhase()) {
            case SUMMONING -> state.setAndContinue(ANIM_SUMMON);
            case SWOOP -> state.setAndContinue(ANIM_SWOOP);
            case CHARGING, LASER -> state.setAndContinue(ANIM_CHARGE);
            case PULSE -> state.setAndContinue(ANIM_RELEASE);
            default -> state.setAndContinue(ANIM_IDLE);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
