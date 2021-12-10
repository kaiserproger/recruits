package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.BowmanEntity;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.util.EnumSet;


public class RecruitRangedBowAttackGoal<T extends BowmanEntity & IRangedAttackMob> extends Goal {
    private final T mob;
    private final double speedModifier;
    private int attackIntervalMin;
    private LivingEntity target;
    private final float attackRadiusSqr;
    private int attackTime = -1;
    private int seeTime;
    private final int attackIntervalMax;
    private final float attackRadius;

    public RecruitRangedBowAttackGoal(T mob, double speedModifier, int attackIntervalMin, int attackIntervalMax, float attackRadius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.attackIntervalMax = attackIntervalMax;
        this.attackRadiusSqr = attackRadius * attackRadius;
        this.attackRadius = attackRadius;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public void setMinAttackInterval(int min) {
        this.attackIntervalMin = min;
    }

    public boolean canUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity != null && livingentity.isAlive() && this.isHoldingBow()) {
            this.target = livingentity;
            return true;
        } else {
            return false;
        }
    }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(item -> item instanceof BowItem);
    }

    public boolean canContinueToUse() {
        return (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingBow();
    }

    public void start() {
        super.start();
        this.mob.setAggressive(true);
    }

    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.mob.stopUsingItem();
    }

    public void tick() {
        //if (mob.getHoldPos() != null)Objects.requireNonNull(this.mob.getOwner()).sendMessage(new StringTextComponent("Pos vorhanden"), mob.getOwner().getUUID());

        double d0 = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getSensing().canSee(target);
        if (canSee) {
            ++this.seeTime;
        } else {
            this.seeTime = 0;
        }

        if (!(d0 > (double)this.attackRadiusSqr / 1.5) && this.seeTime >= 5) {
            this.mob.getNavigation().stop();
        } else {
            if (mob.getHoldPos() != null) {
                if ((!mob.getHoldPos().closerThan(mob.position(), 8D))){
                    this.mob.getNavigation().moveTo(target, this.speedModifier);
                }
            }
            else this.mob.getNavigation().moveTo(target, this.speedModifier);
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.mob.isUsingItem()) {
            if (!canSee && this.seeTime < -60) {
                this.mob.stopUsingItem();
            } else if (canSee) {
                int i = this.mob.getTicksUsingItem();
                if (i >= 20) {
                    this.mob.stopUsingItem();
                    this.mob.performRangedAttack(target, BowItem.getPowerForTime(i));
                    float f = MathHelper.sqrt(d0) / this.attackRadius;
                    this.attackTime = MathHelper.floor(f * (float)(this.attackIntervalMax - this.attackIntervalMin) + (float)this.attackIntervalMin);
                }
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            this.mob.startUsingItem(ProjectileHelper.getWeaponHoldingHand(this.mob, Items.BOW));
        }

    }
}
