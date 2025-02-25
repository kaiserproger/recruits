package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Supplier;

public class MessageAddRecruitToTeam implements Message<MessageAddRecruitToTeam> {

    private String teamName;
    private int x;

    public MessageAddRecruitToTeam(){
    }

    public MessageAddRecruitToTeam(String teamName, int x) {
        this.teamName = teamName;
        this.x = x;
    }

    public void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
        ServerLevel level = Objects.requireNonNull((ServerPlayer) context.get().getPlayer()).getLevel();

        TeamEvents.addNPCToData(level, teamName, x);
    }

    public MessageAddRecruitToTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.x = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeInt(this.x);
    }
}
