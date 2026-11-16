package com.stormray.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Stalk while cloaked, strike with a combo when in reach, then slip away and come back. */
public class PhantomXAttackGoal extends Goal {
    private static final double REACH_SQR = 2.8D * 2.8D;
    private final PhantomXEntity mob;
    private int repathDelay;

    public PhantomXAttackGoal(PhantomXEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.mob.isWithinRestriction(target.blockPosition());
    }

    @Override
    public void start() {
        this.mob.setAggressive(true);
        this.repathDelay = 0;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.mob.isComboActive()) {
            this.mob.getNavigation().stop();
            return;
        }
        if (--this.repathDelay <= 0) {
            this.repathDelay = 4 + this.mob.getRandom().nextInt(6);
            if (this.mob.isRetreating()) {
                Vec3 away = DefaultRandomPos.getPosAway(this.mob, 10, 5, target.position());
                if (away != null) {
                    this.mob.getNavigation().moveTo(away.x, away.y, away.z, 1.35D);
                }
            } else {
                this.mob.getNavigation().moveTo(target, this.mob.isCloaked() ? 1.35D : 1.1D);
            }
        }
        if (!this.mob.isRetreating() && this.mob.canStartCombo() && this.mob.distanceToSqr(target) <= REACH_SQR
                && this.mob.hasLineOfSight(target)) {
            this.mob.getNavigation().stop();
            this.mob.startCombo();
        }
    }
}
