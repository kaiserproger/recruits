package com.talhanation.recruits.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public interface Message<T extends Message>{
    default void executeServerSide(Supplier<NetworkManager.PacketContext> context) {
    }

    default void executeClientSide(Supplier<NetworkManager.PacketContext> context) {
    }

    T fromBytes(FriendlyByteBuf var1);

    void toBytes(FriendlyByteBuf var1);
}
