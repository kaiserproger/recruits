package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAggroGui implements Message<MessageAggroGui> {

    private int state;
    private UUID uuid;

    public MessageAggroGui() {
    }

    public MessageAggroGui(int state, UUID uuid) {
        this.state = state;
        this.uuid = uuid;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());

        player.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(16.0D),
                (recruit) -> recruit.getUUID().equals(this.uuid)
        ).forEach((recruit) -> recruit.setState(this.state));
    }

    public MessageAggroGui fromBytes(FriendlyByteBuf buf) {
        this.state = buf.readInt();
        this.uuid = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(state);
        buf.writeUUID(uuid);
    }
}
