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
import org.patryk3211.powergrid.electricity.sim.special.UnresolvedTransmissionLine;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;
import org.patryk3211.powergrid.network.packets.SolverStateS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineManagementS2CPacket;
import org.patryk3211.powergrid.network.packets.TransmissionLineStateS2CPacket;
import org.patryk3211.powergrid.utility.PlayerLookup;
import org.patryk3211.powergrid.utility.PlayerUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WorldNetworks extends SavedData implements NetworkGraph.IGraphModifyHooks {
    private static final Logger log = LoggerFactory.getLogger(WorldNetworks.class);
    public final Level world;
    public final NetworkGraph globalGraph = new NetworkGraph();

    public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
    public final Map<IElectricNode, TransmissionLine> transmissionLineNodes = new HashMap<>();
    public final Map<Integer, TransmissionLine> transmissionLines = new IntObjectHashMap<>();

    private final Set<UnresolvedTransmissionLine> unresolvedLines = new HashSet<>();
    private final Map<IWireEndpoint, Queue<UnresolvedTransmissionLine>> unresolvedLineNodeMap = new HashMap<>();
    private final Map<UUID, Integer> unresolvedPartHolders = new HashMap<>();
    private final Map<ChunkPos, CheckChunk> expectedInChunks = new HashMap<>();
    private final Map<ChunkPos, CheckChunk> checkForExistence = new HashMap<>();

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
        if(node instanceof OwnedFloatingNode owned) {
            assignTransmissionLine(owned, null);
        }
    }

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
                            var lineId = unresolvedPartHolders.remove(id);
                            if (lineId == null)
                                continue;
                            // Destroy line
                            var line = transmissionLines.get(lineId);
                            if (line == null)
                                continue;
                            line.remove();
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

    protected ElectricalNetwork newNetwork() {
        var network = new GraphedElectricalNetwork(globalGraph);
        subnetworks.add(network);
        return network;
    }

    public void add(IWireEndpoint endpoint) {
        var node = endpoint.getNode(world);
        globalGraph.addNode(node);
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
    protected ElectricWire tryGrabUnloadedPart(IWireEndpoint endpoint1, IWireEndpoint endpoint2, WireEntity forEntity) {
        // Try to resolve trees for correct merging of lines
        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        resolveTree(endpoint1);
        resolveTree(endpoint2);

        // First try with existing lines
        var holderId = unresolvedPartHolders.remove(forEntity.getUUID());
        if(holderId != null) {
            var line = transmissionLines.get(holderId);
            if(line != null) {
                var part = line.grabUnloaded(forEntity);
                if (part != null)
                    return part;
            }
        }

        // TODO: Some of these checks might be redundant
        var line1 = transmissionLineNodes.get(node1);
        if(line1 != null) {
            var part = line1.grabUnloaded(forEntity);
            if(part != null)
                return part;
        }
        for(var line : globalGraph.getConnectedLines(node1)) {
            var part = line.grabUnloaded(forEntity);
            if(part != null)
                return part;
        }

        var line2 = transmissionLineNodes.get(node2);
        if(line2 != null) {
            var part = line2.grabUnloaded(forEntity);
            if(part != null)
                return part;
        }
        for(var line : globalGraph.getConnectedLines(node2)) {
            var part = line.grabUnloaded(forEntity);
            if(part != null)
                return part;
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
                        linePart = new TransmissionLinePart(forEntity.getResistance(), endpoint1, endpoint2, world, forEntity, line1);
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
                    linePart = new TransmissionLinePart(forEntity.getResistance(), endpoint1, endpoint2, world, forEntity, line2);
                    PowerGrid.LOGGER.trace("{}: Extending line at beginning by wire {}, starting node is now {}", line2, linePart, node1);
                    // We can extend this line by the first node.
                    if(line2.getNode1() != node2)
                        line2.flip();
                    line2.addFirstSegment(linePart);
                }
            }
        }
        if(line1 != null) {
            linePart = new TransmissionLinePart(forEntity.getResistance(), endpoint1, endpoint2, world, forEntity, line1);
            // We can extend this line by the second node.
            PowerGrid.LOGGER.trace("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
            if(line1.getNode2() != node1)
                line1.flip();
            line1.addLastSegment(linePart);
        }
        if(line1 == null && line2 == null) {
            linePart = new TransmissionLinePart(forEntity.getResistance(), endpoint1, endpoint2, world, forEntity, null);
            var line = new TransmissionLine(forEntity.getResistance(), endpoint1, endpoint2, linePart, this);
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

    private void addUnresolvedLineMapping(IWireEndpoint endpoint, UnresolvedTransmissionLine line) {
        unresolvedLineNodeMap.computeIfAbsent(endpoint, $ -> new ConcurrentLinkedQueue<>()).add(line);
    }

    public void removeUnresolvedLine(UnresolvedTransmissionLine line) {
        unresolvedLines.remove(line);
        var lines1 = unresolvedLineNodeMap.get(line.endpoint1());
        if(lines1 != null) {
            lines1.remove(line);
            if(lines1.isEmpty())
                unresolvedLineNodeMap.remove(line.endpoint1());
        }
        var lines2 = unresolvedLineNodeMap.get(line.endpoint2());
        if(lines2 != null) {
            lines2.remove(line);
            if(lines2.isEmpty())
                unresolvedLineNodeMap.remove(line.endpoint2());
        }
        setDirty();
    }

    protected void readNbt(CompoundTag nbt) {
        var lineList = nbt.getList("TransmissionLines", Tag.TAG_COMPOUND);
        for(var lineEntryGeneric : lineList) {
            var lineEntry = (CompoundTag) lineEntryGeneric;
            var line = new UnresolvedTransmissionLine(lineEntry);
            unresolvedLines.add(line);
            addUnresolvedLineMapping(line.endpoint1(), line);
            addUnresolvedLineMapping(line.endpoint2(), line);
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
            var unresolved = line.unresolve();
            unresolvedLines.add(unresolved);
            setDirty();
            addUnresolvedLineMapping(unresolved.endpoint1(), unresolved);
            addUnresolvedLineMapping(unresolved.endpoint2(), unresolved);
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

    private void resolveLine(UnresolvedTransmissionLine line) {
        // If node already exists then the resolving code must have triggered for it (no need to resolve again)
        var node1 = globalExternalNodes.get(line.endpoint1());
        if(node1 == null) {
            node1 = new OwnedFloatingNode(line.endpoint1());
            globalExternalNodes.put(line.endpoint1(), node1);
            line.resolveEnd(this, line.endpoint1());
        }
        var node2 = globalExternalNodes.get(line.endpoint2());
        if(node2 == null) {
            node2 = new OwnedFloatingNode(line.endpoint2());
            globalExternalNodes.put(line.endpoint2(), node2);
            line.resolveEnd(this, line.endpoint2());
        }
    }

    private boolean traceTree(IWireEndpoint endpoint, Set<IWireEndpoint> visited) {
        if(!visited.add(endpoint))
            return false;
        var lines = unresolvedLineNodeMap.get(endpoint);
        if(lines == null)
            return false;
        boolean continueResolving = false;
        for(var line : lines) {
            if(lines.size() == 1) {
                if(line.endpoint1().equals(endpoint)) {
                    // Check endpoint2
                    if(line.endpoint2().isValid(world)) {
                        // Resolve segment
                        resolveLine(line);
                        return true;
                    }
                } else {
                    assert line.endpoint2().equals(endpoint);
                    // Check endpoint1
                    if(line.endpoint1().isValid(world)) {
                        // Resolve segment
                        resolveLine(line);
                        return true;
                    }
                }
                break;
            }
            if(line.endpoint1().equals(endpoint)) {
                if(traceTree(line.endpoint2(), visited)) {
                    resolveLine(line);
                    continueResolving = true;
                }
            } else {
                assert line.endpoint2().equals(endpoint);
                if(traceTree(line.endpoint1(), visited)) {
                    resolveLine(line);
                    continueResolving = true;
                }
            }
        }
        return continueResolving;
    }

    private void resolveTree(@NotNull IWireEndpoint endpoint) {
        var unresolvedLines = unresolvedLineNodeMap.get(endpoint);
        if(unresolvedLines == null)
            return;
        // We need to trace the graph to all terminating nodes and see if any are loaded,
        // if so, we need to resolve all lines between them to ensure correct unloaded chunk behaviour.
        var visited = new HashSet<IWireEndpoint>();
        visited.add(endpoint);
        for(var line : unresolvedLines) {
            line.resolveEnd(this, endpoint);
            if(line.endpoint1().equals(endpoint)) {
                traceTree(line.endpoint2(), visited);
            } else {
                assert line.endpoint2().equals(endpoint);
                traceTree(line.endpoint1(), visited);
            }
        }
    }

    public void nodeHolderAdded(@NotNull OwnedFloatingNode ownedNode) {
        var oldNode = globalExternalNodes.put(ownedNode.endpoint, ownedNode);
        if(oldNode != null && oldNode != ownedNode) {
            // Migrate connections into the new node.
            // This happens when a block entity is loaded but its terminal was acting as a transmission line junction.
            if(oldNode.getNetwork() != null) {
                prepareForConnection(ownedNode, oldNode);
                var lines = List.copyOf(globalGraph.getConnectedLines(oldNode));
                for (var line : lines) {
                    if (line.getNode1() == oldNode) {
                        line.setNode1(ownedNode);
                    } else if (line.getNode2() == oldNode) {
                        line.setNode2(ownedNode);
                    }
                }
                oldNode.getNetwork().removeNode(oldNode);
            }
        }
        // Try to resolve an end of a transmission line
        resolveTree(ownedNode.endpoint);
    }

    /**
     * Save an entity to be recovered into the transmission line once its chunk is loaded back into the world.
     *
     * @param entityId       Transmission line part entity id
     * @param lastKnownChunk Last known chunk of the entity
     * @param linePartHolder Transmission line which holds the part
     */
    public void bounty(UUID entityId, ChunkPos lastKnownChunk, TransmissionLine linePartHolder) {
        unresolvedPartHolders.put(entityId, linePartHolder.getId());
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

    @NotNull
    public OwnedFloatingNode holderOrPlaceholderNode(@NotNull IWireEndpoint endpoint) {
        var node = globalExternalNodes.get(endpoint);
        if(node != null)
            return node;
        node = new OwnedFloatingNode(endpoint);
        globalExternalNodes.put(endpoint, node);
        return node;
    }

    private static class CheckChunk {
        public final Set<UUID> entities = new HashSet<>();
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
