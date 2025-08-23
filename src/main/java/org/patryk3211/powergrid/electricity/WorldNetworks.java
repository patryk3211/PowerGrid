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

import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.sim.*;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.sim.special.UnresolvedTransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.network.packets.SolverStateS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineManagementS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineStateS2CPacket;
import org.patryk3211.powergrid.utility.PlayerLookup;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.*;

public class WorldNetworks extends SavedData implements NetworkGraph.IGraphModifyHooks {
    public final Level world;
    public final NetworkGraph globalGraph = new NetworkGraph();

    public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
    public final Map<IElectricNode, TransmissionLine> transmissionLineNodes = new HashMap<>();
    public final Map<Integer, TransmissionLine> transmissionLines = new IntObjectHashMap<>();
    public final List<UnresolvedTransmissionLine> unresolvedLines = new ArrayList<>();

    private final Map<IWireEndpoint, Set<ServerPlayer>> trackers = new HashMap<>();
    private final Set<IWireEndpoint> updatedEndpoints = new HashSet<>();

    public final Map<IWireEndpoint, OwnedFloatingNode> globalExternalNodes = new HashMap<>();

    private final Set<WireEntity> deferredRewireEntities = new HashSet<>();
    protected final Set<ElectricalNetwork> islandDiscoveryQueue = new HashSet<>();
    private int syncTicks = 0;

    public WorldNetworks(Level world) {
        this.world = world;
        this.globalGraph.hooks = this;
    }

    public WorldNetworks(Level world, CompoundTag nbt) {
        this(world);
        readNbt(nbt);
    }

    @Override
    public void removeNode(IElectricNode node) {
        assignTransmissionLine(node, null);
    }

    @Override
    public void lineConnected(TransmissionLine line) {
        var id = line.getId();
        transmissionLines.put(id, line);
        updatedEndpoints.add(line.getNode1().endpoint);
        updatedEndpoints.add(line.getNode2().endpoint);
    }

    @Override
    public void lineDisconnected(TransmissionLine line) {
        transmissionLines.remove(line.getId());
        islandDiscoveryQueue.add(line.getNetwork());
        updatedEndpoints.add(line.getNode1().endpoint);
        updatedEndpoints.add(line.getNode2().endpoint);
    }

    private void traceIsland(OwnedFloatingNode first) {

    }

    private void runIslandDiscoveryFor(ElectricalNetwork network) {
        var visited = new HashSet<OwnedFloatingNode>();
//        for(var node : network.getNodes()) {
//            if(!(node instanceof OwnedFloatingNode owned))
//                continue;
//            if(visited.add(owned)) {
//
//            }
//        }
    }

    public void tick() {
        deferredRewireEntities.removeIf(entity -> {
            if(entity.isRemoved())
                return true;
            entity.makeWire();
            return entity.getWire() != null;
        });

        for(var network : islandDiscoveryQueue) {
            runIslandDiscoveryFor(network);
        }
        islandDiscoveryQueue.clear();

        var iter = subnetworks.iterator();
        while(iter.hasNext()) {
            var network = iter.next();
            if(network.isEmpty()) {
                iter.remove();
                continue;
            }
            if(network.isDirty()) {
                // Two more recalculations to make sure the network is stable.
                network.calculate();
                network.calculate();
            }
            network.calculate();
        }
        if(world instanceof ServerLevel serverWorld) {
            for(var endpoint : updatedEndpoints) {
                var players = trackers.get(endpoint);
                if(players == null)
                    continue;
                var lines = globalGraph.getConnectedLines(endpoint.getNode(world));
                ModdedPackets.sendToClients(new TransmissionLineManagementS2CPacket(endpoint, lines), players);
            }
            updatedEndpoints.clear();
            var iter2 = transmissionLines.values().iterator();
            while(iter2.hasNext()) {
                var line = iter2.next();
                if(line.segments.isEmpty()) {
                    PowerGrid.LOGGER.warn("Empty transmission line {} dropped during tick", line);
                    iter2.remove();
                    continue;
                }
                var players = PlayerUtilities.partialTracking(serverWorld, line);
                if(players.isEmpty())
                    continue;
                try {
                    var packet = new TransmissionLineStateS2CPacket(line);
                    ModdedPackets.sendToClients(packet, players);
                } catch (RuntimeException e) {
                    PowerGrid.LOGGER.error("Failed to send a transmission line packet", e);
                }
            }
            if(syncTicks++ >= 20) {
                for(var network : subnetworks) {
                    if(network.getLastGuess() == null)
                        continue;
                    var tracking = new HashSet<ServerPlayer>();
                    var packet = new SolverStateS2CPacket(world, network);
                    for(var chunk : packet.chunks) {
                        tracking.addAll(PlayerLookup.tracking(serverWorld, chunk));
                    }
                    ModdedPackets.sendToClients(packet, tracking);
                }
                syncTicks = 0;
            }
        }
    }

