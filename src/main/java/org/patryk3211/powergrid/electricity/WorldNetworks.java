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

import com.google.common.collect.Sets;
import io.netty.util.collection.IntObjectHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
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
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.JunctionWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.network.packets.SolverStateS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineManagementS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineStateS2CPacket;
import org.patryk3211.powergrid.utility.PlayerLookup;
import org.patryk3211.powergrid.utility.PlayerUtilities;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldNetworks extends SavedData implements NetworkGraph.IGraphModifyHooks {
    public final Level world;
    public final NetworkGraph globalGraph = new NetworkGraph();

    public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
    public final Map<IElectricNode, TransmissionLine> transmissionLineNodes = new HashMap<>();
    public final Map<Integer, TransmissionLine> transmissionLines = new IntObjectHashMap<>();

    private final Map<ChunkPos, CheckChunk> expectedInChunks = new ConcurrentHashMap<>();
    private final Map<ChunkPos, CheckChunk> checkForExistence = new ConcurrentHashMap<>();
    private final Map<UUID, TransmissionLinePart> lineParts = new HashMap<>();
    private final Map<OwnedFloatingNode, Set<TransmissionLinePart>> partNodeMap = new HashMap<>();

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
//
//    @Override
//    public void removeNode(IElectricNode node) {
//        if(node instanceof OwnedFloatingNode owned) {
//            assignTransmissionLine(owned, null);
//        }
//    }

    @Override
    public void lineConnected(TransmissionLine line) {
        var id = line.getId();
        transmissionLines.put(id, line);
        updatedEndpoints.add(line.getEndpoint1());
        updatedEndpoints.add(line.getEndpoint2());
        updatedEndpoints.add(line.getNode1().endpoint);
        updatedEndpoints.add(line.getNode2().endpoint);
        setDirty();
    }

    @Override
    public void lineDisconnected(TransmissionLine line) {
        transmissionLines.remove(line.getId());
        islandDiscoveryQueue.add(line.getNetwork());
        updatedEndpoints.add(line.getEndpoint1());
        updatedEndpoints.add(line.getEndpoint2());
        updatedEndpoints.add(line.getNode1().endpoint);
        updatedEndpoints.add(line.getNode2().endpoint);
        setDirty();
    }

    @Override
    public void addWire(AbstractElectricWire wire) {
        var line1 = transmissionLineNodes.get(wire.getNode1());
        if(line1 != null && wire.getNode1() instanceof OwnedFloatingNode owned)
//            PowerGrid.LOGGER.warn("WHY YOU SPLITTING");
            line1.splitAt(owned);
        var line2 = transmissionLineNodes.get(wire.getNode2());
        if(line2 != null && wire.getNode2() instanceof OwnedFloatingNode owned)
//            PowerGrid.LOGGER.warn("WHY YOU SPLITTING");
            line2.splitAt(owned);
    }

    @Override
    public void addNode(IElectricNode node) {
        if(node instanceof OwnedFloatingNode owned) {
            // Make sure nodes are always up to date
//            addAndMigrateNode(owned.endpoint);
        }
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
        JunctionWireEndpoint.processNewNodes(world);

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
                network.warmUp();
            }
            network.calculate();
        }
        if(world instanceof ServerLevel serverWorld) {
            // Check for line parts existence
            var checkIter = checkForExistence.entrySet().iterator();
            while(checkIter.hasNext()) {
                var entry = checkIter.next();
                var chunk = entry.getKey();
                if(!world.hasChunk(chunk.x, chunk.z)) {
                    if(expectedInChunks.containsKey(chunk)) {
                        expectedInChunks.get(chunk).addAll(entry.getValue().entities);
                    } else {
                        entry.getValue().ticks = 0;
                        expectedInChunks.put(chunk, entry.getValue());
                    }
                    continue;
                }
                var remove = entry.getValue().ticks++ >= 10;
                var entityIter = entry.getValue().entities.iterator();
                while(entityIter.hasNext()) {
                    var id = entityIter.next();
                    if(serverWorld.getEntity(id) == null) {
                        if(remove) {
                            // Doesn't exist even after chunk has been loaded.
                            var part = lineParts.get(id);
                            if (part == null)
                                continue;
                            // Destroy line
                            var line = part.getLine();
                            if(line != null) {
                                line.remove();
                            } else {
                                part.remove();
                            }
                        }
                    } else {
                        entityIter.remove();
                    }
                }
                if(remove || entry.getValue().entities.isEmpty()) {
                    checkIter.remove();
                }
            }
            // Send lines to clients
            for(var endpoint : updatedEndpoints) {
                var players = trackers.get(endpoint);
                if(players == null)
                    continue;
                var lines = globalGraph.getConnectedLines(endpoint.getNode(world));
                ModdedPackets.sendToClients(new TransmissionLineManagementS2CPacket(endpoint, lines), players);
            }
            updatedEndpoints.clear();
            // Send partial lines to clients
            var iter2 = transmissionLines.values().iterator();
            var removed = new ArrayList<TransmissionLine>();
            while(iter2.hasNext()) {
                var line = iter2.next();
                if(line.segments.isEmpty()) {
                    PowerGrid.LOGGER.warn("Empty transmission line {} dropped during tick", line);
                    removed.add(line);
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
            removed.forEach(TransmissionLine::remove);
            // Synchronize solver state with clients
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

    public ElectricalNetwork newNetwork() {
        var network = new GraphedElectricalNetwork(globalGraph);
        subnetworks.add(network);
        return network;
    }

    public void add(IWireEndpoint endpoint) {
        var node = endpoint.getNode(world);
        globalGraph.addNode(node);
        addAndMigrateNode(endpoint);
    }

    public int connectionCount(IWireEndpoint endpoint) {
        return globalGraph.connectionCount(endpoint.getNode(world));
    }

    public void assignTransmissionLine(OwnedFloatingNode node, @Nullable TransmissionLine line) {
        if(node == null)
            return;
        if (line != null) {
            transmissionLineNodes.put(node, line);
        } else {
            transmissionLineNodes.remove(node);
        }
        updatedEndpoints.add(node.endpoint);
    }

    @Nullable
    public ElectricalNetwork prepareForConnection(IWireEndpoint endpoint1, IWireEndpoint endpoint2) {
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        if(node1 == node2)
            return null;
        if(node1 == null || node2 == null)
            return null;

        add(endpoint1);
        add(endpoint2);

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
    public ElectricalNetwork prepareForConnection(@NotNull OwnedFloatingNode node1, @NotNull OwnedFloatingNode node2) {
        var endpoint1 = node1.endpoint;
        var endpoint2 = node2.endpoint;

        if(node1 == node2)
            return null;

        add(endpoint1);
        add(endpoint2);

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
    protected TransmissionLinePart tryGrabUnloadedPart(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        // Try to resolve trees for correct merging of lines
        resolveTree(endpoint1);
        resolveTree(endpoint2);

        var existingPart = lineParts.get(forEntity.getUUID());
        if(existingPart != null) {
            existingPart.grab(forEntity);
            return existingPart;
        }

        return null;
    }

    private boolean makeTransmissionLine(TransmissionLinePart linePart) {
        if(linePart.getLine() != null)
            return true;

        var endpoint1 = linePart.getEndpoint1();
        var endpoint2 = linePart.getEndpoint2();

        // This method needs to ensure proper ordering of segments in the transmission line.
        var network = prepareForConnection(endpoint1, endpoint2);
        if(network == null)
            return false;

        PowerGrid.LOGGER.debug("Creating a transmission line for {}", linePart);
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        // Make sure nodes are up-to-date
        movePartMap(linePart.getNode1(), node1, linePart);
        linePart.setNode1(node1);
        movePartMap(linePart.getNode2(), node2, linePart);
        linePart.setNode2(node2);

        int nConns1 = connectionCount(endpoint1);
        int nConns2 = connectionCount(endpoint2);
        var connected1 = nConns1 == 1 ? globalGraph.getConnectedNodes(node1).get(0) : null;
        var connected2 = nConns2 == 1 ? globalGraph.getConnectedNodes(node2).get(0) : null;

        TransmissionLine line1 = null, line2 = null;
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
                        linePart.setLine(line1);
                        // We can extend the first line by the second node.
                        PowerGrid.LOGGER.debug("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
                        if(line1.getNode2() != node1)
                            line1.flip();
                        line1.addLastSegment(linePart);
                        // We need to merge lines.
                        PowerGrid.LOGGER.debug("{}: Merging transmission lines between {} and {}", line1, node1, node2);
                        if (curLine.getNode1() != line1.getNode2())
                            curLine.flip();
                        line1.merge(curLine);
                    } else {
                        // We are merging two ends of a single line, this cannot happen or things will break.
                        line2 = null;
                    }
                    line1 = null;
                } else {
                    linePart.setLine(line2);
                    PowerGrid.LOGGER.debug("{}: Extending line at beginning by wire {}, starting node is now {}", line2, linePart, node1);
                    // We can extend this line by the first node.
                    if(line2.getNode1() != node2)
                        line2.flip();
                    line2.addFirstSegment(linePart);
                }
            }
        }
        if(line1 != null) {
            linePart.setLine(line1);
            // We can extend this line by the second node.
            PowerGrid.LOGGER.debug("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
            if(line1.getNode2() != node1)
                line1.flip();
            line1.addLastSegment(linePart);
        }
        if(line1 == null && line2 == null) {
            var line = new TransmissionLine(linePart.getResistance(), endpoint1, endpoint2, this);
            linePart.setLine(line);
            line.segments.add(linePart);
            network.addWire(line);
            PowerGrid.LOGGER.debug("{}: New transmission line between {} and {}", line, node1, node2);
        }

        setDirty();
        return true;
    }

    @Nullable
    public ElectricWire makeTransmissionLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        add(endpoint1);
        add(endpoint2);

        var linePart = tryGrabUnloadedPart(endpoint1, endpoint2, forEntity);
        if(linePart != null && linePart.getLine() != null)
            return linePart;

        if(linePart == null)
            linePart = TransmissionLinePart.uniquePart(forEntity.getResistance(), endpoint1, endpoint2, forEntity, this);

        if(makeTransmissionLine(linePart))
            return linePart;
        return null;
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
                        if(line.segments.isEmpty())
                            return null;
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
        var partList = new ListTag();
        for(var part : lineParts.values()) {
            partList.add(part.toNbt());
        }
        nbt.put("Parts", partList);
        return nbt;
    }

    protected void readNbt(CompoundTag nbt) {
        var partList = nbt.getList("Parts", Tag.TAG_COMPOUND);
        for(var entryGeneric : partList) {
            var partEntry = (CompoundTag) entryGeneric;
            TransmissionLinePart.uniquePart(partEntry, this);
        }
    }

    public void nodeHolderUnloaded(@NotNull OwnedFloatingNode ownedNode) {
        // Here, we need to choose between preserving transmission line junction nodes or removing them.
        // A transmission line should be preserved if terminates on a junction which connects to more
        // transmission lines.
        Collection<TransmissionLine> lines;
        while(true) {
            lines = globalGraph.getConnectedLines(ownedNode);
            if(lines.size() != 1)
                break;
            // No need to keep this line (or the node).
            var line = lines.iterator().next();
            var removeNode = ownedNode;
            if(line.getNode1() == ownedNode) {
                ownedNode = line.getNode2();
            } else {
                assert line.getNode2() == ownedNode;
                ownedNode = line.getNode1();
            }
            // Also, we need to inform the wire entities about this.
            line.unresolve();
            setDirty();
            if(removeNode.getNetwork() != null) {
                removeNode.getNetwork().removeNode(removeNode);
            }
            globalExternalNodes.remove(removeNode.endpoint);
        }
    }

    public void nodeHolderRemoved(@NotNull OwnedFloatingNode ownedNode) {
        // Connections should already be broken by endpoint removal stuff.
        if(ownedNode.getNetwork() != null) {
            ownedNode.getNetwork().removeNode(ownedNode);
        }
        globalExternalNodes.remove(ownedNode.endpoint);
    }

    private boolean traceTree(IWireEndpoint endpoint, Set<IWireEndpoint> visited) {
        if(!visited.add(endpoint))
            return false;
        Collection<TransmissionLinePart> parts = partNodeMap.get(globalExternalNodes.get(endpoint));
        if(parts == null)
            return false;
        parts = List.copyOf(parts);
        boolean continueResolving = false;
        for(var part : parts) {
            if(part.getLine() != null) {
                // This branch connects to a valid line, back-trace and resolve all line parts.
                continueResolving = true;
            }
            if(parts.size() == 1) {
                PowerGrid.LOGGER.debug("Found edge line at {}", endpoint);
                if(part.getEndpoint1().equals(endpoint)) {
                    // Check endpoint2
                    if(part.getEndpoint2().isValid(world)) {
                        // Resolve segment
                        makeTransmissionLine(part);
                        return true;
                    }
                } else {
                    assert part.getEndpoint2().equals(endpoint);
                    // Check endpoint1
                    if(part.getEndpoint1().isValid(world)) {
                        // Resolve segment
                        makeTransmissionLine(part);
                        return true;
                    }
                }
                break;
            }
            PowerGrid.LOGGER.debug("Continuing line trace through {}", endpoint);
            if(part.getEndpoint1().equals(endpoint)) {
                if(traceTree(part.getEndpoint2(), visited)) {
                    makeTransmissionLine(part);
                    continueResolving = true;
                }
            } else {
                assert part.getEndpoint2().equals(endpoint);
                if(traceTree(part.getEndpoint1(), visited)) {
                    makeTransmissionLine(part);
                    continueResolving = true;
                }
            }
        }
        return continueResolving;
    }

    private void resolveTree(@NotNull IWireEndpoint endpoint) {
        var unresolvedLines = partNodeMap.get(globalExternalNodes.get(endpoint));
        if(unresolvedLines == null)
            return;
        // We need to trace the graph to all terminating nodes and see if any are loaded,
        // if so, we need to resolve all lines between them to ensure correct unloaded chunk behaviour.
        var visited = new HashSet<IWireEndpoint>();
        PowerGrid.LOGGER.debug("Starting line trace at {}", endpoint);
        traceTree(endpoint, visited);
    }

    public void addAndMigrateNode(IWireEndpoint endpoint) {
        var newNode = endpoint.getNode(world);
        if(newNode == null)
            return;
        addAndMigrateNode(newNode);
    }

    public void addAndMigrateNode(OwnedFloatingNode newNode) {
        var endpoint = newNode.endpoint;
        var oldNode = globalExternalNodes.put(endpoint, newNode);
        addAndMigrateNode(oldNode, newNode);
    }

    public void addAndMigrateNode(IWireEndpoint oldEndpoint, OwnedFloatingNode newNode) {
        var endpoint = newNode.endpoint;
        var oldNode = globalExternalNodes.put(endpoint, newNode);
        addAndMigrateNode(oldNode, newNode);
        var oldNode2 = globalExternalNodes.remove(oldEndpoint);
        addAndMigrateNode(oldNode2, newNode);

        var line = transmissionLineNodes.get(newNode);
        if(line != null) {
            line.splitAt(newNode);
        }
    }

    public void addAndMigrateNode(OwnedFloatingNode oldNode, OwnedFloatingNode newNode) {
        var endpoint = newNode.endpoint;
        if(oldNode != null && oldNode != newNode) {
            // Migrate connections into the new node.
            // This happens when a block entity is loaded but its terminal was acting as a transmission line junction.
            PowerGrid.LOGGER.debug("Migrating external node from {} to {}", oldNode, newNode);
            var parts = partNodeMap.remove(oldNode);
            if(parts != null) {
                for(TransmissionLinePart part : parts) {
                    PowerGrid.LOGGER.debug("Migrating node for part {}", part);
                    if(part.getEndpoint1().equals(endpoint) || part.getNode1() == oldNode) {
                        part.setNode1(newNode);
                        PowerGrid.LOGGER.debug("Part {} has had its node migrated", part);
                        var line = part.getLine();
                        if(line != null) {
                            if(line.getNode1() == oldNode) {
                                inNetwork(line.getNetwork(), newNode);
                                line.setNode1(newNode);
                                PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                            }
                        }
                    }
                    if(part.getEndpoint2().equals(endpoint) || part.getNode2() == oldNode) {
                        part.setNode2(newNode);
                        PowerGrid.LOGGER.debug("Part {} has had its node migrated", part);
                        var line = part.getLine();
                        if(line != null) {
                            if(line.getNode2() == oldNode) {
                                inNetwork(line.getNetwork(), newNode);
                                line.setNode2(newNode);
                                PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                            }
                        }
                    }
                    partNodeMap.computeIfAbsent(newNode, $ -> new HashSet<>()).add(part);
                }
            }
            if(oldNode.getNetwork() != null) {
                inNetwork(oldNode.getNetwork(), newNode);
                var unified = oldNode.getNetwork();
                var lines = List.copyOf(globalGraph.getConnectedLines(oldNode));
                for (var line : lines) {
                    if (line.getNode1() == oldNode) {
                        line.setNode1(newNode);
                        PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                    } else if (line.getNode2() == oldNode) {
                        line.setNode2(newNode);
                        PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                    }
                }
                unified.removeNode(oldNode);
            } else {
                var line = transmissionLineNodes.remove(oldNode);
                if(line != null) {
                    for (var segment : line.segments) {
                        if (segment.getNode1() == oldNode || segment.getEndpoint1().equals(endpoint)) {
                            movePartMap(segment.getNode1(), newNode, segment);
                            segment.setNode1(newNode);
                            PowerGrid.LOGGER.debug("Line {} has had its internal node migrated", line);
                        } else if (segment.getNode2() == oldNode || segment.getEndpoint2().equals(endpoint)) {
                            movePartMap(segment.getNode2(), newNode, segment);
                            segment.setNode2(newNode);
                            PowerGrid.LOGGER.debug("Line {} has had its internal node migrated", line);
                        }
                    }
                    transmissionLineNodes.put(newNode, line);
                }
            }
        }
    }

    public void nodeHolderAdded(@NotNull OwnedFloatingNode ownedNode, boolean hasInternals) {
        PowerGrid.LOGGER.debug("Node holder added, {}", ownedNode);
        addAndMigrateNode(ownedNode.endpoint);
        // Try to resolve an end of a transmission line
        resolveTree(ownedNode.endpoint);
        if(hasInternals) {
            var line = transmissionLineNodes.get(ownedNode);
            if(line != null) {
                line.splitAt(ownedNode);
            }
        }
    }

    /**
     * Save an entity to be recovered into the transmission line once its chunk is loaded back into the world.
     *
     * @param entityId       Transmission line part entity id
     * @param lastKnownChunk Last known chunk of the entity
     */
    public void bounty(UUID entityId, ChunkPos lastKnownChunk) {
        if(world.hasChunk(lastKnownChunk.x, lastKnownChunk.z)) {
            checkForExistence.computeIfAbsent(lastKnownChunk, $ -> new CheckChunk()).add(entityId);
            return;
        }
        expectedInChunks.computeIfAbsent(lastKnownChunk, $ -> new CheckChunk()).add(entityId);
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

    public void chunkLoaded(ChunkPos chunkPos) {
        var set = expectedInChunks.remove(chunkPos);
        if(set != null) {
            if(checkForExistence.containsKey(chunkPos)) {
                checkForExistence.get(chunkPos).addAll(set.entities);
            } else {
                checkForExistence.put(chunkPos, set);
            }
        }
    }

    public OwnedFloatingNode holderOrPlaceholderNode(@NotNull IWireEndpoint endpoint) {
        var node = globalExternalNodes.get(endpoint);
        if (node != null)
            return node;
        if(endpoint.isValid(world)) {
            node = endpoint.getNode(world);
        } else {
            node = new OwnedFloatingNode(endpoint);
        }
        globalExternalNodes.put(endpoint, node);
        return node;
    }

    public void registerPart(UUID persistentOwnerId, TransmissionLinePart part) {
        lineParts.put(persistentOwnerId, part);
        partNodeMap.computeIfAbsent(part.getNode1(), $ -> new HashSet<>()).add(part);
        partNodeMap.computeIfAbsent(part.getNode2(), $ -> new HashSet<>()).add(part);
        setDirty();
    }

    public void unregisterPart(UUID persistentOwnerId, TransmissionLinePart part) {
        lineParts.remove(persistentOwnerId);
        var set = partNodeMap.get(part.getNode1());
        if(set != null) {
            set.remove(part);
            if(set.isEmpty())
                partNodeMap.remove(part.getNode1());
        }
        set = partNodeMap.get(part.getNode2());
        if(set != null) {
            set.remove(part);
            if(set.isEmpty())
                partNodeMap.remove(part.getNode2());
        }
        setDirty();
    }

    @Nullable
    public TransmissionLinePart getPart(UUID persistentOwnerId) {
        return lineParts.get(persistentOwnerId);
    }

    public void inNetwork(@Nullable ElectricalNetwork network, @NotNull OwnedFloatingNode node) {
        if(network == null)
            return;
        if(node.getNetwork() != null) {
            if(node.getNetwork() != network) {
                network.merge(node.getNetwork());
            }
        } else {
            node.endpoint.joinNetwork(world, network);
        }
    }

    public void movePartMap(OwnedFloatingNode oldNode, OwnedFloatingNode newNode, TransmissionLinePart part) {
        if(oldNode == newNode || oldNode == null || newNode == null)
            return;
        var parts = partNodeMap.get(oldNode);
        if(parts == null)
            return;
        if(parts.remove(part)) {
            if (parts.isEmpty())
                partNodeMap.remove(oldNode);
            partNodeMap.computeIfAbsent(newNode, $ -> new HashSet<>()).add(part);
        }
    }

    private static class CheckChunk {
        public final Set<UUID> entities = Sets.newConcurrentHashSet();
        public int ticks = 0;

        public void add(UUID id) {
            entities.add(id);
            ticks = 0;
        }

        public void addAll(Set<UUID> ids) {
            entities.addAll(ids);
            ticks = 0;
        }
    }
}
