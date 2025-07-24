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
package org.patryk3211.powergrid.electricity;

import com.google.common.base.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.OwnedElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.network.packets.TransmissionLineS2CPacket;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ClientWorldNetworks extends WorldNetworks {
    private final Map<PhantomLine, PhantomLineData> phantomLines = new HashMap<>();

    public ClientWorldNetworks(World world) {
        super(world);
    }

    @Override
    public void tick() {
        super.tick();
        var iter = phantomLines.values().iterator();
        while(iter.hasNext()) {
            var data = iter.next();
            if(data.age++ >= 5) {
                data.remove();
                iter.remove();
            }
        }
    }

    @Override
    public @Nullable ElectricWire makeTransmissionLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var result = super.makeTransmissionLine(endpoint1, endpoint2, forEntity);
        if(result instanceof TransmissionLinePart part) {
            // Delete all phantom lines
            phantomLines.forEach((key, line) -> line.remove());
            phantomLines.clear();

            var line = part.getLine();
            if(line.getNode1() instanceof OwnedFloatingNode node1 && line.getNode2() instanceof OwnedFloatingNode node2) {
                var key1 = new PhantomLine(node1.endpoint, node2.endpoint);
                if(phantomLines.containsKey(key1)) {
                    phantomLines.remove(key1).remove();
                }
                var key2 = new PhantomLine(node2.endpoint, node1.endpoint);
                if(phantomLines.containsKey(key2)) {
                    phantomLines.remove(key2).remove();
                }
            }
        }
        return result;
    }

    public void partialLine(TransmissionLineS2CPacket packet) {
       if(packet.endpoint1.isValid(world) && packet.endpoint2.isValid(world)) {
            var node1 = packet.endpoint1.getNode(world);
            var node2 = packet.endpoint2.getNode(world);

            var wires = globalGraph.getWires(node1, node2);
            for (var wire : wires) {
                if (wire instanceof TransmissionLine)
                    // Packet is not needed, the nodes are actually connected.
                    return;
            }
            if(node1.getNetwork() == null) {
                var network = newNetwork();
                packet.endpoint1.joinNetwork(world, network);
            }
            var line1 = getPhantomLine(packet.endpoint1, packet.endpoint2, packet.lineResistance);
            line1.source.setVoltage(packet.node2Voltage);
            if(node2.getNetwork() != null) {
                var network = newNetwork();
                packet.endpoint2.joinNetwork(world, network);
            }
            var line2 = getPhantomLine(packet.endpoint2, packet.endpoint1, packet.lineResistance);
            line2.source.setVoltage(packet.node1Voltage);
        } else if(packet.endpoint1.isValid(world)) {
            var line = getPhantomLine(packet.endpoint1, packet.endpoint2, packet.lineResistance);
            line.source.setVoltage(packet.node2Voltage);
        } else if(packet.endpoint2.isValid(world)) {
            var line = getPhantomLine(packet.endpoint2, packet.endpoint1, packet.lineResistance);
            line.source.setVoltage(packet.node1Voltage);
        }
    }

    private PhantomLineData getPhantomLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, float resistance) {
        var line = phantomLines.computeIfAbsent(new PhantomLine(endpoint1, endpoint2), key -> new PhantomLineData(endpoint1.getNode(world), resistance));
        if(line.wire.getResistance() != resistance)
            line.wire.setResistance(resistance);
        line.age = 0;
        return line;
    }

    private record PhantomLine(IWireEndpoint endpoint, IWireEndpoint otherEndpoint) {
        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;
            if(obj instanceof PhantomLine line) {
                return endpoint.equals(line.endpoint) && otherEndpoint.equals(line.otherEndpoint);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(endpoint, otherEndpoint);
        }
    }

    private static class PhantomLineData {
        public final VoltageSourceNode source;
        public final ElectricWire wire;
        public final IElectricNode node;
        public int age;

        public PhantomLineData(IElectricNode node, float resistance) {
            assert node.getNetwork() != null;
            this.node = node;
            this.source = new VoltageSourceNode();
            this.wire = new ElectricWire(resistance, node, source);
            this.age = 0;

            var network = node.getNetwork();
            network.addNode(source);
            network.addWire(wire);
        }

        public void remove() {
            source.getNetwork().removeNode(source);
            wire.remove();
        }
    }
}
