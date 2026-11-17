package com.stormray.entity;

import com.stormray.config.StormRayConfig;
import com.stormray.effect.BleedingEffect;
import com.stormray.registry.ModEffects;
import com.stormray.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
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
 * Phantom Bat: a soot-black bat with burning eyes. It is weak, but its bite leaves the victim
 * bleeding for a few seconds, and the wound ignores armor. It attacks by dashing at its prey.
 * They fly in small groups.
 */
public class PhantomBatEntity extends Monster implements GeoEntity {
    private static final String CONTROLLER = "main";
    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("animation.phantom_bat.fly");
    private static final RawAnimation ANIM_DASH = RawAnimation.begin().thenPlay("animation.phantom_bat.dash");
    private static final RawAnimation ANIM_BITE = RawAnimation.begin().thenPlay("animation.phantom_bat.bite");
    private static final RawAnimation ANIM_DEATH = RawAnimation.begin().thenPlayAndHold("animation.phantom_bat.death");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public PhantomBatEntity(EntityType<? extends PhantomBatEntity> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true); // it hovers from the moment it appears instead of dropping to the ground
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new PhantomBatDashGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnData, tag);
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(StormRayConfig.BAT_HEALTH.get());
        }
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(StormRayConfig.BAT_BITE_DAMAGE.get());
        }
        this.setHealth(this.getMaxHealth());
        return data;
    }

    /** Called by the dash goal when the bat rears back to dive at its prey. */
    public void startDashAnimation() {
        this.triggerAnim(CONTROLLER, "dash");
        this.playSound(SoundEvents.BAT_TAKEOFF, 1.0F, 0.6F);
    }

    /** The bite: a little damage now, and bleeding afterwards. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            BleedingEffect.spray(living, 30); // a burst of blood where the fangs went in
            int ticks = StormRayConfig.BAT_BLEED_TICKS.get();
            if (ticks > 0) {
                living.addEffect(new MobEffectInstance(ModEffects.BLEEDING.get(), ticks, 0), this);
            }
            this.playSound(SoundEvents.PHANTOM_BITE, 1.0F, 1.4F);
        }
        return hit;
    }

    /** Nothing the Storm Ray does can hurt its bats, and neither can falling, walls or lightning. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof StormRayEntity || source.getDirectEntity() instanceof StormRayEntity
                || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_LIGHTNING)
                || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isNoGravity() && this.isAlive()) {
            this.setNoGravity(true);
        }
        if (this.level().isClientSide && this.random.nextFloat() < 0.12F) {
            this.level().addParticle(ModParticles.SOOT.get(), getRandomX(0.5D), getRandomY(), getRandomZ(0.5D), 0.0D, -0.01D, 0.0D);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.setNoGravity(false); // a dead bat drops
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ModParticles.SOOT.get(), getX(), getY() + 0.4D, getZ(), 20, 0.3D, 0.3D, 0.3D, 0.04D);
        }
    }

    // ------------------------------------------------------------------ flying creature

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    // ------------------------------------------------------------------ sounds (vanilla bat, pitched down)

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.15F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, 3, this::animationPredicate)
                .triggerableAnim("dash", ANIM_DASH)
                .triggerableAnim("bite", ANIM_BITE));
    }

    private PlayState animationPredicate(AnimationState<PhantomBatEntity> state) {
        return state.setAndContinue(this.isDeadOrDying() ? ANIM_DEATH : ANIM_FLY);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
