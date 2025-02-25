package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.*;
import java.util.function.Supplier;

public class MessageApplyNoGroup implements Message<MessageApplyNoGroup> {

    private UUID owner;
    private int groupID;

    public MessageApplyNoGroup(){}

    public MessageApplyNoGroup(UUID owner, int groupID) {
        this.owner = owner;
        this.groupID = groupID;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());

        List<AbstractRecruitEntity> recruitList = new ArrayList<>();

        if(player.getCommandSenderWorld() instanceof ServerLevel serverLevel) {
            for(Entity entity : serverLevel.getAllEntities()){
                if(entity instanceof AbstractRecruitEntity recruit && recruit.isEffectedByCommand(owner, groupID))
                    recruitList.add(recruit);
            }
        }

        for(AbstractRecruitEntity recruit : recruitList){
            recruit.setGroup(0);
        }
    }
    public MessageApplyNoGroup fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.groupID = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeInt(this.groupID);
    }

}