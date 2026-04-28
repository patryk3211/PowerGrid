/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.equipment.zapper;


import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.collections.ModdedPackets;

public class ElectroZapperS2CPacket implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ElectroZapperS2CPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.location.x);
                        buf.writeDouble(pkt.location.y);
                        buf.writeDouble(pkt.location.z);
                        buf.writeEnum(pkt.hand);
                        buf.writeBoolean(pkt.self);
                    },
                    buf -> new ElectroZapperS2CPacket(
                            new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                            buf.readEnum(InteractionHand.class),
                            buf.readBoolean()
                    )
            );

    private final Vec3 location;
    private final InteractionHand hand;
    private final boolean self;

    public ElectroZapperS2CPacket(Vec3 location, InteractionHand hand, boolean self) {
        this.location = location;
        this.hand = hand;
        this.self = self;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return ModdedPackets.ELECTRO_ZAPPER_SHOOT;
    }

    @Override
    public void handle(LocalPlayer player) {
        if (player.position().distanceTo(location) > 100)
            return;

        var handler = PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER;
        if (self)
            handler.shoot(hand, location);
        else
            handler.playSound(hand, location);
    }
}
