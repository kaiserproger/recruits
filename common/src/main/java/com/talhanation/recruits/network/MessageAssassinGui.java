package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AssassinLeaderEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAssassinGui implements Message<MessageAssassinGui> {
    private UUID uuid;
    private UUID recruit;

    public MessageAssassinGui() {
        this.uuid = new UUID(0, 0);
    }

    public MessageAssassinGui(Player player, UUID recruit) {
        this.uuid = player.getUUID();
        this.recruit = recruit;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());
        if (!player.getUUID().equals(uuid)) {
            return;
        }

        player.getLevel().getEntitiesOfClass(
                AssassinLeaderEntity.class,
                player.getBoundingBox().inflate(16.0D),
                v -> v.getUUID().equals(this.recruit) && v.isAlive()
        ).forEach((recruit) -> recruit.openGUI(player));
    }

    public MessageAssassinGui fromBytes(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(recruit);
    }
}