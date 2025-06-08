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
import org.patryk3211.powergrid.collections.ModdedTags;
import org.patryk3211.powergrid.electricity.sim.*;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
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
    }

    public static WorldNetworks getWorldNetworks(World world) {
        return worldNetworks.computeIfAbsent(world, WorldNetworks::new);
    }

    public static ElectricalNetwork createNetwork(World world) {
        var networkList = getWorldNetworks(world);
        return networkList.newNetwork();
    }

    private static void traceGraph(WorldNetworks worldNetworks, TransmissionLine line, Set<IElectricNode> visited, List<IElectricNode> toVisit) {
        while(!toVisit.isEmpty()) {
            var node = toVisit.remove(0);
            if(worldNetworks.globalGraph.connectionCount(node) > 2) {
                // Line end
                PowerGrid.LOGGER.info("Found line end");
                continue;
            }
            var connected = worldNetworks.globalGraph.getConnectedNodes(node);
            for(var connectedNode : connected) {
                if(!visited.add(connectedNode))
                    continue;
                var wire = worldNetworks.globalGraph.getWire(node, connectedNode);
                if(wire != null)
                    wire.remove();
                // Migrate connection in graph to transmission line.
                worldNetworks.globalGraph.connect(node, connectedNode, line);
                if(wire instanceof OwnedElectricWire ownedWire) {
                    line.addHolder(ownedWire.owner);
                    ownedWire.owner.setWire(line);
                }
            }
        }
        for(var node : visited) {
            if(node.getNetwork() != null) {
                worldNetworks.globalGraph.keepNode(node);
                node.getNetwork().removeNode(node);
            }
        }
    }

    public static void removeTransmissionLine(TransmissionLine line) {
        World world = null;
        IElectricNode startNode = null;
        for(var holder : line.holders) {
            if(world == null)
                world = holder.getWorld();
            if(world != null) {
                startNode = holder.getEndpoint1().getNode(world);
                if(startNode == null)
                    startNode = holder.getEndpoint2().getNode(world);
                if(startNode != null)
                    break;
            }
        }
        if(world == null) {
            // No holders
            return;
        }

        var worldNetworks = getWorldNetworks(world);
        worldNetworks.transmissionLines.remove(line);

        var toVisit = new ArrayList<IElectricNode>();
        var visited = new HashSet<IElectricNode>();
        toVisit.add(startNode);
        while(!toVisit.isEmpty()) {
            var node = toVisit.remove(0);
            if(!visited.add(node))
                continue;
            var connectedNodes = worldNetworks.globalGraph.getConnectedNodes(node);
            for(var connected : connectedNodes) {
                var wire = worldNetworks.globalGraph.getWire(node, connected);
                if(wire != line) {
                    // Not part of this transmission line
                    continue;
                }
                worldNetworks.globalGraph.disconnect(node, connected);
                toVisit.add(connected);
            }
            worldNetworks.transmissionLineMap.remove(node);
            if(worldNetworks.globalGraph.connectionCount(node) == 0)
                worldNetworks.globalGraph.removeNode(node);
        }
    }

    public static void collapseTransmissionLine(World world, TransmissionLine line) {
        var worldNetworks = getWorldNetworks(world);

        var invalidNodes = new HashMap<IElectricNode, Integer>();
        for(var holder : line.holders) {
            var n1 = holder.getEndpoint1().getNode(world);
            var n2 = holder.getEndpoint2().getNode(world);

            int n1Count = worldNetworks.globalGraph.connectionCount(n1);
            if(worldNetworks.globalGraph.connectionCount(n1) > 2)
                invalidNodes.put(n1, n1Count);
            int n2Count = worldNetworks.globalGraph.connectionCount(n2);
            if(worldNetworks.globalGraph.connectionCount(n2) > 2)
                invalidNodes.put(n2, n2Count);
        }

        worldNetworks.globalGraph.addCountOverrides(invalidNodes);
        line.remove();
        worldNetworks.globalGraph.removeCountOverride(invalidNodes);
    }

    @Nullable
    public static TransmissionLine makeTransmissionLine(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var worldNetworks = getWorldNetworks(world);
        if(!worldNetworks.isTransmissionLinePoint(endpoint1) || !worldNetworks.isTransmissionLinePoint(endpoint2))
            return null;

        int nConns1 = worldNetworks.connectionCount(endpoint1);
        int nConns2 = worldNetworks.connectionCount(endpoint2);
        if(nConns1 > 1 || nConns2 > 1)
            return null;

        var line1 = worldNetworks.getTransmissionLine(endpoint1);
        var line2 = worldNetworks.getTransmissionLine(endpoint2);

        TransmissionLine line;
        if(line1 != null && line2 != null) {
            PowerGrid.LOGGER.info("Merging lines");
            // Merge lines
            line1.merge(line2);
            line = line1;
        } else if(line1 != null || line2 != null) {
            PowerGrid.LOGGER.info("Extending line");
            // Extend one line
            line = line1 != null ? line1 : line2;
            var visited = new HashSet<IElectricNode>();
            var toVisit = new ArrayList<IElectricNode>();
            toVisit.add(line1 != null ? endpoint2.getNode(world) : endpoint1.getNode(world));
            traceGraph(worldNetworks, line, visited, toVisit);
        } else {
            PowerGrid.LOGGER.info("New line");
            // Completely new line
            line = new TransmissionLine();
            worldNetworks.transmissionLines.add(line);
            var visited = new HashSet<IElectricNode>();
            var toVisit = new ArrayList<IElectricNode>();
            toVisit.add(endpoint1.getNode(world));
            toVisit.add(endpoint2.getNode(world));
            traceGraph(worldNetworks, line, visited, toVisit);
        }

        line.addHolder(forEntity);
        worldNetworks.globalGraph.connect(endpoint1.getNode(world), endpoint2.getNode(world), line);
        for(var holder : line.holders) {
            var n1 = holder.getEndpoint1().getNode(world);
            var n2 = holder.getEndpoint2().getNode(world);
            worldNetworks.transmissionLineMap.put(n1, line);
            worldNetworks.transmissionLineMap.put(n2, line);
            holder.setWire(line);
        }
        return line;
    }

    public static ElectricWire makeConnection(World world, IWireEndpoint endpoint1, IWireEndpoint endpoint2, float resistance, WireEntity forEntity) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        var worldNetworks = getWorldNetworks(world);
        worldNetworks.add(endpoint1);
        worldNetworks.add(endpoint2);

        var line = makeTransmissionLine(world, endpoint1, endpoint2, forEntity);
        if(line != null) {
            // TODO: Add ends of transmission line to corresponding networks.
            return line;
        }

        // Put both nodes into the same network.
        ElectricalNetwork network;
        if(node1.getNetwork() == null && node2.getNetwork() == null) {
            network = GlobalElectricNetworks.createNetwork(world);
            endpoint1.joinNetwork(world, network);
            endpoint2.joinNetwork(world, network);
        } else if(node1.getNetwork() == null) {
            network = node2.getNetwork();
            endpoint1.joinNetwork(world, network);
        } else if(node2.getNetwork() == null) {
            network = node1.getNetwork();
            endpoint2.joinNetwork(world, network);
        } else if(node1.getNetwork() != node2.getNetwork()) {
            if(node1.getNetwork().size() >= node2.getNetwork().size()) {
                network = node1.getNetwork();
                network.merge(node2.getNetwork());
            } else {
                network = node2.getNetwork();
                network.merge(node1.getNetwork());
            }
        } else {
            network = node1.getNetwork();
        }

        var wire = new OwnedElectricWire(resistance, node1, node2, forEntity);
        network.addWire(wire);

        var line1 = worldNetworks.transmissionLineMap.get(node1);
        var line2 = worldNetworks.transmissionLineMap.get(node2);
        if(line1 != null)
            collapseTransmissionLine(world, line1);
        if(line2 != null)
            collapseTransmissionLine(world, line2);

        return wire;
    }

    public static class WorldNetworks {
        public final World world;
        public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
        public final List<TransmissionLine> transmissionLines = new ArrayList<>();
        public final Map<IElectricNode, TransmissionLine> transmissionLineMap = new HashMap<>();
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
                boolean transmissionLinePoint = block.isIn(ModdedTags.Block.TRANSMISSION_LINE_POINT.tag);
                globalGraph.addNode(endpoint.getNode(world), transmissionLinePoint);
            }
        }

        public int connectionCount(IWireEndpoint endpoint) {
            return globalGraph.connectionCount(endpoint.getNode(world));
        }

        public boolean isTransmissionLinePoint(IWireEndpoint endpoint) {
            return globalGraph.isTransmissionLinePoint(endpoint.getNode(world));
        }

        public TransmissionLine getTransmissionLine(IWireEndpoint endpoint) {
            return transmissionLineMap.get(endpoint.getNode(world));
        }

        public void removeAll(Collection<ElectricalNetwork> networks) {
            subnetworks.removeAll(networks);
        }
    }
}
