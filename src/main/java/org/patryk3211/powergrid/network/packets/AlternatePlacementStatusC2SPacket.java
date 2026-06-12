package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.electricity.wire.IAlternatePlacementExtension;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class AlternatePlacementStatusC2SPacket implements SimplePacket {
    private final boolean status;

    public AlternatePlacementStatusC2SPacket(boolean status) {
        this.status = status;
    }

    public AlternatePlacementStatusC2SPacket(FriendlyByteBuf buf) {
        status = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(status);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            if(ctx.getPlayer() instanceof IAlternatePlacementExtension ext) {
                ext.powerGrid$setAlternatePlacement(status);
            }
        });
    }
}