    protected ElectricalNetwork newNetwork() {
        var network = new GraphedElectricalNetwork(globalGraph);
        subnetworks.add(network);
        return network;
    }

    public void add(IWireEndpoint endpoint) {
        globalGraph.addNode(endpoint.getNode(world));
    }

    public int connectionCount(IWireEndpoint endpoint) {
        return globalGraph.connectionCount(endpoint.getNode(world));
    }

    public void assignTransmissionLine(IElectricNode node, @Nullable TransmissionLine line) {
        if (line != null) {
            transmissionLineNodes.put(node, line);
//            transmissionLines.add(line);
        } else {
            transmissionLineNodes.remove(node);
//            if(!transmissionLineNodes.containsValue(removed))
//                transmissionLines.remove(removed);
        }
    }

    @Nullable
    public ElectricalNetwork prepareForConnection(IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        add(endpoint1);
        add(endpoint2);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        // Split transmission lines if needed.
        var line1 = transmissionLineNodes.get(node1);
        if(line1 != null)
            line1.splitAt(node1);
        var line2 = transmissionLineNodes.get(node2);
        if(line2 != null)
            line2.splitAt(node2);

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

    @Nullable
    protected ElectricWire tryGrabUnloadedPart(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        // First try with existing lines
        var node1 = endpoint1.getNode(world);
        var line1 = transmissionLineNodes.get(node1);
        if(line1 != null) {
            var part = line1.grabUnloaded(forEntity);
            if(part != null)
                return part;
        }

        var node2 = endpoint2.getNode(world);
        var line2 = transmissionLineNodes.get(node2);
        if(line2 != null) {
            var part = line2.grabUnloaded(forEntity);
            if(part != null)
                return part;
        }

        var wires = globalGraph.getWires(node1, node2);
        for(var wire : wires) {
            if(wire instanceof TransmissionLine line) {
                var part = line.grabUnloaded(forEntity);
                if(part != null)
                    return part;
            }
        }

        // Next move onto the unresolved lines
        for(var unresolved : unresolvedLines) {
            var part = unresolved.resolvePart(this, forEntity);
            if(part != null)
                return part;
        }
        return null;
    }

    @Nullable
    public OwnedElectricWire makeSimpleWire(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var network = prepareForConnection(endpoint1, endpoint2);
        if(network == null)
            return null;

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        var wire = new OwnedElectricWire(forEntity.getResistance(), node1, node2, forEntity);
        network.addWire(wire);
        return wire;
    }

    @Nullable
    public ElectricWire makeTransmissionLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        var unloadedPart = tryGrabUnloadedPart(endpoint1, endpoint2, forEntity);
        if(unloadedPart != null)
            return unloadedPart;

        // This method needs to ensure proper ordering of segments in the transmission line.
        var network = prepareForConnection(endpoint1, endpoint2);
        if(network == null)
            return null;

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        int nConns1 = connectionCount(endpoint1);
        int nConns2 = connectionCount(endpoint2);
        var connected1 = nConns1 == 1 ? globalGraph.getConnectedNodes(node1).get(0) : null;
        var connected2 = nConns2 == 1 ? globalGraph.getConnectedNodes(node2).get(0) : null;

        TransmissionLine line1 = null, line2 = null;
        TransmissionLinePart linePart = null;
        if(nConns1 == 1) {
            // We can attach to an existing line on endpoint1
            var wire = globalGraph.getFirstWire(node1, connected1);
            if(wire instanceof TransmissionLine curLine) {
                line1 = curLine;
            }
        }
        if(nConns2 == 1) {
            // We can attach to an existing line on endpoint2
            var wire = globalGraph.getFirstWire(node2, connected2);
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
            var line = new TransmissionLine(forEntity.getResistance(), node1, node2, linePart, this);
            linePart.setLine(line);
            network.addWire(line);
            PowerGrid.LOGGER.trace("{}: New transmission line between {} and {}", line, node1, node2);
        }

        setDirty();
        return linePart;
    }

