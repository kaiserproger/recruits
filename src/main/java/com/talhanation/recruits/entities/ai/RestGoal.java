package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Comparator;
import java.util.Stack;

public class RestGoal extends Goal {
    private final AbstractRecruitEntity recruit;
    private Stack<BlockPos> stackOfBeds;
    private BlockPos sleepPos;
    private long lastCanUseCheck;

    public RestGoal(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    private boolean isHealth(){
        return recruit.getHealth() < recruit.getMaxHealth();
    }

    private boolean isMorale(){
        return recruit.getMoral() < 45;
    }
    private boolean canRest(){
        return (recruit.getShouldRest() || this.recruit.getCommandSenderWorld().isNight()) && recruit.getFollowState() == 0 && recruit.getTarget() == null && (isMorale() || isHealth());
    }

    @Override
    public boolean canUse() {
        long i = this.recruit.getCommandSenderWorld().getGameTime();
        if (i - this.lastCanUseCheck >= 20L) {
            this.lastCanUseCheck = i;
            return this.canRest();
        }
        return false;
    }

    public boolean canContinueToUse() {
        return this.canRest();
    }

    @Override
    public void start() {
        super.start();
        this.stackOfBeds = this.getListOfBeds();
    }

    @Override
    public void stop() {
        super.stop();
        this.stackOfBeds.removeAllElements();
        this.recruit.stopSleeping();
        this.recruit.clearSleepingPos();
        this.recruit.setShouldRest(false);
    }

    @Override
    public void tick() {

        if (recruit.isSleeping()) {
            this.recruit.getNavigation().stop();
            this.recruit.heal(0.0025F);
            if(this.recruit.getMoral() < 60) this.recruit.setMoral(this.recruit.getMoral() + 0.0025F);
            return;
        }

        if(!stackOfBeds.isEmpty()){
            if(this.sleepPos != null){
                BlockEntity bedEntity = recruit.getCommandSenderWorld().getBlockEntity(sleepPos);
                if (bedEntity != null && bedEntity.getBlockState().isBed(recruit.getCommandSenderWorld(), sleepPos, recruit) && !bedEntity.getBlockState().getValue(BlockStateProperties.OCCUPIED)) {
                    this.goToBed(sleepPos);
                }
                else{
                    this.sleepPos = this.stackOfBeds.pop();
                }
            }
            else{
                this.sleepPos = this.stackOfBeds.pop();
            }

        }
        else stop();
    }


    private void goToBed(BlockPos bedPos) {
        if (bedPos == null) {
            return;
        }
        // Move to the bed and stay there.
        PathNavigation pathFinder = this.recruit.getNavigation();
        pathFinder.moveTo(bedPos.getX(), bedPos.getY(), bedPos.getZ(), 1D);
        this.recruit.getLookControl().setLookAt(
                bedPos.getX(),
                bedPos.getY() + 1,
                bedPos.getZ(),
                10.0F,
                (float) this.recruit.getMaxHeadXRot()
        );

        if (bedPos.distToCenterSqr(recruit.position()) <= 75) {
            this.recruit.startSleeping(bedPos);
            this.recruit.setSleepingPos(bedPos);
            pathFinder.stop();
        }
    }

    private Stack<BlockPos> getListOfBeds() {
        Stack<BlockPos> stack = new Stack<>();
        int range = 25;

        for (int x = -range; x < range; x++) {
            for (int y = -3; y < 10; y++) {
                for (int z = -range; z < range; z++) {
                    BlockPos pos = recruit.getOnPos().offset(x, y, z);
                    BlockState state = recruit.getCommandSenderWorld().getBlockState(pos);

                    if (state.isBed(recruit.getCommandSenderWorld(), pos, this.recruit) &&
                        state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD &&
                        !state.getValue(BlockStateProperties.OCCUPIED)) {
                        stack.push(pos);
                    }
                }
            }
        }
        stack.sort(Comparator.comparing(pos -> recruit.distanceToSqr(pos.getCenter())));

        return stack;
    }


}