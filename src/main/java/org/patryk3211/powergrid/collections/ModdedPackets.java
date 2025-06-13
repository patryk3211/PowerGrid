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
package org.patryk3211.powergrid.collections;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import me.pepperbell.simplenetworking.SimpleChannel;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @see com.simibubi.create.AllPackets
 */
public class ModdedPackets {
    public static final Identifier CHANNEL_NAME = PowerGrid.asResource("main");
    private static final List<PacketType<?>> allPackets = new ArrayList<>();
    private static SimpleChannel channel;

    // TODO: Move all packets to the simple packet thing
    public static final Identifier ENTITY_DATA_PACKET = new Identifier(PowerGrid.MOD_ID, "entity_data");
    public static final Identifier AGGREGATE_COILS_PACKET = new Identifier(PowerGrid.MOD_ID, "aggregate_coils");

    public static final Identifier BLOCK_WIRE_CUT = PowerGrid.asResource("block_wire_cut");
    public static final Identifier BLOCK_WIRE_ATTACH = PowerGrid.asResource("block_wire_attach");

    public static final PacketType<ElectroZapperPacket> ELECTRO_ZAPPER_SHOOT = register(ElectroZapperPacket.class, ElectroZapperPacket::new, SimplePacketBase.NetworkDirection.PLAY_TO_CLIENT);

    private static <T extends SimplePacketBase> PacketType<T> register(Class<T> type, Function<PacketByteBuf, T> factory, SimplePacketBase.NetworkDirection direction) {
        var packetType = new PacketType<>(type, factory, direction);
        allPackets.add(packetType);
        return packetType;
    }

    public static void registerPackets() {
        channel = new SimpleChannel(CHANNEL_NAME);
        for(var packet : allPackets)
            packet.register();
    }

    public static SimpleChannel getChannel() {
        return channel;
    }

    public static class PacketType<T extends SimplePacketBase> {
        private static int index = 0;

        private Function<PacketByteBuf, T> decoder;
        private Class<T> type;
        private SimplePacketBase.NetworkDirection direction;

        private PacketType(Class<T> type, Function<PacketByteBuf, T> factory, SimplePacketBase.NetworkDirection direction) {
            decoder = factory;
            this.type = type;
            this.direction = direction;
        }

        private void register() {
            switch (direction) {
                case PLAY_TO_CLIENT -> getChannel().registerS2CPacket(type, index++, decoder);
                case PLAY_TO_SERVER -> getChannel().registerC2SPacket(type, index++, decoder);
            }
        }
    }
}
