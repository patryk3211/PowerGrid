package org.patryk3211.powergrid.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.patryk3211.powergrid.electricity.wire.powercord.CordEntity;
import org.patryk3211.powergrid.network.C2SPacket;

public class CordDetachC2SPacket implements C2SPacket {
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
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(secondEndpoint);
    }

    @Override
    public void handle(ServerPlayer player) {
        var level = player.level();
        var entity = level.getEntity(entityId);
        if(!(entity instanceof CordEntity cord))
            return;
        cord.cordDetach(player, secondEndpoint);
    }
}
