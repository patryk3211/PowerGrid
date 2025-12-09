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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.DummyElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.network.packets.EndpointTrackingC2SPacket;

import java.util.ArrayList;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ClientWorldNetworks extends WorldNetworks {
    private final DummyElectricalNetwork network = new DummyElectricalNetwork(globalGraph);

    public ClientWorldNetworks(Level world) {
        super(world);
        perf.rename("ClientWorld");
        subnetworks.add(network);
    }

    @Override
    public ElectricalNetwork newNetwork() {
        return network;
    }

    @Override
    public void scheduleIslandDiscovery(ElectricalNetwork network) { }

    @Override
    public @Nullable ElectricWire makeTransmissionLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, BaseWireEntity forEntity, PartId id) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        if(node1.getNetwork() == null)
            network.addNode(node1);
        if(node2.getNetwork() == null)
            network.addNode(node2);

        var wire = new ElectricWire(forEntity.getResistance(), node1, node2);
        network.addWire(wire);
        return wire;
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
    public void nodeHolderAdded(@NotNull OwnedFloatingNode ownedNode, boolean hasInternals) {
        var oldNode = globalExternalNodes.put(ownedNode.endpoint, ownedNode);
        ownedNode.endpoint.joinNetwork(world, network);
        if(oldNode != null && oldNode != ownedNode) {
            var nodes = globalGraph.getConnectedNodes(oldNode);
            var allWires = new ArrayList<AbstractElectricWire>();
            for(var node : nodes) {
                allWires.addAll(globalGraph.getWires(oldNode, node));
            }
            for(var wire : allWires) {
                if(wire.getNode1() == oldNode) {
                    wire.setNode1(ownedNode);
                } else if(wire.getNode2() == oldNode) {
                    wire.setNode2(ownedNode);
                }
            }
            network.removeNode(oldNode);
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

        removeFromNetwork(ownedNode);
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
}
