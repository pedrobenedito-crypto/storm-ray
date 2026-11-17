package com.stormray.entity;

import com.stormray.config.StormRayConfig;
import com.stormray.registry.ModParticles;
import com.stormray.registry.ModSounds;
import com.stormray.util.StormMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * Phantom X: a floating soot wraith. It lurks cloaked (a faint, trembling silhouette), slips
 * close to its prey, reveals itself and unleashes a fast multi-hit combo, then fades away
 * and retreats before striking again. Being hurt also tears the cloak away for a moment.
 */
public class PhantomXEntity extends Monster implements GeoEntity {
    public static final float CLOAKED_ALPHA = 0.12F;
    private static final String CONTROLLER = "main";
    private static final int COMBO_SPACING = 5;      // ticks between hits
    private static final int REVEAL_AFTER_COMBO = 12;
    private static final int REVEAL_WHEN_HURT = 30;
    private static final double REACH = 3.2D;

    private static final EntityDataAccessor<Boolean> DATA_CLOAKED =
            SynchedEntityData.defineId(PhantomXEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.phantom_x.idle");
    private static final RawAnimation ANIM_MOVE = RawAnimation.begin().thenLoop("animation.phantom_x.move");
    private static final RawAnimation ANIM_COMBO = RawAnimation.begin().thenPlay("animation.phantom_x.combo");
    private static final RawAnimation ANIM_REVEAL = RawAnimation.begin().thenPlay("animation.phantom_x.reveal");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("animation.phantom_x.death");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int comboTick = -1;
    private int comboHitsLeft;
    private int comboCooldown = 20;
    private int revealTicks;
    private int retreatTicks;

    // client-side smoothing of the cloak
    private float cloak = 1.0F;
    private float prevCloak = 1.0F;

    public PhantomXEntity(EntityType<? extends PhantomXEntity> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_CLOAKED, true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PhantomXAttackGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(StormRayConfig.PHANTOM_HEALTH.get());
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(StormRayConfig.PHANTOM_HIT_DAMAGE.get());
        }
        this.setHealth(this.getMaxHealth());
        return data;
    }

    // ------------------------------------------------------------------ cloak

    public boolean isCloaked() {
        return this.entityData.get(DATA_CLOAKED);
    }

    private void setCloaked(boolean cloaked) {
        if (cloaked != isCloaked()) {
            this.entityData.set(DATA_CLOAKED, cloaked);
            this.playSound(cloaked ? ModSounds.PHANTOM_CLOAK.get() : ModSounds.PHANTOM_REVEAL.get(), 1.0F,
                    0.9F + this.random.nextFloat() * 0.2F);
            if (!cloaked) {
                this.triggerAnim(CONTROLLER, "reveal");
            }
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ModParticles.SOOT.get(), getX(), getY() + 1.3D, getZ(), 25, 0.4D, 0.8D, 0.4D, 0.03D);
            }
        }
    }

    /** Tears the cloak away for a while, e.g. when a colosseum wave steps out of the ruins. */
    public void revealFor(int ticks) {
        this.revealTicks = Math.max(this.revealTicks, ticks);
        setCloaked(false);
    }

    /** 0 = fully cloaked look, 1 = solid. Smoothed on the client for rendering. */
    public float getCloakVisibility(float partialTick) {
        return Mth.lerp(partialTick, this.prevCloak, this.cloak);
    }

    // ------------------------------------------------------------------ combo

    public boolean isComboActive() {
        return this.comboTick >= 0;
    }

    public boolean canStartCombo() {
        return !isComboActive() && this.comboCooldown <= 0 && this.retreatTicks <= 0;
    }

    public boolean isRetreating() {
        return this.retreatTicks > 0;
    }

    public void startCombo() {
        this.comboTick = 0;
        this.comboHitsLeft = StormRayConfig.PHANTOM_COMBO_HITS.get();
        this.revealTicks = Math.max(this.revealTicks, this.comboHitsLeft * COMBO_SPACING + REVEAL_AFTER_COMBO);
        setCloaked(false);
        this.triggerAnim(CONTROLLER, "combo");
    }

    private void tickCombo() {
        LivingEntity target = this.getTarget();
        this.comboTick++;
        if (this.comboTick % COMBO_SPACING == 3 && this.comboHitsLeft > 0) {
            this.comboHitsLeft--;
            if (target != null && target.isAlive() && this.distanceToSqr(target) <= REACH * REACH) {
                target.invulnerableTime = 0; // every hit of the flurry lands
                this.doHurtTarget(target);
            }
            this.playSound(ModSounds.PHANTOM_SLASH.get(), 1.0F, 0.85F + this.random.nextFloat() * 0.3F);
        }
        if (this.comboHitsLeft <= 0 && this.comboTick % COMBO_SPACING == 4) {
            this.comboTick = -1;
            this.comboCooldown = StormRayConfig.PHANTOM_COMBO_COOLDOWN.get();
            this.retreatTicks = 25;
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.comboCooldown > 0) {
            this.comboCooldown--;
        }
        if (this.retreatTicks > 0) {
            this.retreatTicks--;
        }
        if (this.revealTicks > 0) {
            this.revealTicks--;
        }
        if (isComboActive()) {
            tickCombo();
        }
        setCloaked(this.revealTicks <= 0 && !isComboActive());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.prevCloak = this.cloak;
            this.cloak = StormMath.approach(this.cloak, isCloaked() ? CLOAKED_ALPHA : 1.0F, isCloaked() ? 0.06F : 0.25F);
            if (isCloaked() && this.random.nextFloat() < 0.35F) {
                // the faint trail of soot that gives it away
                this.level().addParticle(ModParticles.SOOT.get(), getRandomX(0.6D), getY() + this.random.nextDouble() * 2.2D,
                        getRandomZ(0.6D), 0.0D, -0.01D, 0.0D);
            }
            if (!isCloaked() && this.random.nextFloat() < 0.15F) {
                this.level().addParticle(ModParticles.STORM_EMBER.get(), getX(), getY() + 2.1D, getZ(), 0.0D, 0.02D, 0.0D);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof StormRayEntity || source.getDirectEntity() instanceof StormRayEntity) {
            return false; // the Storm Ray never harms its own phantoms
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.revealTicks = Math.max(this.revealTicks, REVEAL_WHEN_HURT);
            setCloaked(false);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.SOOT.get(), getX(), getY() + 1.2D, getZ(), 60, 0.4D, 0.9D, 0.4D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0D, getZ(), 20, 0.3D, 0.6D, 0.3D, 0.02D);
        }
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PHANTOM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PHANTOM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PHANTOM_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.9F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // it floats
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ComboCooldown", this.comboCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.comboCooldown = tag.getInt("ComboCooldown");
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 4, this::animationPredicate)
                .triggerableAnim("combo", ANIM_COMBO)
                .triggerableAnim("reveal", ANIM_REVEAL));
    }

    private PlayState animationPredicate(AnimationState<PhantomXEntity> state) {
        if (this.isDeadOrDying()) {
            return state.setAndContinue(ANIM_DEATH);
        }
        return state.setAndContinue(state.isMoving() ? ANIM_MOVE : ANIM_IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
