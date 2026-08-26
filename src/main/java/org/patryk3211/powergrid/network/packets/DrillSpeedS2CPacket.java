package org.patryk3211.powergrid.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.equipment.drill.PlayerDrillExtensions;
import org.patryk3211.powergrid.network.S2CPacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

public class DrillSpeedS2CPacket implements S2CPacket {
    private final int speed;

    public DrillSpeedS2CPacket(int speed) {
        this.speed = speed;
    }

    public DrillSpeedS2CPacket(FriendlyByteBuf buf) {
        this.speed = buf.readByte();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(speed);
    }

    @Override
    public void handle(Minecraft mc) {
        var player = ClientSideAccess.player();
        if(player instanceof PlayerDrillExtensions ext) {
            ext.powerGrid$receiveSpeed(speed);
        }
    }
}
