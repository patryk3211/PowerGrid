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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.sim.*;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.*;

public class GlobalElectricNetworks {
    protected static final Map<World, WorldNetworks> worldNetworks = new HashMap<>();

    public static void init() {
        ServerTickEvents.START_WORLD_TICK.register(GlobalElectricNetworks::tick);
        ServerWorldEvents.UNLOAD.register((server, world) -> worldNetworks.remove(world));
    }

    protected static void tick(World world) {
        var networks = worldNetworks.get(world);
        if(networks == null)
            return;
        List<ElectricalNetwork> removed = new LinkedList<>();
        for(final var network : networks.subnetworks) {
            if(network.isEmpty()) {
                removed.add(network);
                continue;
            }
            if(network.isDirty()) {
                // Two more recalculations to make sure the network is stable.
                network.calculate();
                network.calculate();
            }
            network.calculate();
        }
        networks.removeAll(removed);
        for(final var network : networks.subnetworks) {
            network.calculate();
        }
    }

    public static WorldNetworks getWorldNetworks(World world) {
        return worldNetworks.computeIfAbsent(world, WorldNetworks::new);
    }

    public static ElectricalNetwork createNetwork(World world) {
        var networkList = getWorldNetworks(world);
        return networkList.newNetwork();
    }

    public static void splitTransmissionLine(World world, IElectricNode node) {
        var worldNetworks = getWorldNetworks(world);
        var connCount = worldNetworks.globalGraph.connectionCount(node);
        if(connCount == 0) {
            var line = worldNetworks.transmissionLineNodes.get(node);
            if(line != null) {
                line.splitAt(node);
            }
        }
    }

    public static TransmissionLine getLine(WireEntity entity) {
        var wire = entity.getWire();
        if(wire == null)
            return null;
        var worldNetworks = getWorldNetworks(entity.getWorld());
        var line = worldNetworks.transmissionLineNodes.get(wire.getNode1());
        if(line != null && line.isPart(wire)) {
            return line;
        }
        line = worldNetworks.transmissionLineNodes.get(wire.getNode2());
        if(line != null && line.isPart(wire)) {
            return line;
        }
        // If that fails, the only other option is that the line has one segment (or doesn't exist).
        var lineWire = worldNetworks.globalGraph.getFirstWire(wire.getNode1(), wire.getNode2());
        if(lineWire instanceof TransmissionLine line1) {
            return line1;
        }
        return null;
    }

