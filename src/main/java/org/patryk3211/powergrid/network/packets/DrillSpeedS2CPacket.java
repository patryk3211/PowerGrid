package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.equipment.drill.PlayerDrillExtensions;
import org.patryk3211.powergrid.network.SimplePacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

import java.util.function.Supplier;

public class DrillSpeedS2CPacket implements SimplePacket {
    private final int speed;

    public DrillSpeedS2CPacket(int speed) {
        this.speed = speed;
    }

    public DrillSpeedS2CPacket(FriendlyByteBuf buf) {
        this.speed = buf.readByte();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(speed);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var player = ClientSideAccess.player();
            if(player instanceof PlayerDrillExtensions ext) {
                ext.powerGrid$receiveSpeed(speed);
            }
        });
    }
}
