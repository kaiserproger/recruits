package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class MessageAggro implements Message<MessageAggro> {

    private UUID player;
    private UUID recruit;
    private int state;
    private int group;
    private boolean fromGui;


    public MessageAggro() {
    }

    public MessageAggro(UUID player, int state, int group) {
        this.player = player;
        this.state = state;
        this.group = group;
        this.fromGui = false;
        this.recruit = null;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());

        double boundBoxInflateModifier = 16.0D;
        if(!fromGui) {
            boundBoxInflateModifier = 100.0D;
        }

        player.getLevel().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(boundBoxInflateModifier)
        ).forEach((recruit) -> {
            if (fromGui && !recruit.getUUID().equals(this.recruit)) {
                return;
            }

            CommandEvents.onAggroCommand(this.player, recruit, this.state, group, fromGui);
        });
    }

    public MessageAggro fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readInt();
        if (this.recruit != null) this.recruit = buf.readUUID();
        this.fromGui = buf.readBoolean();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
        buf.writeInt(this.group);
        buf.writeBoolean(this.fromGui);
        if (this.recruit != null) buf.writeUUID(this.recruit);
    }
}