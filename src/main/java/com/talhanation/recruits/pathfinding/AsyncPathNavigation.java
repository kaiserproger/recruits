package com.talhanation.recruits.pathfinding;

import com.google.common.collect.ImmutableSet;
import com.talhanation.recruits.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AsyncPathNavigation extends PathNavigation {
    @Nullable
    private BlockPos targetPos;
    private int reachRange;
    private final PathFinder pathFinder;
    private boolean isStuck;

    public AsyncPathNavigation(PathfinderMob p_26515_, Level p_26516_) {
        super(p_26515_, p_26516_);
        int i = Mth.floor(p_26515_.getAttributeValue(Attributes.FOLLOW_RANGE) * 16.0D);
        this.pathFinder = this.createPathFinder(i);
    }
    
    protected @NotNull PathFinder createPathFinder(int p_26531_) {
        return new AsyncPathfinder(this.nodeEvaluator, p_26531_);
    }

    @Nullable
    public final Path createPathAsync(double p_26525_, double p_26526_, double p_26527_, int p_26528_) {
        return this.createPath(new BlockPos(p_26525_, p_26526_, p_26527_), p_26528_);
    }

    @Nullable
    public Path createPath(Stream<BlockPos> p_26557_, int p_26558_) {
        return this.createPath(p_26557_.collect(Collectors.toSet()), 8, false, p_26558_);
    }

    
    @Nullable
    public Path createPath(Set<BlockPos> p_26549_, int p_26550_) {
        return this.createPath(p_26549_, 8, false, p_26550_);
    }

    
    @Nullable
    public Path createPath(BlockPos p_26546_, int p_26547_) {
        return this.createPath(ImmutableSet.of(p_26546_), 8, false, p_26547_);
    }

    
    @Nullable
    public Path createPath(BlockPos p_148219_, int p_148220_, int p_148221_) {
        return this.createPath(ImmutableSet.of(p_148219_), 8, false, p_148220_, (float) p_148221_);
    }

    
    @Nullable
    public Path createPath(Entity p_26534_, int p_26535_) {
        return this.createPath(ImmutableSet.of(p_26534_.blockPosition()), 16, true, p_26535_);
    }


    @Nullable
    protected Path createPath(Set<BlockPos> p_26552_, int p_26553_, boolean p_26554_, int p_26555_) {
        return this.createPath(p_26552_, p_26553_, p_26554_, p_26555_, (float) this.mob.getAttributeValue(Attributes.FOLLOW_RANGE));
    }


    @Nullable
    protected Path createPath(Set<BlockPos> p_148223_, int p_148224_, boolean p_148225_, int p_148226_, float p_148227_) {
        if (p_148223_.isEmpty()) {
            return null;
        } else if (this.mob.getY() < (double) this.level.getMinBuildHeight()) {
            return null;
        } else if (!this.canUpdatePath()) {
            return null;
        } else if (this.path != null && (path instanceof AsyncPath asyncPath && !asyncPath.isProcessed() && asyncPath.hasSameProcessingPositions(p_148223_))) { // petal start - catch early if it's still processing these positions let it keep processing
            return this.path;
        } else if (this.path != null && !this.path.isDone() && p_148223_.contains(this.targetPos)) {
            return this.path;
        } else {
            this.level.getProfiler().push("pathfind");
            BlockPos blockpos = p_148225_ ? this.mob.blockPosition().above() : this.mob.blockPosition();
            int i = (int)(p_148227_ + (float)p_148224_);
            PathNavigationRegion pathnavigationregion = new PathNavigationRegion(this.level, blockpos.offset(-i, -i, -i), blockpos.offset(i, i, i));
            float maxVisitedNodesMultiplier = 1.0F;
            Path path = this.pathFinder.findPath(pathnavigationregion, this.mob, p_148223_, p_148227_, p_148226_, maxVisitedNodesMultiplier);
            this.level.getProfiler().pop();
            if (!p_148223_.isEmpty())
                this.targetPos = p_148223_.iterator().next(); // petal - assign early a target position. most calls will only have 1 position

            // petal start - async
            AsyncPathProcessor.awaitProcessing(path, this.level.getServer(), processedPath -> {
                if (processedPath != this.path){
                    return; // petal - check that processing didn't take so long that we calculated a new path
                }

                if (processedPath != null && processedPath.getTarget() != null) {
                    this.targetPos = processedPath.getTarget();
                    this.reachRange = p_148226_;
                    this.resetStuckTimeout();
                } else {
                    Main.LOGGER.error("PROCESSED PATH IS NULL OR TARGET IS NULL!!!!!");
                }
            });

            return path;
        }
    }

    @Override
    public boolean moveTo(double p_26520_, double p_26521_, double p_26522_, double p_26523_) {
        Main.LOGGER.info("Async moveTo called {} {} {}", p_26520_, p_26521_, p_26522_);
        return this.moveTo(this.createPath(new BlockPos(p_26520_, p_26521_, p_26522_), 1), p_26523_);
    }

    // Paper start - optimise pathfinding
    private int lastFailure = 0;
    private int pathfindFailures = 0;
    // Paper end

    @Override
    public boolean moveTo(@NotNull Entity p_26532_, double p_26533_) {
        int currentTick = Objects.requireNonNull(this.level.getServer()).getTickCount();
        // Paper start - Pathfinding optimizations
        if (this.pathfindFailures > 10 && this.path == null && currentTick < this.lastFailure + 40) {
            return false;
        }
        // Paper end
        Path path = this.createPath(p_26532_, 1);
        // Paper start - Pathfinding optimizations
        if (path != null && this.moveTo(path, p_26533_)) {
            this.lastFailure = 0;
            this.pathfindFailures = 0;
            return true;
        } else {
            this.pathfindFailures++;
            this.lastFailure = currentTick;
            return false;
        }
        // Paper end
    }

    @Override
    public boolean moveTo(@Nullable Path p_26537_, double p_26538_) {
        if (p_26537_ == null) {
            this.path = null;
            return false;
        }
        if (!p_26537_.sameAs(this.path)) {
            this.path = p_26537_;
        }

        if (this.isDone()) {
            return false;
        }

        if (path != null) {
            if (!(path instanceof AsyncPath asyncPath)) {
                this.trimPath();
            } else {
                if (asyncPath.isProcessed()) {
                    this.trimPath();
                }
            }

            if (this.path.getNodeCount() <= 0) {
                return false;
            }
        }

        this.speedModifier = p_26538_;
        Vec3 vec3 = this.getTempMobPos();
        this.lastStuckCheck = this.tick;
        this.lastStuckCheckPos = vec3;
        return true;
    }

    public void recomputePath() {
        if (this.level.getGameTime() - this.timeLastRecompute > 20L) {
            if (this.targetPos != null) {
                this.path = null;
                this.path = this.createPath(this.targetPos, this.reachRange);
                this.timeLastRecompute = this.level.getGameTime();
                this.hasDelayedRecomputation = false;
            }
        } else {
            this.hasDelayedRecomputation = true;
        }
    }

    private void resetStuckTimeout() {
        this.timeoutCachedNode = Vec3i.ZERO;
        this.timeoutTimer = 0L;
        this.timeoutLimit = 0.0D;
        this.isStuck = false;
    }

    @Override
    public boolean isStuck() {
        return this.isStuck;
    }

    @Override
    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }

        if (this.path instanceof AsyncPath asyncPath && !asyncPath.isProcessed()) return; // petal - skip pathfinding if we're still processing

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3 vec3 = this.getTempMobPos();
                Vec3 vec31 = this.path.getNextEntityPos(this.mob);
                if (vec3.y > vec31.y && !this.mob.isOnGround() && Mth.floor(vec3.x) == Mth.floor(vec31.x) && Mth.floor(vec3.z) == Mth.floor(vec31.z)) {
                    this.path.advance();
                }
            }

            DebugPackets.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
            if (!this.isDone()) {
                Path path = Objects.requireNonNull(this.path);
                Vec3 vec32 = path.getNextEntityPos(this.mob);
                this.mob.getMoveControl().setWantedPosition(vec32.x, this.getGroundY(vec32), vec32.z, this.speedModifier);
            }
        }
    }
}