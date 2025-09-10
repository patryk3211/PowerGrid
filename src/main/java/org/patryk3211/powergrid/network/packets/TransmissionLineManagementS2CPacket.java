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
package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.electricity.ClientElectricNetwork;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.Collection;
import java.util.function.Supplier;

public class TransmissionLineManagementS2CPacket implements SimplePacket {
    public record Entry(int id, double resistance, IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        public static Entry of(TransmissionLine line) {
            return new Entry(line.getId(), line.getResistance(), line.getNode1().endpoint, line.getNode2().endpoint);
        }
    }

    private final IWireEndpoint endpoint;
    private final Entry[] entries;

    public TransmissionLineManagementS2CPacket(IWireEndpoint endpoint, Collection<TransmissionLine> lines) {
        this.endpoint = endpoint;
        entries = new Entry[lines.size()];
        var i = 0;
        for(var line : lines) {
            entries[i++] = Entry.of(line);
        }
    }

    public TransmissionLineManagementS2CPacket(FriendlyByteBuf buf) {
        endpoint = WireEndpointType.deserialize(buf.readNbt());
        var count = buf.readByte();
        entries = new Entry[count];
        for(int i = 0; i < count; ++i) {
            entries[i] = new Entry(
                    buf.readInt(),
                    buf.readDouble(),
                    WireEndpointType.deserialize(buf.readNbt()),
                    WireEndpointType.deserialize(buf.readNbt())
            );
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        if(entries.length > Byte.MAX_VALUE)
            throw new IllegalStateException("Too many lines for one packet");
        buf.writeNbt(endpoint.serialize());
        buf.writeByte(entries.length);
        for(var entry : entries) {
            buf.writeInt(entry.id);
            buf.writeDouble(entry.resistance);
            buf.writeNbt(entry.endpoint1.serialize());
            buf.writeNbt(entry.endpoint2.serialize());
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            ClientElectricNetwork.getWorldNetworks().lineManagement(endpoint, entries);
        });
    }
}
