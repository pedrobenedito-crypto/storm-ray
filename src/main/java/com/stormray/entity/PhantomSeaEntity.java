package com.stormray.entity;

import com.stormray.config.StormRayConfig;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Phantom Sea: a drowned soot wraith with an eel-like tail. It hunts anyone swimming in the
 * Ashen Reflection's oceans, grabs them and drags them down to the sea floor, biting while it
 * holds on. Hitting it hard enough breaks its grip; out of water it slowly dries out.
 */
public class PhantomSeaEntity extends Monster implements GeoEntity {
    private static final String CONTROLLER = "main";
    private static final double GRAB_RANGE = 2.4D;
    private static final int GRAB_COOLDOWN = 80;

    private static final EntityDataAccessor<Boolean> DATA_GRABBING =
            SynchedEntityData.defineId(PhantomSeaEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.phantom_sea.idle");
    private static final RawAnimation ANIM_SWIM = RawAnimation.begin().thenLoop("animation.phantom_sea.swim");
    private static final RawAnimation ANIM_GRAB = RawAnimation.begin().thenLoop("animation.phantom_sea.grab");
    private static final RawAnimation ANIM_LUNGE = RawAnimation.begin().thenPlay("animation.phantom_sea.lunge");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("animation.phantom_sea.death");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID grabbedId;
    private int grabTicks;
    private int grabCooldown = 40;
    private float damageWhileGrabbing;
    private int dryTicks;

    public PhantomSeaEntity(EntityType<? extends PhantomSeaEntity> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.06F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.1D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    /** Natural spawn rule: dark water at least two blocks deep. */
    public static boolean checkSpawnRules(EntityType<PhantomSeaEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                          BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.below()).is(FluidTags.WATER)
                && (reason == MobSpawnType.SPAWNER || Monster.isDarkEnoughToSpawn(level, pos, random));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_GRABBING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PhantomSeaHuntGoal(this));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0D, 10));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target.isInWater()));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(StormRayConfig.SEA_HEALTH.get());
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(StormRayConfig.SEA_BITE_DAMAGE.get());
        }
        this.setHealth(this.getMaxHealth());
        return data;
    }

    // ------------------------------------------------------------------ grabbing

    public boolean isGrabbing() {
        return this.entityData.get(DATA_GRABBING);
    }

    public boolean canGrab(LivingEntity target) {
        return !isGrabbing() && this.grabCooldown <= 0 && target.isInWater()
                && this.distanceToSqr(target) <= GRAB_RANGE * GRAB_RANGE;
    }

    public void startGrab(LivingEntity target) {
        this.grabbedId = target.getUUID();
        this.grabTicks = 0;
        this.damageWhileGrabbing = 0.0F;
        this.entityData.set(DATA_GRABBING, true);
        this.triggerAnim(CONTROLLER, "lunge");
        this.playSound(ModSounds.SEA_GRAB.get(), 1.5F, 0.9F + this.random.nextFloat() * 0.2F);
    }

    private void release() {
        this.grabbedId = null;
        this.grabTicks = 0;
        this.damageWhileGrabbing = 0.0F;
        this.grabCooldown = GRAB_COOLDOWN;
        this.entityData.set(DATA_GRABBING, false);
    }

    private void tickGrab() {
        LivingEntity target = this.getTarget();
        boolean holding = target != null && target.isAlive() && target.getUUID().equals(this.grabbedId)
                && target.isInWater() && this.distanceToSqr(target) < 5.0D * 5.0D
                && !(target instanceof Player player && (player.isCreative() || player.isSpectator()));
        if (!holding || ++this.grabTicks > StormRayConfig.SEA_GRAB_TICKS.get()
                || this.damageWhileGrabbing >= StormRayConfig.SEA_BREAK_FREE_DAMAGE.get()) {
            release();
            return;
        }
        // drag the prey straight down; it can still drift a little sideways
        double pull = StormRayConfig.SEA_PULL_STRENGTH.get();
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x * 0.3D, Math.min(motion.y, -pull), motion.z * 0.3D);
        target.hurtMarked = true;
        target.setSprinting(false);
        // cling just below the prey's legs, facing it
        Vec3 anchor = new Vec3(target.getX(), target.getY() - 0.8D, target.getZ());
        this.setDeltaMovement(anchor.subtract(position()).scale(0.6D));
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.grabTicks % 20 == 10) {
            target.invulnerableTime = 0;
            this.doHurtTarget(target);
        }
        if (this.grabTicks % 6 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.BUBBLE, target.getX(), target.getY() + 0.5D, target.getZ(), 6, 0.3D, 0.5D, 0.3D, 0.05D);
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.grabCooldown > 0) {
            this.grabCooldown--;
        }
        if (isGrabbing()) {
            tickGrab();
        }
        // it slowly dries out on land
        if (this.isInWaterOrBubble()) {
            this.dryTicks = 0;
        } else if (++this.dryTicks > 40 && this.dryTicks % 20 == 0) {
            this.hurt(this.damageSources().dryOut(), 2.0F);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(input);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isInWater() && this.random.nextFloat() < 0.2F) {
            this.level().addParticle(ModParticles.SOOT.get(), getRandomX(0.8D), getRandomY(), getRandomZ(0.8D), 0.0D, 0.01D, 0.0D);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isGrabbing()) {
            this.damageWhileGrabbing += amount;
        }
        return hurt;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.WATER;
    }

    @Override
    public int getMaxAirSupply() {
        return 6000;
    }

    @Override
    public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
        return level.isUnobstructed(this);
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SEA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SEA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SEA_DEATH.get();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GrabCooldown", this.grabCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.grabCooldown = tag.getInt("GrabCooldown");
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 5, this::animationPredicate)
                .triggerableAnim("lunge", ANIM_LUNGE));
    }

    private PlayState animationPredicate(AnimationState<PhantomSeaEntity> state) {
        if (this.isDeadOrDying()) {
            return state.setAndContinue(ANIM_DEATH);
        }
        if (isGrabbing()) {
            return state.setAndContinue(ANIM_GRAB);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_SWIM : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
