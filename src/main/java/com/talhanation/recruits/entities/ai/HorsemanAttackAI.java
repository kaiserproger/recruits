package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.HorsemanEntity;
import com.talhanation.recruits.network.MessagePatrolLeaderSetCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

import static com.talhanation.recruits.entities.HorsemanEntity.State.*;

public class HorsemanAttackAI extends Goal {
    private final HorsemanEntity horseman;
    private LivingEntity target;
    private HorsemanEntity.State state;
    private BlockPos movePos;
    private int ticksUntilNextAttack;
    private int vecRotation = 0;
    private byte timeOut = 0;
    private BlockPos prevPos;

    public HorsemanAttackAI(HorsemanEntity recruit) {
        this.horseman = recruit;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public boolean canUse() {
        return horseman.getVehicle() instanceof AbstractHorse && horseman.getFollowState() == 0 && !horseman.needsToGetFood() && !horseman.getShouldMount();
    }

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public void start() {
        super.start();
        this.target = horseman.getTarget();
        this.state = SELECT_TARGET;
        this.ticksUntilNextAttack = 10 + getCooldownModifier();
    }

    public void tick() {
        if (this.horseman.tickCount % 15 == 0)
            this.prevPos = this.horseman.getOnPos();

        if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;
        switch (state) {
            case SELECT_TARGET -> {
                this.target = horseman.getTarget();
                if (target != null) {
                    Vec3 moveVec = target.position().subtract(horseman.position()).normalize();
                    Vec3 movePosVec = target.position().add(moveVec.scale(10)).yRot(this.vecRotation);
                    BlockPos possibleMovePosVec = new BlockPos(movePosVec.x, horseman.position().y, movePosVec.z);

                    if (isFreeSpotAbove(target.getOnPos())) {
                        //horseman.level.setBlock(possibleMovePosVec, Blocks.ICE.defaultBlockState(), 3);
                        this.movePos = possibleMovePosVec;
                        this.vecRotation = 0;
                        this.state = CHARGE_TARGET;
                    } else if (vecRotation > 360) {
                        vecRotation += 15;
                    } else {
                        stop();
                    }
                }
            }

            case CHARGE_TARGET -> {

                if (this.isStuck() || target == null || !target.isAlive() || !isFreeSpotAbove(target.getOnPos())) {
                    state = SELECT_TARGET;
                    this.prevPos = null;
                    return;
                }

                if (horseman.distanceToSqr(target) > 5F) {
                    horseman.getNavigation().moveTo(target.position().x, target.position().y, target.position().z, 1.15F);
                } else
                    state = MOVE_TO_POS;

                horseman.getLookControl().setLookAt(target, 30.0F, 30.0F);

                //Perform Attack
                if (horseman.distanceToSqr(target) < 7F) {
                    if (this.ticksUntilNextAttack <= 0) {
                        this.checkAndPerformAttack(target);
                    }
                }

                if (this.isStuck()) removeLeaves();
                else this.knockback();
            }

            case MOVE_TO_POS -> {
                if (this.isStuck() || target == null || !target.isAlive() || !isFreeSpotAbove(target.getOnPos())) {
                    state = SELECT_TARGET;
                    this.prevPos = null;
                    return;
                }

                Vec3 movePos2 = new Vec3(movePos.getX(), horseman.position().y, movePos.getZ());
                horseman.getLookControl().setLookAt(movePos2);
                horseman.getNavigation().moveTo(movePos2.x, movePos2.y, movePos2.z, 1.15F);

                if (horseman.distanceToSqr(movePos2) < 6F || ++timeOut > 126) {
                    timeOut = 0;
                    this.state = SELECT_TARGET;
                    this.prevPos = null;
                }

                if (this.isStuck()) removeLeaves();
                else this.knockback();
            }
        }
    }

    private void removeLeaves() {
        BlockState state = this.horseman.getCommandSenderWorld().getBlockState(horseman.getOnPos().above(1));
        if (state.getBlock() instanceof LeavesBlock)
            this.horseman.getCommandSenderWorld().destroyBlock(horseman.getOnPos().above(1), true);
    }

    private boolean isStuck() {
        return prevPos != null && this.prevPos.equals(this.horseman.getOnPos());
    }

    private boolean isFreeSpotAbove(BlockPos pos) {
        BlockState state = this.horseman.getCommandSenderWorld().getBlockState(pos.above(1));
        return state.isAir();
    }

    private void knockback() {
        horseman.getLevel().getEntitiesOfClass(
                LivingEntity.class,
                horseman.getBoundingBox().inflate(8D),
                (entity) -> horseman.distanceToSqr(entity) < 3.75F && horseman.canAttack(entity) && !entity.equals(horseman) && entity.getVehicle() == null
        ).forEach(entity -> {
            entity.knockback(0.85, Mth.sin(this.horseman.getYRot() * ((float) Math.PI / 180F)), (-Mth.cos(this.horseman.getYRot() * ((float) Math.PI / 180F))));
            entity.hurt(DamageSource.mobAttack(this.horseman), 1F);
            ;
        });
    }

    protected void checkAndPerformAttack(LivingEntity target) {
        if (!horseman.swinging) {
            this.horseman.swing(InteractionHand.MAIN_HAND);
            this.horseman.doHurtTarget(target);
            this.resetAttackCooldown();
        }
    }

    protected void resetAttackCooldown() {
        this.ticksUntilNextAttack = 15 + getCooldownModifier();
    }

    private int getCooldownModifier() {
        int modifier = 0;
        Item item = horseman.getMainHandItem().getItem();

        if (item instanceof TieredItem tieredItem) {
            modifier = 5 - (int) tieredItem.getTier().getSpeed();
        }

        if (item instanceof AxeItem) {
            modifier += 3;
        }

        return modifier;
    }

    private boolean canReach(BlockPos pos) {
        //this.reachCacheTime = reducedTickDelay(10 + this.horseman.getRandom().nextInt(5));
        Path path = horseman.getNavigation().createPath(pos, 0);
        if (path == null) {
            return false;
        } else {
            Node node = path.getEndNode();
            if (node == null) {
                return false;
            } else {
                int i = node.x - pos.getX();
                int j = node.z - pos.getZ();
                return (double) (i * i + j * j) <= 200D;
            }
        }
    }
}
