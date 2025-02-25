package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAnswerMessenger implements Message<MessageAnswerMessenger> {
    private UUID recruit;

    public MessageAnswerMessenger() {}

    public MessageAnswerMessenger(UUID recruit) {
        this.recruit = recruit;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context){
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());

        player.getLevel().getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D),
                (entity) -> entity.getUUID().equals(this.recruit) && entity.isAlive()
        ).forEach((messenger) -> {
            messenger.teleportWaitTimer = 100;
            player.sendSystemMessage(messenger.MESSENGER_INFO_ON_MY_WAY());
            messenger.dropDeliverItem();
            messenger.state = MessengerEntity.State.TELEPORT_BACK;
        });
    }

    public MessageAnswerMessenger fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
    }
}
