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

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.PacketByteBuf;
import org.patryk3211.powergrid.electricity.ClientElectricNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

public class TransmissionLineS2CPacket extends SimplePacketBase {
    public final IWireEndpoint endpoint1;
    public final IWireEndpoint endpoint2;
    public final float lineResistance;
    public final float node1Voltage;
    public final float node2Voltage;

    public TransmissionLineS2CPacket(TransmissionLine line) {
        this.endpoint1 = ((OwnedFloatingNode) line.getNode1()).endpoint;
        this.endpoint2 = ((OwnedFloatingNode) line.getNode2()).endpoint;
        this.lineResistance = (float) line.getResistance();
        this.node1Voltage = line.getNode1().getVoltage();
        this.node2Voltage = line.getNode2().getVoltage();
    }

    public TransmissionLineS2CPacket(PacketByteBuf buf) {
        this.endpoint1 = WireEndpointType.deserialize(buf.readNbt());
        this.endpoint2 = WireEndpointType.deserialize(buf.readNbt());
        this.lineResistance = buf.readFloat();
        this.node1Voltage = buf.readFloat();
        this.node2Voltage = buf.readFloat();
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeNbt(endpoint1.serialize());
        buf.writeNbt(endpoint2.serialize());
        buf.writeFloat(lineResistance);
        buf.writeFloat(node1Voltage);
        buf.writeFloat(node2Voltage);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean handle(Context context) {
        context.enqueueWork(() -> ClientElectricNetwork.partialTrackedLine(this));
        return true;
    }
}
