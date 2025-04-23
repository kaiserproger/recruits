package com.talhanation.recruits.entities.ai;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.List;

public class RecruitMountEntity extends Goal {

    private final AbstractRecruitEntity recruit;
    private Entity mount;
    private int timeToRecalcPath;
    private int searchTime;
    private static final int MAX_SEARCH_TIME = 400;

    public RecruitMountEntity(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    @Override
    public boolean canUse() {
        return recruit.getShouldMount ();
    }

    public boolean canContinueToUse() {
        return canUse () && searchTime < MAX_SEARCH_TIME;
    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.searchTime = 0;
        this.findMount ();
        this.recruit.setMountTimer (200);
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        searchTime++;
        if (this.recruit.getVehicle () == null && this.mount != null) {
            if (recruit.getMountTimer () > 0) {

                if (--this.timeToRecalcPath <= 0) {
                    this.timeToRecalcPath = this.adjustedTickDelay (10);
                    recruit.getNavigation ().moveTo (mount, 1.15F);
                }

                if (recruit.horizontalCollision || recruit.minorHorizontalCollision) {
                    this.recruit.getJumpControl ().jump ();
                }

                if (recruit.distanceToSqr (mount) < 50D) {
                    recruit.startRiding (mount);
                    if (recruit.isPassenger ()) recruit.setShouldMount (false);
                }
            } else {
                recruit.setShouldMount (false);
            }
        } else if (searchTime >= MAX_SEARCH_TIME) {
            recruit.setShouldMount (false);
        }
    }

    private void findMount() {
        List<Entity> mounts = recruit.getCommandSenderWorld ().getEntitiesOfClass (
                Entity.class,
                recruit.getBoundingBox ().inflate (20D),
                (mount) -> (RecruitsServerConfig.MountWhiteList.get ().contains (mount.getEncodeId ()) ||
                        mount instanceof AbstractHorse) &&
                        !mount.isVehicle () &&
                        mount.getPassengers ().isEmpty ()
        );

        if (!mounts.isEmpty ()) {
            this.mount = mounts.stream ()
                    .min ((m1, m2) -> Double.compare (recruit.distanceToSqr (m1), recruit.distanceToSqr (m2)))
                    .orElse (null);
        }
    }
}
