package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.PatrolLeaderScreen;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public class MessageToClientUpdateLeaderScreen implements Message<MessageToClientUpdateLeaderScreen> {
    public List<BlockPos> waypoints;
    public List<ItemStack> waypointItems;
    public int size;

    public MessageToClientUpdateLeaderScreen() {
    }

    public MessageToClientUpdateLeaderScreen(List<BlockPos> waypoints, List<ItemStack> waypointItems, int size) {
        this.waypoints = waypoints;
        this.waypointItems = waypointItems;
        this.size = size;
    }

    public void executeClientSide(Supplier<NetworkManager.PacketContext> context) {
        PatrolLeaderScreen.waypoints = this.waypoints;
        PatrolLeaderScreen.waypointItems = this.waypointItems;
        PatrolLeaderScreen.recruitsSize = this.size;
    }

    public MessageToClientUpdateLeaderScreen fromBytes(FriendlyByteBuf buf) {
        this.waypoints = buf.readList(FriendlyByteBuf::readBlockPos);
        this.waypointItems = buf.readList(FriendlyByteBuf::readItem);
        this.size = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(waypoints, FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(waypointItems, FriendlyByteBuf::writeItem);
        buf.writeInt(this.size);
    }
}

