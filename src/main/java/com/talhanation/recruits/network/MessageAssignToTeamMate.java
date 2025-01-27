package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAssignToTeamMate implements Message<MessageAssignToTeamMate> {

    private UUID recruit;

    public MessageAssignToTeamMate() {
    }

    public MessageAssignToTeamMate(UUID recruit) {
        this.recruit = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer serverPlayer = Objects.requireNonNull(context.getSender());
        serverPlayer.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                serverPlayer.getBoundingBox().inflate(64.0D),
                (recruit) -> recruit.getUUID().equals(this.recruit)
        ).forEach(
                (recruit) -> TeamEvents.assignToTeamMate(serverPlayer, recruit)
        );
    }

    public MessageAssignToTeamMate fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
    }
}