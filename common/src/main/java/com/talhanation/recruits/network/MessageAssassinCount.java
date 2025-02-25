package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AssassinLeaderEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAssassinCount implements Message<MessageAssassinCount> {
    private int count;
    private UUID uuid;

    public MessageAssassinCount(){
    }

    public MessageAssassinCount(int count, UUID uuid) {
        this.count = count;
        this.uuid = uuid;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context){
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());
        player.getLevel().getEntitiesOfClass(
                AssassinLeaderEntity.class,
                player.getBoundingBox().inflate(16.0D),
                (leader) -> leader.getUUID().equals(this.uuid)
        ).forEach((leader) -> leader.setCount(this.count));
    }
    public MessageAssassinCount fromBytes(FriendlyByteBuf buf) {
        this.count = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(count);
        buf.writeUUID(uuid);
    }

}