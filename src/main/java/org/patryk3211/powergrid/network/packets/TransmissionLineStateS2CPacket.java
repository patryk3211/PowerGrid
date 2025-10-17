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

import java.util.function.Supplier;

public class TransmissionLineStateS2CPacket implements SimplePacket {
    public final int lineId;
    public final IWireEndpoint endpoint1;
    public final IWireEndpoint endpoint2;
    public final float lineResistance;
    public final float node1Voltage;
    public final float node2Voltage;

    public TransmissionLineStateS2CPacket(TransmissionLine line) {
        this.lineId = line.getId();
        this.endpoint1 = line.getNode1().endpoint;
        this.endpoint2 = line.getNode2().endpoint;
        this.lineResistance = (float) line.getResistance();
        this.node1Voltage = line.getNode1().getVoltage();
        this.node2Voltage = line.getNode2().getVoltage();
    }

    public TransmissionLineStateS2CPacket(FriendlyByteBuf buf) {
        this.lineId = buf.readInt();
        this.endpoint1 = WireEndpointType.deserialize(buf.readNbt());
        this.endpoint2 = WireEndpointType.deserialize(buf.readNbt());
        this.lineResistance = buf.readFloat();
        this.node1Voltage = buf.readFloat();
        this.node2Voltage = buf.readFloat();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(lineId);
        buf.writeNbt(endpoint1.serialize());
        buf.writeNbt(endpoint2.serialize());
        buf.writeFloat(lineResistance);
        buf.writeFloat(node1Voltage);
        buf.writeFloat(node2Voltage);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> ClientElectricNetwork.partialTrackedLine(this));
    }
}