    public static OwnedElectricWire makeTransmissionLine(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        // This method needs to ensure proper ordering of segments in the transmission line.
        var worldNetworks = getWorldNetworks(world);

        var network = unifyNetwork(world, endpoint1, endpoint2);
        if(network == null)
            return null;

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        int nConns1 = worldNetworks.connectionCount(endpoint1);
        int nConns2 = worldNetworks.connectionCount(endpoint2);
        var connected1 = nConns1 == 1 ? worldNetworks.globalGraph.getConnectedNodes(node1).get(0) : null;
        var connected2 = nConns2 == 1 ? worldNetworks.globalGraph.getConnectedNodes(node2).get(0) : null;

        TransmissionLine line1 = null, line2 = null;
        TransmissionLinePart linePart = null;
        if(nConns1 == 1) {
            // We can attach to an existing line on endpoint1
            var wire = worldNetworks.globalGraph.getFirstWire(node1, connected1);
            if(wire instanceof TransmissionLine curLine) {
                line1 = curLine;
            }
        } else if(nConns1 == 0) {
            // Possibly part of a transmission line
            // After splitting, the transmission line might not be in the same network.
            splitTransmissionLine(world, node1);
        }
        if(nConns2 == 1) {
            // We can attach to an existing line on endpoint2
            var wire = worldNetworks.globalGraph.getFirstWire(node2, connected2);
            if(wire instanceof TransmissionLine curLine) {
                line2 = curLine;
                if(line1 != null) {
                    if(line1 != line2) {
                        linePart = new TransmissionLinePart(forEntity.getResistance(), node1, node2, forEntity, line1);
                        // We can extend the first line by the second node.
                        PowerGrid.LOGGER.trace("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
                        if(line1.getNode2() != node1)
                            line1.flip();
                        line1.addLastSegment(linePart);
                        // We need to merge lines.
                        PowerGrid.LOGGER.trace("{}: Merging transmission lines between {} and {}", line1, node1, node2);
                        if (curLine.getNode1() != line1.getNode2())
                            curLine.flip();
                        line1.merge(curLine);
                    } else {
                        // We are merging two ends of a single line, this cannot happen or things will break.
                        line2 = null;
                    }
                    line1 = null;
                } else {
                    linePart = new TransmissionLinePart(forEntity.getResistance(), node1, node2, forEntity, line2);
                    PowerGrid.LOGGER.trace("{}: Extending line at beginning by wire {}, starting node is now {}", line2, linePart, node1);
                    // We can extend this line by the first node.
                    if(line2.getNode1() != node2)
                        line2.flip();
                    line2.addFirstSegment(linePart);
                }
            }
        } else if(nConns2 == 0) {
            // Possibly part of a transmission line
            splitTransmissionLine(world, node2);
        }
        if(line1 != null) {
            linePart = new TransmissionLinePart(forEntity.getResistance(), node1, node2, forEntity, line1);
            // We can extend this line by the second node.
            PowerGrid.LOGGER.trace("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
            if(line1.getNode2() != node1)
                line1.flip();
            line1.addLastSegment(linePart);
        }
        if(line1 == null && line2 == null) {
            linePart = new TransmissionLinePart(forEntity.getResistance(), node1, node2, forEntity, null);
            var line = new TransmissionLine(forEntity.getResistance(), node1, node2, linePart, worldNetworks);
            linePart.setLine(line);
            network.addWire(line);
            PowerGrid.LOGGER.trace("{}: New transmission line between {} and {}", line, node1, node2);
        }
        return linePart;
    }

    @Nullable
    private static ElectricalNetwork unifyNetwork(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        var worldNetworks = getWorldNetworks(world);
        worldNetworks.add(endpoint1);
        worldNetworks.add(endpoint2);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        var line1 = worldNetworks.transmissionLineNodes.get(node1);
        var line2 = worldNetworks.transmissionLineNodes.get(node2);

        var net1 = line1 == null ? node1.getNetwork() : line1.getNetwork();
        var net2 = line2 == null ? node2.getNetwork() : line2.getNetwork();

        // Put both nodes into the same network.
        ElectricalNetwork network;
        if(net1 == null && net2 == null) {
            network = GlobalElectricNetworks.createNetwork(world);
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

    private static OwnedElectricWire makeSimpleWire(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var network = unifyNetwork(world, endpoint1, endpoint2);
        if(network == null)
            return null;

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        var wire = new OwnedElectricWire(forEntity.getResistance(), node1, node2, forEntity);
        network.addWire(wire);
        return wire;
    }

    public static ElectricWire makeConnection(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
//        return makeSimpleWire(world, endpoint1, endpoint2, forEntity);
        return makeTransmissionLine(world, endpoint1, endpoint2, forEntity);
    }

    public static class WorldNetworks {
        public final World world;
        public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
        public final Map<IElectricNode, TransmissionLine> transmissionLineNodes = new HashMap<>();
        public final NetworkGraph globalGraph = new NetworkGraph();

        public WorldNetworks(World world) {
            this.world = world;
        }

        public ElectricalNetwork newNetwork() {
            var network = new GraphedElectricalNetwork(globalGraph);
            subnetworks.add(network);
            return network;
        }

        public void add(ElectricalNetwork network) {
            subnetworks.add(network);
        }

        public void add(IWireEndpoint endpoint) {
            if(endpoint instanceof BlockWireEndpoint blockEndpoint) {
                var block = world.getBlockState(blockEndpoint.getPos());
                globalGraph.addNode(endpoint.getNode(world));
            }
        }

        public int connectionCount(IWireEndpoint endpoint) {
            return globalGraph.connectionCount(endpoint.getNode(world));
        }

        public void removeAll(Collection<ElectricalNetwork> networks) {
            subnetworks.removeAll(networks);
        }

        public void assignTransmissionLine(IElectricNode node, @Nullable TransmissionLine line) {
            if(line != null) {
                transmissionLineNodes.put(node, line);
            } else {
                transmissionLineNodes.remove(node);
            }
        }

        public void validateRemoval(TransmissionLine line) {
            if(transmissionLineNodes.containsValue(line)) {
                PowerGrid.LOGGER.error("Line {} was not fully removed from transmission line node map!", line);
            }
        }
    }
}
