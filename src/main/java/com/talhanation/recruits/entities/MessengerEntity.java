package com.talhanation.recruits.entities;


import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.ai.UseShield;
import com.talhanation.recruits.init.ModSounds;
import com.talhanation.recruits.inventory.MessengerAnswerContainer;
import com.talhanation.recruits.inventory.MessengerContainer;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsPatrolSpawn;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Predicate;

public class MessengerEntity extends AbstractChunkLoaderEntity implements ICompanion {
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Byte> TASK_STATE = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> WAITING_TIME = SynchedEntityData.defineId(MessengerEntity.class, EntityDataSerializers.INT);
    private String ownerName = "";
    private String message = "";
    public int teleportWaitTimer;
    private int arrivedWaitTimer;
    public State state;
    public boolean targetPlayerOpened;
    public BlockPos initialPos;
    public RecruitsPlayerInfo targetPlayerInfo;

    private final Predicate<ItemEntity> ALLOWED_ITEMS = (item) ->
            (!item.hasPickUpDelay() && item.isAlive() && getInventory().canAddItem(item.getItem()) && this.wantsToPickUp(item.getItem()));

    public MessengerEntity(EntityType<? extends AbstractChunkLoaderEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_NAME, "");
        this.entityData.define(TASK_STATE, (byte) 0);
        this.entityData.define(WAITING_TIME, 0);
    }

    @Override
    protected void registerGoals() {
       super.registerGoals();
        this.goalSelector.addGoal(2, new UseShield(this));
    }

    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Message", this.getMessage());

        if(this.targetPlayerInfo != null){
            nbt.put("TargetPlayerInfo", this.targetPlayerInfo.toNBT());
        }

        nbt.putString("OwnerName", this.getOwnerName());
        nbt.putInt("waitTimer", teleportWaitTimer);
        nbt.putInt("arrivedWaitTimer", arrivedWaitTimer);
        nbt.putInt("waitingTime", this.getWaitingTime());
        if(state != null) nbt.putInt("state", state.getIndex());

        if(this.initialPos != null){
            nbt.putInt("initialPosX", this.initialPos.getX());
            nbt.putInt("initialPosY", this.initialPos.getY());
            nbt.putInt("initialPosZ", this.initialPos.getZ());
        }
    }

    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        if(nbt.contains("TargetPlayerInfo")){
            this.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(nbt.getCompound("TargetPlayerInfo")));
        }

        this.setMessage(nbt.getString("Message"));
        this.setOwnerName(nbt.getString("OwnerName"));
        this.setWaitingTime(nbt.getInt("waitingTime"));
        this.teleportWaitTimer = nbt.getInt("waitTimer");
        this.arrivedWaitTimer = nbt.getInt("arrivedWaitTimer");
        if(nbt.contains("state")){
            this.state = State.fromIndex(nbt.getInt("state"));
        }
        if(state == null) state = State.IDLE;

        if (nbt.contains("initialPosX")) {
            this.initialPos = new BlockPos(
                    nbt.getInt("initialPosX"),
                    nbt.getInt("initialPosY"),
                    nbt.getInt("initialPosZ"));
        }
    }

    //ATTRIBUTES
    public static AttributeSupplier.Builder setAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1D)
                .add(Attributes.ATTACK_DAMAGE, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(ForgeMod.REACH_DISTANCE.get(), 0D)
                .add(Attributes.ATTACK_SPEED);
    }


    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficultyInstance, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag nbt) {
        SpawnGroupData ilivingentitydata = super.finalizeSpawn(world, difficultyInstance, reason, data, nbt);
        ((GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.populateDefaultEquipmentEnchantments(difficultyInstance);

        this.initSpawn();

        return ilivingentitydata;
    }

    @Override
    public void initSpawn() {
        this.setDropEquipment();
        this.setPersistenceRequired();
        if(this.getOwner() != null)this.setOwnerName(this.getOwner().getName().getString());
        AbstractRecruitEntity.applySpawnValues(this);
    }

    @Override
    public boolean wantsToPickUp(ItemStack itemStack) {//TODO: add ranged combat
        if((itemStack.getItem() instanceof SwordItem && this.getMainHandItem().isEmpty()) ||
          (itemStack.getItem() instanceof ShieldItem) && this.getOffhandItem().isEmpty())
            return !hasSameTypeOfItem(itemStack);

        else return super.wantsToPickUp(itemStack);
    }

    public Predicate<ItemEntity> getAllowedItems(){
        return ALLOWED_ITEMS;
    }

    @Override
    public boolean canHoldItem(ItemStack itemStack){
        return !(itemStack.getItem() instanceof CrossbowItem || itemStack.getItem() instanceof BowItem); //TODO: add ranged combat
    }

    @Override
    public AbstractRecruitEntity get() {
        return this;
    }
    @Nullable
    public ServerPlayer getTargetPlayer(){
        if(this.targetPlayerInfo != null && !this.getCommandSenderWorld().isClientSide()){
            ServerLevel serverLevel = (ServerLevel) this.getCommandSenderWorld();
            return serverLevel.getServer().getPlayerList().getPlayerByName(this.targetPlayerInfo.getName());
        }
        return null;
    }

    public void openSpecialGUI(Player player) {
        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMessengerScreen(this.message, this.targetPlayerInfo));
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return MessengerEntity.this.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MessengerContainer(i, playerEntity,  MessengerEntity.this);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(this.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenSpecialScreen(player, this.getUUID()));
        }
    }

    public void openAnswerGUI(Player player) {
        if (player instanceof ServerPlayer) {
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateMessengerAnswerScreen(this.message, this.targetPlayerInfo));
            NetworkHooks.openGui((ServerPlayer) player, new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return MessengerEntity.this.getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player playerEntity) {
                    return new MessengerAnswerContainer(i, playerEntity,  MessengerEntity.this);
                }
            }, packetBuffer -> {packetBuffer.writeUUID(this.getUUID());});
        } else {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenMessengerAnswerScreen(player, this.getUUID()));
        }
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        //if(this.state != State.IDLE && !player.isCrouching()){ //For debug
        if(this.getTargetPlayer() != null && this.getTargetPlayer().getUUID().equals(player.getUUID()) && !this.getTargetPlayer().getUUID().equals(getOwnerUUID())){
            openAnswerGUI(player);
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    public String getOwnerName() {
        return entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        entityData.set(OWNER_NAME, name);
    }

    @Override
    public boolean isAtMission() {
        return this.state != State.IDLE;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RecruitsPlayerInfo setTargetPlayerInfo() {
        return this.targetPlayerInfo;
    }

    public void setTargetPlayerInfo(RecruitsPlayerInfo info) {
        targetPlayerInfo = info;
    }

    public void setWaitingTime(int x){
        this.entityData.set(WAITING_TIME, x);
    }

    public int getWaitingTime(){
        return entityData.get(WAITING_TIME);
    }

    public void start(){
        if(!this.getCommandSenderWorld().isClientSide()){
            this.initialPos = this.getOnPos();
            ServerLevel serverLevel = (ServerLevel) getCommandSenderWorld();

            MinecraftServer server = serverLevel.getServer();
            ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(this.targetPlayerInfo.getName());
            if(this.getOwner() != null){
                if(targetPlayer == null || targetPlayer.equals(this.getOwner())){
                    this.getOwner().sendMessage(PLAYER_NOT_FOUND(), this.getOwnerUUID());
                    return;
                }
                else
                    this.getOwner().sendMessage(MESSENGER_INFO_ON_MY_WAY(), this.getOwnerUUID());
            }

            this.setListen(false);
            this.setState(3);//PASSIVE
            this.teleportWaitTimer = 200;
            this.state = State.TELEPORT;
        }
    }
    @Override
    public void tick() {
        super.tick();

        if(state != null){
            switch (state){
                case IDLE -> {
                    if (this.hasEffect(MobEffects.GLOWING))
                        this.removeEffect(MobEffects.GLOWING);
                }

                case TELEPORT -> {
                    if(--teleportWaitTimer <= 0){
                        this.teleportNearTargetPlayer(getTargetPlayer());
                        this.arriveAtTargetPlayer(getTargetPlayer());
                        this.setFollowState(0);

                        this.state = State.MOVING_TO_TARGET_PLAYER;
                    }
                }

                case MOVING_TO_TARGET_PLAYER -> {
                    Player targetPlayer = getTargetPlayer();
                    if(targetPlayer != null){
                        if(this.tickCount % 20 == 0) {
                            this.getNavigation().moveTo(targetPlayer, 1);
                        }

                        double distance = this.distanceToSqr(targetPlayer);
                        if(distance <= 50){
                            if(this.getOwner() != null) this.getOwner().sendMessage(MESSENGER_ARRIVED_AT_TARGET_OWNER(), this.getOwner().getUUID());
                            if(!this.getMainHandItem().isEmpty()) targetPlayer.sendMessage(MESSENGER_INFO_AT_TARGET_WITH_ITEM(), this.getOwner().getUUID());
                            else targetPlayer.sendMessage(MESSENGER_INFO_AT_TARGET(), this.getOwner().getUUID());

                            this.setFollowState(2);
                            this.arrivedWaitTimer = 1500;
                            this.targetPlayerOpened = false;
                            this.state = State.ARRIVED;
                        }
                    }
                    else {
                        if(this.getOwner() != null) this.getOwner().sendMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER(), this.getOwner().getUUID());
                        teleportWaitTimer = 100;
                        this.state = State.TELEPORT_BACK;
                    }
                }

                case ARRIVED -> {
                    if(--arrivedWaitTimer < 0){
                        if(this.getOwner() != null) this.getOwner().sendMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER(), this.getOwner().getUUID());
                        teleportWaitTimer = 0;
                        state = State.TELEPORT_BACK;
                    }
                    if(targetPlayerOpened){
                        if (this.hasEffect(MobEffects.GLOWING))
                            this.removeEffect(MobEffects.GLOWING);
                        state = State.WAITING;
                        setWaitingTime(5 * 60 * 20);
                    }
                }

                case WAITING ->{

                    if(this.tickCount % 20 == 0) {
                        this.getNavigation().stop();
                        if(getTargetPlayer() != null) this.getLookControl().setLookAt(getTargetPlayer());
                    }

                    int time = getWaitingTime();
                    if(time > 0){
                        time--;
                        setWaitingTime(time);
                    }
                    else{
                        if(this.getOwner() != null) {
                            if(this.targetPlayerOpened) this.getOwner().sendMessage(MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED(), this.getOwner().getUUID());
                            else this.getOwner().sendMessage(MESSENGER_ARRIVED_NO_TARGET_PLAYER(), this.getOwner().getUUID());
                        }
                        teleportWaitTimer = 100;
                        state = State.TELEPORT_BACK;
                    }

                }

                case TELEPORT_BACK -> {
                    if(--teleportWaitTimer <= 0){
                        this.teleportNearOwner();
                        this.state = State.MOVING_TO_OWNER;
                    }
                }

                case MOVING_TO_OWNER -> {
                    if(this.getOwner() != null){
                        if(this.distanceToSqr(this.getOwner()) < 50F) {
                            this.setListen(true);
                            this.state = State.IDLE;
                        }
                    }
                    else{
                        teleportNearOwner();
                    }
                }
            }
        }
    }

    public void dropDeliverItem(){
        ItemStack deliverItem = this.getMainHandItem();
        if(!deliverItem.isEmpty()){
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.getInventory().setChanged();

            ItemEntity itementity = new ItemEntity(this.getCommandSenderWorld(), this.getX() + this.getLookAngle().x, this.getY() + 2.0D, this.getZ() + this.getLookAngle().z, deliverItem);
            this.getInventory().setChanged();
            this.getCommandSenderWorld().addFreshEntity(itementity);
        }
    }
    private void teleportNearOwner() {
        if(getOwner() != null && !this.getCommandSenderWorld().isClientSide()){
            BlockPos targetPos = getOwner().getOnPos();
            BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(targetPos, 10, new Random(), (ServerLevel) this.getCommandSenderWorld());
            if(tpPos == null) tpPos = targetPos;

            if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());

            this.setFollowState(1);
            this.addGlowEffect();
        }
        else {
            BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(initialPos, 10, new Random(), (ServerLevel) this.getCommandSenderWorld());
            if(tpPos == null) tpPos = initialPos;

            if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());

            this.setHoldPos(Vec3.atCenterOf(initialPos));
            this.setFollowState(3);
        }
    }
    private void teleportNearTargetPlayer(Player player) {
        if(player != null && !this.getCommandSenderWorld().isClientSide()){
            BlockPos targetPos = player.getOnPos();
            BlockPos tpPos = RecruitsPatrolSpawn.func_221244_a(targetPos, 20, new Random(), (ServerLevel) this.getCommandSenderWorld());
            if(tpPos == null) tpPos = targetPos;

            if(this.getVehicle() instanceof AbstractHorse horse) horse.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
            else this.teleportTo(tpPos.getX(), tpPos.getY(), tpPos.getZ());
        }
    }

    private void addGlowEffect() {
        this.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60*20*3, 1, false, false, true));
    }

    private void playHornSound() {
        this.level.playSound(null, this.getX(), this.getY() + 1 , this.getZ(), ModSounds.MESSENGER_HORN.get(), this.getSoundSource(), 5.0F, 0.8F + 0.4F * this.random.nextFloat());
    }

    public void arriveAtTargetPlayer(ServerPlayer target){
        this.addGlowEffect();
        this.playHornSound();
        this.tellTargetPlayerArrived(target);
    }

    public void tellTargetPlayerArrived(ServerPlayer target){
        Team ownerTeam = this.getTeam();
        if(ownerTeam != null )target.sendMessage(MESSENGER_ARRIVED_TEAM(this.getOwnerName(), ownerTeam.getName()), target.getUUID());
        else target.sendMessage(MESSENGER_ARRIVED(this.getOwnerName()), target.getUUID());
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(()-> target), new MessageToClientSetToast(1, this.getOwnerName()));
    }

    private MutableComponent PLAYER_NOT_FOUND(){
        return new TranslatableComponent("chat.recruits.text.messenger_player_not_found", this.getName().getString());
    }

    private MutableComponent MESSENGER_ARRIVED(String ownerName){
        return new TranslatableComponent("chat.recruits.text.messenger_arrived_at_target", this.getName().getString(), ownerName);
    }

    private MutableComponent MESSENGER_ARRIVED_TEAM(String ownerName, String teamName){
        return new TranslatableComponent("chat.recruits.text.messenger_arrived_at_target_team", this.getName().getString(), ownerName, teamName);
    }

    private MutableComponent MESSENGER_ARRIVED_AT_TARGET_OWNER(){
        return new TranslatableComponent("chat.recruits.text.messenger_arrived_at_target_owner", this.getName().getString(), this.targetPlayerInfo.getName());
    }

    private MutableComponent MESSENGER_INFO_AT_TARGET(){

        return new TranslatableComponent("chat.recruits.text.messenger_info_to_target", this.getName().getString(), this.getOwnerName());
    }
    private MutableComponent MESSENGER_INFO_AT_TARGET_WITH_ITEM(){
        return new TranslatableComponent("chat.recruits.text.messenger_info_to_target_with_item", this.getName().getString(), this.getOwnerName());

    }

    public MutableComponent MESSENGER_INFO_ON_MY_WAY(){
        return new TranslatableComponent("chat.recruits.text.messenger_info_on_my_way", this.getName().getString());
    }

    private MutableComponent MESSENGER_ARRIVED_NO_TARGET_PLAYER(){
        return new TranslatableComponent("chat.recruits.text.messenger_arrived_no_player", this.getName().getString(), this.targetPlayerInfo.getName());
    }

    private MutableComponent MESSENGER_ARRIVED_TARGET_PLAYER_NOT_ANSWERED(){
        return new TranslatableComponent("chat.recruits.text.messenger_target_player_not_answered", this.getName().getString());
    }

    public enum State{
        IDLE(0),
        TELEPORT(1),
        MOVING_TO_TARGET_PLAYER(2),
        ARRIVED(3),
        WAITING(4),
        TELEPORT_BACK(5),
        MOVING_TO_OWNER(6);


        private final int index;
        State(int index){
            this.index = index;
        }

        public int getIndex(){
            return this.index;
        }

        public static State fromIndex(int index) {
            for (State state : State.values()) {
                if (state.getIndex() == index) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid State index: " + index);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource dmg, float amt) {
        if(this.state == null || this.state == State.IDLE){
            return super.hurt(dmg, amt);
        }
        else return false;
    }
}










