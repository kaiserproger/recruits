package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageClearTarget implements Message<MessageClearTarget> {
    private UUID uuid;
    private int group;

    public MessageClearTarget(){}

    public MessageClearTarget(UUID uuid, int group) {
        this.uuid = uuid;
        this.group = group;

    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context){
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());

        List<AbstractRecruitEntity> list = player.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100)
        );

        for (AbstractRecruitEntity recruits : list) {
            CommandEvents.onClearTargetButton(uuid, recruits, group);
        }
    }
    public MessageClearTarget fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.group = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(group);
    }
}

