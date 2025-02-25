package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAssignToTeamMate implements Message<MessageAssignToTeamMate> {

    private UUID recruit;
    private UUID newOwner;

    public MessageAssignToTeamMate() {
    }

    public MessageAssignToTeamMate(UUID recruit, UUID newOwner) {
        this.recruit = recruit;
        this.newOwner = newOwner;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(64.0D),
                (entity) -> entity.getUUID().equals(this.recruit) && entity.isAlive()
        ).forEach((recruit) -> TeamEvents.assignToTeamMate(player, newOwner, recruit));
    }

    public MessageAssignToTeamMate fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.newOwner = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeUUID(this.newOwner);
    }
}