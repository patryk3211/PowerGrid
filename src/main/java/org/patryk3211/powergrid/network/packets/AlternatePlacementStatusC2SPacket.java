package org.patryk3211.powergrid.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.patryk3211.powergrid.electricity.wire.IAlternatePlacementExtension;
import org.patryk3211.powergrid.network.C2SPacket;

public class AlternatePlacementStatusC2SPacket implements C2SPacket {
    private final boolean status;

    public AlternatePlacementStatusC2SPacket(boolean status) {
        this.status = status;
    }

    public AlternatePlacementStatusC2SPacket(FriendlyByteBuf buf) {
        status = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(status);
    }

    @Override
    public void handle(ServerPlayer player) {
        if(player instanceof IAlternatePlacementExtension ext) {
            ext.powerGrid$setAlternatePlacement(status);
        }
    }
}
