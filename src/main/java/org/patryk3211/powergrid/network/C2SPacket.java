package org.patryk3211.powergrid.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface C2SPacket {
    void write(FriendlyByteBuf buf);
    void handle(ServerPlayer player);
}