    public List<WireEntity> findConnectedWires(ElectricBehaviour behaviour) {
        var wires = new ArrayList<WireEntity>();
        for(var node : behaviour.getExternalNodes()) {
            var nodes = globalGraph.getConnectedNodes(node);
            nodes.stream()
                    .flatMap(connected -> globalGraph.getWires(node, connected).stream())
                    .filter(wire -> wire instanceof TransmissionLine)
                    .map(wire -> {
                        var line = (TransmissionLine) wire;
                        if(line.getNode1() == node)
                            return line.segments.get(0);
                        else if(line.getNode2() == node)
                            return line.segments.get(line.segments.size() - 1);
                        else
                            return null;
                    })
                    .filter(segment -> segment != null && segment.owner != null)
                    .forEach(segment -> wires.add(segment.owner));
        }
        return wires;
    }

    public void deferredRewire(Collection<WireEntity> wires) {
        wires.forEach(WireEntity::dropWire);
        deferredRewireEntities.addAll(wires);
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag nbt) {
        var lineList = new ListTag();
        for(var line : transmissionLines.values()) {
            lineList.add(new UnresolvedTransmissionLine(line).writeNbt());
        }
        // Write unresolved lines back to nbt.
        for(var unresolved : unresolvedLines) {
            lineList.add(unresolved.writeNbt());
        }
        nbt.put("TransmissionLines", lineList);
        return nbt;
    }

    protected void readNbt(CompoundTag nbt) {
        var lineList = nbt.getList("TransmissionLines", Tag.TAG_COMPOUND);
        for(var lineEntryGeneric : lineList) {
            var lineEntry = (CompoundTag) lineEntryGeneric;
            unresolvedLines.add(new UnresolvedTransmissionLine(lineEntry));
        }
    }

    public void nodeHolderUnloaded(@NotNull OwnedFloatingNode ownedNode) {
        // TODO:
        // Here, we need to choose between preserving transmission line junction nodes or removing them.
    }

    public void nodeHolderRemoved(@NotNull OwnedFloatingNode ownedNode) {
        // Connections should already be broken by endpoint removal stuff.
        if(ownedNode.getNetwork() != null) {
            ownedNode.getNetwork().removeNode(ownedNode);
        }
        globalExternalNodes.remove(ownedNode.endpoint);
    }

    public void nodeHolderAdded(@NotNull OwnedFloatingNode ownedNode) {
        var oldNode = globalExternalNodes.put(ownedNode.endpoint, ownedNode);
        if(oldNode != null) {
            // TODO:
            // Migrate connections into the new node.
            // This happens when a block entity is loaded but its terminal was acting as a transmission line junction.
        }
    }

    public void tracking(ServerPlayer tracker, IWireEndpoint endpoint, boolean end) {
        if(!end) {
            trackers.computeIfAbsent(endpoint, $ -> new HashSet<>()).add(tracker);
            var lines = globalGraph.getConnectedLines(endpoint.getNode(world));
            ModdedPackets.sendToClient(new TransmissionLineManagementS2CPacket(endpoint, lines), tracker);
        } else {
            var list = trackers.get(endpoint);
            if(list == null)
                return;
            list.remove(tracker);
            if(list.isEmpty())
                trackers.remove(endpoint);
        }
    }

    @NotNull
    public Set<ServerPlayer> getTrackers(IWireEndpoint endpoint) {
        var set = trackers.get(endpoint);
        if(set == null)
            return Set.of();
        return set;
    }
}
