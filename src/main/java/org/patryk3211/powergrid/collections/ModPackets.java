package org.patryk3211.powergrid.collections;

import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.network.PacketSet;
import org.patryk3211.powergrid.network.packets.*;

public class ModPackets {
    public static final PacketSet PACKETS = PacketSet.builder(PowerGrid.MOD_ID, 16) // increment version on changes

            // Client to Server
            .c2s(AlternatePlacementStatusC2SPacket.class, AlternatePlacementStatusC2SPacket::new)
            .c2s(BlockWireCutC2SPacket.class, BlockWireCutC2SPacket::new)
            .c2s(BlockWireAttachC2SPacket.class, BlockWireAttachC2SPacket::new)
            .c2s(CordDetachC2SPacket.class, CordDetachC2SPacket::new)
            .c2s(SaveSchematicC2SPacket.class, SaveSchematicC2SPacket::new)
            .c2s(SaveCardC2SPacket.class, SaveCardC2SPacket::new)
            .c2s(SetCustomDisplayC2SPacket.class, SetCustomDisplayC2SPacket::new)
            .c2s(TransformerWindingC2SPacket.class, TransformerWindingC2SPacket::new)
            .c2s(ChangeScreenC2SPacket.class, ChangeScreenC2SPacket::new)
            .c2s(EndpointTrackingC2SPacket.class, EndpointTrackingC2SPacket::new)
            .c2s(MultimeterDataC2SPacket.class, MultimeterDataC2SPacket::new)
            .c2s(UpdateComponentBiPacket.class, UpdateComponentBiPacket::new)
            .c2s(NegotiateSyncC2SPacket.class, NegotiateSyncC2SPacket::new)

            // Server to Client
            .s2c(ZapProjectileS2CPacket.class, ZapProjectileS2CPacket::new)
            .s2c(LightningSyncS2CPacket.class, LightningSyncS2CPacket::new)
            .s2c(EntityDataS2CPacket.class, EntityDataS2CPacket::new)
            .s2c(StateS2CPacket.class, StateS2CPacket::new)
            .s2c(UpdateComponentBiPacket.class, UpdateComponentBiPacket::new)

            .build();
}
