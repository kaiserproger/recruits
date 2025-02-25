package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAssignGroupToTeamMate implements Message<MessageAssignGroupToTeamMate> {
    private UUID owner;
    private UUID newOwner;
    private UUID recruit;

    public MessageAssignGroupToTeamMate(){}

    public MessageAssignGroupToTeamMate(UUID owner, UUID newOwner, UUID recruit) {
        this.owner = owner;
        this.newOwner = newOwner;
        this.recruit = recruit;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());
        List<AbstractRecruitEntity> list = player.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100D),
                (recruit) -> recruit.getOwnerUUID() != null && recruit.getOwnerUUID().equals(owner)
        );

        int group = -1;

        for (AbstractRecruitEntity recruit1 : list){
            if(recruit1.getUUID().equals(recruit)){
                group = recruit1.getGroup();
                break;
            }
        }

        for (AbstractRecruitEntity recruit : list) {
            UUID recruitOwner = recruit.getOwnerUUID();
            if (recruitOwner != null && recruitOwner.equals(owner) && recruit.getGroup() == group)
                TeamEvents.assignToTeamMate(player, newOwner, recruit);
        }
    }
    
    public MessageAssignGroupToTeamMate fromBytes(FriendlyByteBuf buf) {
        this.owner = buf.readUUID();
        this.newOwner = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.owner);
        buf.writeUUID(this.newOwner);
        buf.writeUUID(this.recruit);
    }
}