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
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.network.packets.EndpointTrackingC2SPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineManagementS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineStateS2CPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ClientWorldNetworks extends WorldNetworks {
    private final Map<PhantomLine, PhantomLineData> phantomLines = new HashMap<>();

    public ClientWorldNetworks(Level world) {
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
        throw new IllegalCallerException("Not on the client");
    }

    private void removePhantomLines(TransmissionLine line) {
        // TODO: I don't remember why all lines are getting deleted, this needs verification.
        // Delete all phantom lines
        phantomLines.forEach((key, line2) -> line2.remove());
        phantomLines.clear();

        var node1 = line.getNode1();
        var node2 = line.getNode2();
        var key1 = new PhantomLine(node1.endpoint, node2.endpoint);
        if(phantomLines.containsKey(key1)) {
            phantomLines.remove(key1).remove();
        }
        var key2 = new PhantomLine(node2.endpoint, node1.endpoint);
        if(phantomLines.containsKey(key2)) {
            phantomLines.remove(key2).remove();
        }
    }

    public void partialLine(TransmissionLineStateS2CPacket packet) {
       if(packet.endpoint1.isValid(world) && packet.endpoint2.isValid(world)) {
            var node1 = packet.endpoint1.getNode(world);
            var node2 = packet.endpoint2.getNode(world);

            var wires = globalGraph.getWires(node1, node2);
            for (var wire : wires) {
                if (wire instanceof TransmissionLine)
                    // Packet is not needed, the nodes are actually connected.
                    return;
            }
            var line1 = getPhantomLine(packet.endpoint1, packet.endpoint2, packet.lineResistance);
            line1.source.setVoltage(packet.node2Voltage);

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
        var targetNode = endpoint1.getNode(world);
        if(targetNode.getNetwork() == null) {
            var network = newNetwork();
            endpoint1.joinNetwork(world, network);
        }
        var line = phantomLines.computeIfAbsent(new PhantomLine(endpoint1, endpoint2),
                key -> new PhantomLineData(targetNode, resistance));
        if(line.wire.getResistance() != resistance)
            line.wire.setResistance(resistance);
        line.age = 0;
        return line;
    }

    @Override
    public void lineConnected(TransmissionLine line) {
        transmissionLines.put(line.getId(), line);
    }

    @Override
    public void lineDisconnected(TransmissionLine line) {
        transmissionLines.remove(line.getId());
        islandDiscoveryQueue.add(line.getNetwork());
    }

    @Override
    public void nodeHolderAdded(@NotNull OwnedFloatingNode ownedNode) {
        var oldNode = globalExternalNodes.put(ownedNode.endpoint, ownedNode);
        if(oldNode != null && oldNode != ownedNode) {
            // Drops all connections from the old node (they will be recreated by the line management packet)
            nodeHolderRemoved(oldNode);
        }
        ModdedPackets.sendToServer(new EndpointTrackingC2SPacket(ownedNode, false));
    }

    @Override
    public void nodeHolderUnloaded(@NotNull OwnedFloatingNode ownedNode) {
        nodeHolderRemoved(ownedNode);
    }

    @Override
    public void nodeHolderRemoved(@NotNull OwnedFloatingNode ownedNode) {
        var lines = Set.copyOf(globalGraph.getConnectedLines(ownedNode));
        lines.forEach(TransmissionLine::remove);

        super.nodeHolderRemoved(ownedNode);
        ModdedPackets.sendToServer(new EndpointTrackingC2SPacket(ownedNode, true));
    }

    @Override
    public @Nullable ElectricalNetwork prepareForConnection(IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        add(endpoint1);
        add(endpoint2);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        // Client doesn't do line splitting

        var net1 = node1.getNetwork();
        var net2 = node2.getNetwork();

        // Put both nodes into the same network.
        ElectricalNetwork network;
        if(net1 == null && net2 == null) {
            network = newNetwork();
            endpoint1.joinNetwork(world, network);
            endpoint2.joinNetwork(world, network);
        } else if(net1 == null) {
            network = net2;
            endpoint1.joinNetwork(world, network);
        } else if(net2 == null) {
            network = net1;
            endpoint2.joinNetwork(world, network);
        } else if(net1 != net2) {
            if(net1.size() >= net2.size()) {
                network = net1;
                network.merge(net2);
            } else {
                network = net2;
                network.merge(net1);
            }
        } else {
            network = net1;
        }

        return network;
    }

    @Override
    public @Nullable ElectricalNetwork prepareForConnection(@NotNull OwnedFloatingNode node1, @NotNull OwnedFloatingNode node2) {
        var endpoint1 = node1.endpoint;
        var endpoint2 = node2.endpoint;

        if(node1 == node2)
            return null;

        add(endpoint1);
        add(endpoint2);

        // Client doesn't do line splitting

        var net1 = node1.getNetwork();
        var net2 = node2.getNetwork();

        // Put both nodes into the same network.
        ElectricalNetwork network;
        if(net1 == null && net2 == null) {
            network = newNetwork();
            endpoint1.joinNetwork(world, network);
            endpoint2.joinNetwork(world, network);
        } else if(net1 == null) {
            network = net2;
            endpoint1.joinNetwork(world, network);
        } else if(net2 == null) {
            network = net1;
            endpoint2.joinNetwork(world, network);
        } else if(net1 != net2) {
            if(net1.size() >= net2.size()) {
                network = net1;
                network.merge(net2);
            } else {
                network = net2;
                network.merge(net1);
            }
        } else {
            network = net1;
        }

        return network;
    }

    public void lineManagement(IWireEndpoint endpoint, TransmissionLineManagementS2CPacket.Entry[] entries) {
        var lines = globalGraph.getConnectedLines(endpoint.getNode(world))
                .stream().map(TransmissionLine::getId).collect(Collectors.toCollection(IntOpenHashSet::new));
        for(var entry : entries) {
            if(!entry.endpoint1().isValid(world) || !entry.endpoint2().isValid(world)) {
                // If an endpoint of the line is not valid it is discarded. If it existed before,
                // it is treated as if it were removed.
                continue;
            }
            var line = transmissionLines.get(entry.id());
            if(line != null) {
                // Alter existing line
                lines.remove(line.getId());
                // We compare NODES not ENDPOINTS.
                // This is important since stale endpoints might remain here.
                // They shouldn't, but that's a whole different subject.
                var ln1 = line.getNode1();
                var ln2 = line.getNode2();
                var en1 = entry.endpoint1().getNode(world);
                var en2 = entry.endpoint2().getNode(world);
                if(!((ln1 == en1 && ln2 == en2) || (ln1 == en2 && ln2 == en1))) {
                    // Nodes do not match.
                    if(ln1 != en1) {
                        prepareForConnection(ln1, en1);
                        line.setNode1(entry.endpoint1(), en1);
                    }
                    if(ln2 != en2) {
                        prepareForConnection(ln2, en2);
                        line.setNode2(entry.endpoint2(), en2);
                    }
                }
                line.setResistance(entry.resistance());
            } else {
                // Completely new line
                var node1 = entry.endpoint1().getNode(world);
                var node2 = entry.endpoint2().getNode(world);
                var network = prepareForConnection(node1, node2);
                if(network == null) {
                    PowerGrid.LOGGER.warn("Failed to create a new transmission line from management packet");
                    return;
                }
                line = new TransmissionLine(entry.id(), entry.resistance(),
                        node1, node2, this);
                network.addWire(line);
            }
            removePhantomLines(line);
        }
        for(var id : lines) {
            // Remaining lines have been removed.
            var line = transmissionLines.get(id);
            line.remove();
        }
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
