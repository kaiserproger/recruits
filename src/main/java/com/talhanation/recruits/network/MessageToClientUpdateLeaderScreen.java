package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.PatrolLeaderScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;


public class MessageToClientUpdateLeaderScreen implements Message<MessageToClientUpdateLeaderScreen> {
    public List<BlockPos> waypoints;
    public List<ItemStack> waypointItems;
    public boolean isStarted;

    public MessageToClientUpdateLeaderScreen() {
    }

    public MessageToClientUpdateLeaderScreen(List<BlockPos> waypoints, List<ItemStack> waypointItems) {
        this.waypoints = waypoints;
        this.waypointItems = waypointItems;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        PatrolLeaderScreen.waypoints = this.waypoints;
        PatrolLeaderScreen.waypointItems = this.waypointItems;
    }

    @Override
    public MessageToClientUpdateLeaderScreen fromBytes(FriendlyByteBuf buf) {
        this.waypoints = buf.readList(FriendlyByteBuf::readBlockPos);
        this.waypointItems = buf.readList(FriendlyByteBuf::readItem);
//        this.isStarted = buf.readBoolean();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(waypoints, FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(waypointItems, FriendlyByteBuf::writeItem);
//      buf.writeBoolean(this.isStarted);
    }
}

