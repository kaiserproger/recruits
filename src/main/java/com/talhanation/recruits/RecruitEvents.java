package com.talhanation.recruits;

import com.talhanation.recruits.compat.IWeapon;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.ai.horse.HorseRiddenByRecruitGoal;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.inventory.PromoteContainer;
import com.talhanation.recruits.network.MessageOpenPromoteScreen;
import com.talhanation.recruits.world.PillagerPatrolSpawn;
import com.talhanation.recruits.world.RecruitUnitManager;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RecruitEvents {
    private static final Map<ServerLevel, RecruitsPatrolSpawn> RECRUIT_PATROL = new HashMap<>();
    private static final Map<ServerLevel, PillagerPatrolSpawn> PILLAGER_PATROL = new HashMap<>();
    public static RecruitUnitManager recruitUnitManager;
    public static MinecraftServer server;
    static HashMap<Integer, EntityType<? extends AbstractRecruitEntity>> entitiesByProfession = new HashMap<>() {
        {
            put(0, ModEntityTypes.MESSENGER.get());
            put(2, ModEntityTypes.PATROL_LEADER.get());
            put(3, ModEntityTypes.CAPTAIN.get());
        }
    };

    public static void promoteRecruit(AbstractRecruitEntity recruit, int profession, String name, ServerPlayer player) {
        EntityType<? extends AbstractRecruitEntity> companionType = entitiesByProfession.get(profession);
        AbstractRecruitEntity abstractRecruit = companionType.create(recruit.getCommandSenderWorld());
        if (abstractRecruit instanceof ICompanion companion) {
            abstractRecruit.setCustomName(Component.literal(name));
            abstractRecruit.copyPosition(recruit);
            companion.applyRecruitValues(recruit);
            companion.setOwnerName(player.getName().getString());

            recruit.discard();
            abstractRecruit.getCommandSenderWorld().addFreshEntity(abstractRecruit);
        }
    }

    public static void openPromoteScreen(Player player, AbstractRecruitEntity recruit) {
        if (player instanceof ServerPlayer) {
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return recruit.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new PromoteContainer(i, playerEntity, recruit);
                }
            }, packetBuffer -> {
                packetBuffer.writeUUID(recruit.getUUID());
            });
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenPromoteScreen(player, recruit.getUUID()));
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();

        recruitUnitManager = new RecruitUnitManager();
        recruitUnitManager.load(server.overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        recruitUnitManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        recruitUnitManager.save(server.overworld());
    }


    @SubscribeEvent
    public void onTeleportEvent(EntityTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(event instanceof EntityTeleportEvent.EnderPearl) && !(event instanceof EntityTeleportEvent.ChorusFruit) && !(event instanceof EntityTeleportEvent.EnderEntity)) {
            double targetX = event.getTargetX();
            double targetY = event.getTargetY();
            double targetZ = event.getTargetZ();

            List<AbstractRecruitEntity> recruits = player.getLevel().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    player.getBoundingBox()
                            .inflate(64, 32, 64),
                    AbstractRecruitEntity::isAlive
            );

            recruits.forEach(recruit -> recruit.teleportTo(targetX, targetY, targetZ));
            //wip
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.level instanceof ServerLevel serverWorld) {
            if (RecruitsServerConfig.ShouldRecruitPatrolsSpawn.get()) {
                RECRUIT_PATROL.computeIfAbsent(serverWorld,
                        serverLevel -> new RecruitsPatrolSpawn(serverWorld));
                RecruitsPatrolSpawn spawner = RECRUIT_PATROL.get(serverWorld);
                spawner.tick();
            }

            if (RecruitsServerConfig.ShouldPillagerPatrolsSpawn.get()) {
                PILLAGER_PATROL.computeIfAbsent(serverWorld,
                        serverLevel -> new PillagerPatrolSpawn(serverWorld));
                PillagerPatrolSpawn pillagerSpawner = PILLAGER_PATROL.get(serverWorld);
                pillagerSpawner.tick();
            }
        }
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        Entity entity = event.getEntity();
        HitResult rayTrace = event.getRayTraceResult();
        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();

            if (rayTrace.getType() == HitResult.Type.ENTITY) {
                Entity impactEntity = ((EntityHitResult) rayTrace).getEntity();
                String encode = impactEntity.getEncodeId();
                if (impactEntity instanceof LivingEntity impactLiving) {

                    if (owner instanceof AbstractRecruitEntity recruit) {

                        if (impactLiving instanceof Animal animal) {
                            if (animal.getFirstPassenger() instanceof AbstractRecruitEntity passenger) {
                                if (!canDamageTarget(recruit, passenger)) {
                                    event.setCanceled(true);
                                }
                            } else if (animal.getFirstPassenger() instanceof Player player) {
                                if (!canDamageTarget(recruit, player)) {
                                    event.setCanceled(true);
                                }
                            }
                        }

                        if (!canDamageTarget(recruit, impactLiving)) {
                            event.setCanceled(true);
                        } else {
                            recruit.addXp(2);
                            recruit.checkLevel();
                        }
                    }

                    if (owner instanceof AbstractIllager illager && !RecruitsServerConfig.PillagerFriendlyFire.get()) {

                        if (illager.isAlliedTo(impactLiving)) {
                            event.setCanceled(true);
                        }
                    }

                    if (owner instanceof Player player) {
                        if (!canHarmTeam(player, impactLiving)) {
                            event.setCanceled(true);
                        }
                    }
                } else if (encode != null && encode.contains("corpse")) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteractWithCaravan(PlayerInteractEvent.EntityInteract entityInteract) {
        Player player = entityInteract.getEntity();
        Entity interacting = entityInteract.getTarget();

        if (interacting instanceof AbstractChestedHorse chestedHorse) {
            CompoundTag nbt = chestedHorse.getPersistentData();
            if (!nbt.contains("Caravan") || !chestedHorse.hasChest()) {
                return;
            }

            player.getLevel().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    player.getBoundingBox().inflate(64F),
                    (recruit) -> !recruit.isOwned() &&
                            (recruit.getName().getString().equals("Caravan Leader") ||
                                    recruit.getName().getString().equals("Caravan Guard"))
            ).forEach((recruit) -> recruit.setTarget(player));
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (Main.isMusketModLoaded) {
            Entity sourceEntity = event.getSource().getEntity();
            if (sourceEntity instanceof AbstractRecruitEntity owner && IWeapon.isMusketModWeapon(owner.getMainHandItem())) {
                Entity target = event.getEntity();
                if (target instanceof LivingEntity impactEntity) {

                    if (!canDamageTarget(owner, impactEntity)) {
                        event.setCanceled(true);
                    } else {
                        owner.addXp(2);
                        owner.checkLevel();
                    }
                }
            }
        }

        Entity target = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source instanceof LivingEntity sourceEntity) {
            if (target.getTeam() == null) return;

            target.getLevel().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    target.getBoundingBox().inflate(32D),
                    (recruit) -> recruit.getTarget() == null &&
                            recruit.getTeam() != null &&
                            recruit.getTeam().equals(target.getTeam())
            ).forEach((recruit) -> recruit.setTarget(sourceEntity));
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        Entity target = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();

        if (target instanceof AbstractRecruitEntity recruit &&
                sourceEntity instanceof Player player &&
                !canHarmTeam(player, recruit)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHorseJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof AbstractHorse horse) {
            horse.goalSelector.addGoal(0, new HorseRiddenByRecruitGoal(horse));
        }
    }

    @SubscribeEvent
    public void onBlockBreakEvent(BlockEvent.BreakEvent event) {
        if (RecruitsServerConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();
            if (blockBreaker == null) return;

            final boolean[] warn = {false};
            final String[] name = new String[1];
            blockBreaker.getLevel().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    blockBreaker.getBoundingBox().inflate(32.0D)
            ).forEach((recruit) -> {
                if (canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 1) {
                    recruit.setTarget(blockBreaker);
                }

                if (!warn[0] && canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned()) {
                    warn[0] = true;
                    name[0] = recruit.getName().toString();
                }
            });

            if (warn[0]) {
                warnPlayer(blockBreaker, TEXT_BLOCK_WARN(name[0]));
            }
        }

        if (RecruitsServerConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Player blockBreaker = event.getPlayer();
            if (blockBreaker == null) return;

            final boolean[] warn = {false};
            final String[] name = new String[1];

            blockBreaker.getLevel().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    blockBreaker.getBoundingBox().inflate(32.0D)
            ).forEach((recruit) -> {
                if (canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned()) {
                    recruit.setTarget(blockBreaker);
                }

                if (!warn[0] && canDamageTargetBlockEvent(recruit, blockBreaker) && recruit.getState() == 0 && recruit.isOwned()) {
                    warn[0] = true;
                    name[0] = recruit.getName().toString();
                }
            });

            if (warn[0]) {
                warnPlayer(blockBreaker, TEXT_BLOCK_WARN(name[0]));
            }
        }
    }

    @SubscribeEvent
    public void onBlockPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if (RecruitsServerConfig.AggroRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                final boolean[] warn = {false};
                final String[] name = new String[1];

                livingBlockPlacer.getLevel().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        livingBlockPlacer.getBoundingBox().inflate(32.0D),
                        (recruit) -> canDamageTargetBlockEvent(recruit, livingBlockPlacer)
                ).forEach((recruit) -> {
                    if (recruit.getState() == 1) {
                        recruit.setTarget(livingBlockPlacer);
                    }

                    if (blockPlacer instanceof Player && !warn[0] &&
                            recruit.getState() == 0 && recruit.isOwned()) {
                        warn[0] = true;
                        name[0] = recruit.getName().toString();
                    }
                });

                if (blockPlacer instanceof Player player && warn[0]) {
                    warnPlayer(player, TEXT_BLOCK_WARN(name[0]));
                }
            }
        }

        if (RecruitsServerConfig.NeutralRecruitsBlockPlaceBreakEvents.get()) {
            Entity blockPlacer = event.getEntity();

            final boolean[] warn = {false};
            final String[] name = new String[1];

            if (blockPlacer instanceof LivingEntity livingBlockPlacer) {
                livingBlockPlacer.getLevel().getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        livingBlockPlacer.getBoundingBox().inflate(32.0D),
                        (recruit) -> canDamageTargetBlockEvent(recruit, livingBlockPlacer) &&
                                recruit.getState() == 0 && recruit.isOwned()
                ).forEach((recruit) -> {
                    recruit.setTarget(livingBlockPlacer);

                    if (blockPlacer instanceof Player && !warn[0]) {
                        warn[0] = true;
                        name[0] = recruit.getName().toString();
                    }
                });

                if (blockPlacer instanceof Player player && warn[0]) {
                    warnPlayer(player, TEXT_BLOCK_WARN(name[0]));
                }
            }
        }
    }

    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getHitVec().getBlockPos();
        Player player = event.getEntity();

        BlockState selectedBlock = player.level.getBlockState(pos);
        BlockEntity blockEntity = player.level.getBlockEntity(pos);

        if (selectedBlock.is(BlockTags.BUTTONS) ||
                selectedBlock.is(BlockTags.DOORS) ||
                selectedBlock.is(BlockTags.WOODEN_TRAPDOORS) ||
                selectedBlock.is(BlockTags.WOODEN_BUTTONS) ||
                selectedBlock.is(BlockTags.WOODEN_DOORS) ||
                selectedBlock.is(BlockTags.SHULKER_BOXES) ||
                selectedBlock.is(BlockTags.FENCE_GATES) ||
                selectedBlock.is(BlockTags.ANVIL) ||
                (blockEntity instanceof Container)
        ) {
            if (RecruitsServerConfig.AggroRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = player.level.getEntitiesOfClass(
                        AbstractRecruitEntity.class,
                        player.getBoundingBox().inflate(32.0D)
                );
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 1) {
                        recruits.setTarget(player);
                    }
                }

                if (list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())) {
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }

            if (RecruitsServerConfig.NeutralRecruitsBlockInteractingEvents.get()) {
                List<AbstractRecruitEntity> list = Objects.requireNonNull(player.level.getEntitiesOfClass(AbstractRecruitEntity.class, player.getBoundingBox().inflate(32.0D)));
                for (AbstractRecruitEntity recruits : list) {
                    if (canDamageTargetBlockEvent(recruits, player) && recruits.getState() == 0 && recruits.isOwned()) {
                        recruits.setTarget(player);
                    }
                }

                if (list.stream().anyMatch(recruit -> canDamageTargetBlockEvent(recruit, player) && recruit.getState() == 0 && recruit.isOwned())) {
                    warnPlayer(player, TEXT_INTERACT_WARN(list.get(0).getName().getString()));
                }
            }
        }
    }

    public boolean canDamageTargetBlockEvent(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isOwned() && target instanceof AbstractRecruitEntity recruitEntityTarget) {
            if (recruit.getOwnerUUID().equals(recruitEntityTarget.getOwnerUUID())) {
                return false;
            }
            //extra for safety
            else if (recruit.getTeam() != null && recruitEntityTarget.getTeam() != null && recruit.getTeam().equals(recruitEntityTarget.getTeam())) {
                return false;
            }
        } else if (recruit.isOwned() && target instanceof Player player) {
            if (recruit.getOwnerUUID().equals(player.getUUID())) {
                return false;
            }
        } else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getProtectUUID() != null && recruitEntityTarget.getProtectUUID() != null && recruit.getProtectUUID().equals(recruitEntityTarget.getProtectUUID())) {
            return false;
        }
        return RecruitEvents.canHarmTeamNoFriendlyFire(recruit, target);
    }

    public static boolean canDamageTarget(AbstractRecruitEntity recruit, LivingEntity target) {
        if (recruit.isOwned() && target instanceof AbstractRecruitEntity recruitEntityTarget) {
            if (recruit.getOwnerUUID().equals(recruitEntityTarget.getOwnerUUID())) {
                return false;
            } else if (recruit.equals(target)) {
                return false;
            }
            //extra for safety
            else if (recruit.getTeam() != null && recruitEntityTarget.getTeam() != null && recruit.getTeam().equals(recruitEntityTarget.getTeam())) {
                return false;
            }
        } else if (recruit.isOwned() && target instanceof Player player) {
            if (recruit.getOwnerUUID().equals(player.getUUID())) {
                return false;
            }
        } else if (target instanceof AbstractRecruitEntity recruitEntityTarget && recruit.getProtectUUID() != null && recruitEntityTarget.getProtectUUID() != null && recruit.getProtectUUID().equals(recruitEntityTarget.getProtectUUID())) {
            return false;
        }
        return RecruitEvents.canHarmTeam(recruit, target);
    }

    public static boolean canHarmTeam(LivingEntity attacker, LivingEntity target) {
        Team team = attacker.getTeam();
        Team team1 = target.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1) || team.isAllowFriendlyFire();
            //attacker can Harm target when attacker has no team
            //or attacker and target are not allied
            //or team friendly fire is true
        }
    }

    public static boolean canHarmTeamNoFriendlyFire(LivingEntity attacker, LivingEntity target) {
        Team team = attacker.getTeam();
        Team team1 = target.getTeam();
        if (team == null) {
            return true;
        } else {
            return !team.isAlliedTo(team1);
            //attacker can Harm target when attacker has no team
            //or attacker and target are not allied
            //or team friendly is true
        }
    }

    @SubscribeEvent
    public void onRecruitDeath(LivingDeathEvent event) {
        Entity target = event.getEntity();

        if (target instanceof AbstractRecruitEntity recruit) {
            if (!recruit.getIsOwned() || server.overworld().isClientSide()) return;

            //Morale loss when recruits teammate die
            UUID owner = recruit.getOwnerUUID();
            recruit.getLevel().getEntitiesOfClass(
                    AbstractRecruitEntity.class,
                    recruit.getBoundingBox().inflate(64.0D),
                    (entity) -> entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(owner)
            ).forEach((entity) -> {
                float currentMoral = entity.getMorale();
                float newMorale = currentMoral - 0.2F;
                entity.setMoral(Math.max(newMorale, 0F));
            });
        }
    }

    public byte getSavedWarning(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getByte("RecruitWarnings");
    }

    public void saveCurrentWarning(Player player, byte x) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putByte("RecruitWarnings", x);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    private void warnPlayer(Player player, Component component) {
        saveCurrentWarning(player, (byte) (getSavedWarning(player) + 1));

        if (getSavedWarning(player) >= 0) {
            player.sendSystemMessage(component);
            saveCurrentWarning(player, (byte) -10);
        }
    }

    public static MutableComponent TEXT_BLOCK_WARN(String name) {
        return Component.translatable("chat.recruits.text.block_placing_warn", name);
    }

    public static MutableComponent TEXT_INTERACT_WARN(String name) {
        return Component.translatable("chat.recruits.text.block_interact_warn", name);
    }
}
