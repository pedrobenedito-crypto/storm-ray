package com.stormray.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * How the Phantom Bat attacks: it flies up to its prey, hangs in the air for a moment with its
 * wings raised, then dashes straight at it and bites on contact. After the dash it pulls away
 * and circles back for the next one.
 */
public class PhantomBatDashGoal extends Goal {
    private static final int WINDUP_TICKS = 5;
    private static final int DASH_TICKS = 14;
    private static final double DASH_SPEED = 0.85D;
    private static final double DASH_RANGE = 7.0D;

    private final PhantomBatEntity bat;
    private int repathDelay;
    private int cooldown;
    /** -1 = approaching; 0..WINDUP = rearing back; above = dashing. */
    private int dashTick = -1;
    private Vec3 dashDirection = Vec3.ZERO;
    private boolean bitten;

    public PhantomBatDashGoal(PhantomBatEntity bat) {
        this.bat = bat;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.bat.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.bat.setAggressive(true);
        this.repathDelay = 0;
        this.dashTick = -1;
    }

    @Override
    public void stop() {
        this.bat.setAggressive(false);
        this.bat.getNavigation().stop();
        this.dashTick = -1;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.bat.getTarget();
        if (target == null) {
            return;
        }
        this.bat.getLookControl().setLookAt(target, 60.0F, 60.0F);
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        Vec3 toTarget = aim.subtract(this.bat.position());

        if (this.dashTick < 0) {
            // approach, staying a little above the prey
            if (--this.repathDelay <= 0) {
                this.repathDelay = 6 + this.bat.getRandom().nextInt(6);
                this.bat.getNavigation().moveTo(target.getX(), target.getY() + 2.5D, target.getZ(), 1.3D);
            }
            if (this.cooldown <= 0 && toTarget.lengthSqr() <= DASH_RANGE * DASH_RANGE && this.bat.hasLineOfSight(target)) {
                this.dashTick = 0;
                this.bitten = false;
                this.bat.getNavigation().stop();
                this.bat.startDashAnimation();
            }
            return;
        }

        this.dashTick++;
        if (this.dashTick <= WINDUP_TICKS) {
            // rear back: hang in the air and drift slightly away from the prey
            Vec3 back = toTarget.lengthSqr() > 1.0E-4D ? toTarget.normalize().scale(-0.06D) : Vec3.ZERO;
            this.bat.setDeltaMovement(back.x, 0.05D, back.z);
            if (this.dashTick == WINDUP_TICKS) {
                this.dashDirection = toTarget.lengthSqr() > 1.0E-4D ? toTarget.normalize() : Vec3.ZERO;
            }
            return;
        }
        // the dash itself: a straight line, so a sidestep makes it miss
        this.bat.setDeltaMovement(this.dashDirection.scale(DASH_SPEED));
        if (!this.bitten && this.bat.getBoundingBox().inflate(0.35D).intersects(target.getBoundingBox())) {
            this.bitten = true;
            this.bat.doHurtTarget(target);
        }
        if (this.bitten || this.dashTick >= WINDUP_TICKS + DASH_TICKS || this.bat.horizontalCollision) {
            // pull up and away, then come round again
            this.bat.setDeltaMovement(this.dashDirection.scale(0.25D).add(0.0D, 0.35D, 0.0D));
            this.cooldown = 30 + this.bat.getRandom().nextInt(25);
            this.dashTick = -1;
            this.repathDelay = 10;
        }
    }
}
