package com.stormray.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Swims at prey that is in the water and grabs it when close enough. */
public class PhantomSeaHuntGoal extends Goal {
    private final PhantomSeaEntity mob;
    private int repathDelay;

    public PhantomSeaHuntGoal(PhantomSeaEntity mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && (target.isInWater() || this.mob.isGrabbing());
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
        if (this.mob.isGrabbing()) {
            this.mob.getNavigation().stop();
            return;
        }
        if (--this.repathDelay <= 0) {
            this.repathDelay = 5 + this.mob.getRandom().nextInt(5);
            this.mob.getNavigation().moveTo(target, 1.4D);
        }
        if (this.mob.canGrab(target)) {
            this.mob.getNavigation().stop();
            this.mob.startGrab(target);
        }
    }
}
