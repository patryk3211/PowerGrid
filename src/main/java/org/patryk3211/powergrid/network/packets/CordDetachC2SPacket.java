package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.electricity.wire.powercord.CordEntity;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class CordDetachC2SPacket implements SimplePacket {
    private final int entityId;
    private final boolean secondEndpoint;

    public CordDetachC2SPacket(CordEntity entity, boolean secondEndpoint) {
        this.entityId = entity.getId();
        this.secondEndpoint = secondEndpoint;
    }

    public CordDetachC2SPacket(FriendlyByteBuf buf) {
        entityId = buf.readInt();
        secondEndpoint = buf.readBoolean();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(secondEndpoint);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            var level = player.level();
            var entity = level.getEntity(entityId);
            if(!(entity instanceof CordEntity cord))
                return;
            cord.cordDetach(player, secondEndpoint);
        });
    }
}
