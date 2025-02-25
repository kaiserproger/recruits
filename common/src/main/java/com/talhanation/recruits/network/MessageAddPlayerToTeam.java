package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Supplier;

public class MessageAddPlayerToTeam implements Message<MessageAddPlayerToTeam> {

    private String teamName;
    private String namePlayerToAdd;

    public MessageAddPlayerToTeam(){
    }

    public MessageAddPlayerToTeam(String teamName, String namePlayerToAdd) {
        this.teamName = teamName;
        this.namePlayerToAdd = namePlayerToAdd;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerPlayer player = Objects.requireNonNull((ServerPlayer) context.get().getPlayer());
        ServerLevel world = player.getLevel();

        TeamEvents.addPlayerToTeam(player, world, this.teamName, this.namePlayerToAdd);
    }

    public MessageAddPlayerToTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.namePlayerToAdd = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeUtf(this.namePlayerToAdd);
    }
}
