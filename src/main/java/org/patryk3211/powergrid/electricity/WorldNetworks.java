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
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.config.CSolver;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ISynchronizedElement;
import org.patryk3211.powergrid.electricity.sim.*;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePart;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLinePort;
import org.patryk3211.powergrid.electricity.wire.*;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlockEntity;
import org.patryk3211.powergrid.network.packets.NegotiateSyncC2SPacket;
import org.patryk3211.powergrid.network.packets.StateS2CPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldNetworks extends SavedData implements NetworkGraph.IGraphModifyHooks {
    static final int ENTITY_LOAD_GRACE_TICKS = 10;
    static final int MAX_AUTOMATIC_REBUILD_RETRIES = 3;
    static final int REBUILD_RETRY_INTERVAL_TICKS = 20;
    static final int MAX_REBUILD_RETRIES_PER_TICK = 16;

    enum TransmissionPartSide {
        FIRST,
        SECOND,
        NONE
    }

    enum NodeBindingMode {
        BIND_ONLY,
        BIND_AND_SPLIT
    }

    public final Level world;
    public final NetworkGraph globalGraph = new NetworkGraph();
    protected final PerformanceCounter perf;

    public final List<ElectricalNetwork> subnetworks = new ArrayList<>();
    public final Map<Integer, TransmissionLine> transmissionLines = new IntObjectHashMap<>();
    public final Map<IWireEndpoint, OwnedFloatingNode> globalExternalNodes = new HashMap<>();

    private final Map<ChunkPos, CheckChunk> expectedInChunks = new ConcurrentHashMap<>();
    private final Map<ChunkPos, CheckChunk> checkForExistence = new ConcurrentHashMap<>();
    private final Map<PartId, TransmissionLinePart> lineParts = new HashMap<>();
    private final Map<OwnedFloatingNode, Set<TransmissionLinePart>> partNodeMap = new HashMap<>();

    private final Map<IWireEndpoint, Set<ServerPlayer>> trackers = new HashMap<>();

    protected final Set<TransmissionLinePart> deferredRewireEntities = new HashSet<>();
    protected final Set<ElectricalNetwork> islandDiscoveryQueue = new HashSet<>();
    private final Map<PartId, RebuildRetry> rebuildRetryQueue = new HashMap<>();
    private TransmissionNetworkRebuildJob verifiedRebuildJob;
    private boolean startupRebuildStarted;
    private boolean verifiedRebuildIsolated;
    private boolean assemblingTransmissionTopology;
    private int verifiedRebuildLinesBefore;
    private boolean runningDiscovery = false;
    private int syncTicks = 0;

    private CompoundTag nbt;

    private record SyncState(int lod) { }
    private final Map<ServerPlayer, Map<ISynchronizedElement, SyncState>> syncStates = new HashMap<>();

    public record RebuildResult(int parts, int linesBefore, int linesAfter, int retryingParts, int retryLimit) {
        public boolean complete() {
            return retryingParts == 0;
        }
    }

    public enum VerifiedRebuildStartStatus {
        STARTED,
        ALREADY_RUNNING,
        TOO_MANY_CHUNKS
    }

    public record VerifiedRebuildStart(
            VerifiedRebuildStartStatus status,
            @Nullable TransmissionNetworkRebuildJob.StartInfo info,
            int requestedChunks
    ) {
    }

    private static class RebuildRetry {
        private final TransmissionLinePart part;
        private final RetryBudget budget = new RetryBudget(MAX_AUTOMATIC_REBUILD_RETRIES);
        private long nextAttemptTick;

        private RebuildRetry(TransmissionLinePart part, long nextAttemptTick) {
            this.part = part;
            this.nextAttemptTick = nextAttemptTick;
        }
    }

    public WorldNetworks(Level world) {
        this.world = world;
        this.globalGraph.hooks = this;
        this.perf = new PerformanceCounter(world.dimension().location().toString());
    }

    public WorldNetworks(Level world, CompoundTag nbt) {
        this(world);
        this.nbt = nbt;
    }

    void completeLoad() {
        if(nbt != null) {
            readNbt(nbt);
            nbt = null;
        }
    }

    @Override
    public void lineConnected(TransmissionLine line) {
        var node1 = Objects.requireNonNull(
                line.getNode1(),
                "Transmission lines cannot connect without a first node"
        );
        var node2 = Objects.requireNonNull(
                line.getNode2(),
                "Transmission lines cannot connect without a second node"
        );
        var id = line.getId();
        transmissionLines.put(id, line);

        var line1 = findLineMiddle(node1);
        if(line1 != null)
            line1.splitAt(node1);
        var line2 = findLineMiddle(node2);
        if(line2 != null)
            line2.splitAt(node2);
        scheduleIslandDiscovery(node1.getNetwork());
        scheduleIslandDiscovery(node2.getNetwork());
        setDirty();
    }

    @Override
    public void lineDisconnected(TransmissionLine line) {
        transmissionLines.remove(line.getId());
        scheduleIslandDiscovery(line.getNetwork());
        setDirty();
    }

    public static boolean canWeakCouple(TransmissionLine line) {
        return ModdedConfigs.server().electricity.solver.splittingTransmissionLines.get() &&
                line.getResistance() > ModdedConfigs.server().electricity.solver.transmissionLineThreshold.getF();
    }

    public void scheduleIslandDiscovery(ElectricalNetwork network) {
        if(network != null && !runningDiscovery && !verifiedRebuildIsolated)
            islandDiscoveryQueue.add(network);
    }

    private void runIslandDiscoveryFor(ElectricalNetwork network) {
        var visited = new HashSet<IElectricNode>();
        var islands = new ArrayList<Island>();
        var couplings = new HashMap<CouplingKey, Set<TransmissionLine>>();
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("Running island discovery for {}", network);

        var queue = new ArrayList<>(network.getNodes());
        queue.addAll(network.getLeafs());
        while(!queue.isEmpty()) {
            var node = queue.remove(0);
            if(!(node instanceof IElectricNode enode))
                continue;
            if (!visited.add(enode))
                continue;
            Island island = null;
            for(var otherIsland : islands) {
                if(otherIsland.contains(node)) {
                    island = otherIsland;
                    break;
                }
            }
            if(island == null) {
                island = new Island(couplings);
                islands.add(island);
            }
            island.add(enode);
            for(var wire : globalGraph.getWires(enode)) {
                if(wire instanceof TransmissionLine line && canWeakCouple(line)) {
                    // Weak line can split islands.
                    var otherNode = line.getNode1() == node ? line.getNode2() : line.getNode1();
                    Island connectedIsland = null;
                    for(var otherIsland : islands) {
                        if(otherIsland.contains(otherNode)) {
                            connectedIsland = otherIsland;
                            break;
                        }
                    }
                    if(connectedIsland == null) {
                        connectedIsland = new Island(couplings);
                        connectedIsland.add(otherNode);
                        islands.add(connectedIsland);
                    }
                    if(connectedIsland != island) {
                        island.addCoupling(connectedIsland, line);
                        queue.add(otherNode);
                        continue;
                    }
                }
                for(var otherNode : wire.coupledNodes()) {
                    if(otherNode == node)
                        continue;
                    island.add(otherNode);
                    queue.add(otherNode);
                    for(var otherIsland : islands) {
                        if(otherIsland == island)
                            continue;
                        if(otherIsland.contains(otherNode)) {
                            // Merge islands
                            otherIsland.addAll(island);
                            islands.remove(island);
                            island = otherIsland;
                            break;
                        }
                    }
                }
                island.add(wire);
            }
            var remove = new ArrayList<ICouplingNode>();
            for(var coupling : globalGraph.getCouplings(enode)) {
                if(coupling instanceof TransmissionLinePort) {
                    // This should be handled by transmission lines above
                    remove.add(coupling);
                    continue;
                }
                for(var otherNode : coupling.coupledNodes()) {
                    if(otherNode == node)
                        continue;
                    island.add(otherNode);
                    queue.add(otherNode);
                    for(var otherIsland : islands) {
                        if(otherIsland == island)
                            continue;
                        if(otherIsland.contains(otherNode)) {
                            // Merge islands
                            otherIsland.addAll(island);
                            islands.remove(island);
                            island = otherIsland;
                            break;
                        }
                    }
                }
                island.add(coupling);
            }
            remove.forEach(INode::remove);
        }
        if(islands.size() <= 1)
            return;
        for(var island : islands) {
            var islandNetwork = newNetwork();
            islandNetwork.fromElements(island.elements);
        }
        for(var lines : couplings.values()) {
            for(var line : lines) {
                line.setNetwork(null);
                line.makePortPair();
            }
        }
        network.clear();
        subnetworks.remove(network);
        network.cleanup();
    }

    public void preTick() {
        if(!verifiedRebuildIsolated) {
            deferredRewireEntities.removeIf(part -> {
                part.refreshEndpointNodes();
                return true;
            });
        }
        JunctionWireEndpoint.processNewNodes(world);
        startStartupRebuildIfNeeded();
        tickVerifiedRebuild();
        if(verifiedRebuildIsolated) {
            // Physical entities keep ticking so the current chunk batch can
            // register itself, but no partially rebuilt transmission topology
            // may enter island discovery or the electrical solver.
            islandDiscoveryQueue.clear();
            return;
        }
        processRebuildRetries();

        runningDiscovery = true;
        for(var network : islandDiscoveryQueue) {
            runIslandDiscoveryFor(network);
        }
        islandDiscoveryQueue.clear();
        runningDiscovery = false;

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
            line.tick();
        }
        removed.forEach(TransmissionLine::remove);

        perf.start();
        int multiTick = ModdedConfigs.server().electricity.solver.multiTicks.get();
        var iter = subnetworks.iterator();
        while (iter.hasNext()) {
            var network = iter.next();
            if (network.isEmpty()) {
                iter.remove();
                network.cleanup();
                continue;
            }
            network.prepare(multiTick);
        }
        for(int i = 0; i < multiTick; ++i) {
            // I guess this could go on a thread-pool
            for(var network : subnetworks) {
                network.singleTick();
            }
        }
        perf.end();
    }

    public boolean verifiedRebuildInProgress() {
        return verifiedRebuildJob != null && !verifiedRebuildJob.isFinished();
    }

    public VerifiedRebuildStart startVerifiedRebuild(TransmissionNetworkRebuildJob.Listener listener) {
        if(!(world instanceof ServerLevel serverWorld))
            throw new IllegalStateException("Verified rebuilds require a server level");
        if(verifiedRebuildInProgress()) {
            return new VerifiedRebuildStart(
                    VerifiedRebuildStartStatus.ALREADY_RUNNING,
                    verifiedRebuildJob.startInfo(),
                    verifiedRebuildJob.startInfo().uniqueChunks()
            );
        }

        rebuildRetryQueue.clear();
        try {
            var job = TransmissionNetworkRebuildJob.create(this, serverWorld, listener);
            beginVerifiedRebuildIsolation();
            verifiedRebuildJob = job;
            return new VerifiedRebuildStart(
                    VerifiedRebuildStartStatus.STARTED,
                    verifiedRebuildJob.startInfo(),
                    verifiedRebuildJob.startInfo().uniqueChunks()
            );
        } catch(TransmissionNetworkRebuildJob.TooManyChunksException exception) {
            return new VerifiedRebuildStart(
                    VerifiedRebuildStartStatus.TOO_MANY_CHUNKS,
                    null,
                    exception.chunks()
            );
        }
    }

    public void close() {
        if(verifiedRebuildJob != null)
            verifiedRebuildJob.cancel();
        verifiedRebuildJob = null;
    }

    private void startStartupRebuildIfNeeded() {
        if(startupRebuildStarted || !(world instanceof ServerLevel))
            return;
        startupRebuildStarted = true;
        var start = startVerifiedRebuild(new TransmissionNetworkRebuildJob.Listener() {
            @Override
            public void completed(TransmissionNetworkRebuildJob.Outcome outcome) {
                if(outcome.complete()) {
                    PowerGrid.LOGGER.info(
                            "Startup transmission rebuild completed successfully in {}",
                            world.dimension().location()
                    );
                } else {
                    PowerGrid.LOGGER.warn(
                            "Startup transmission rebuild completed partially in {}: {} unresolved parts, {} parts queued for retry",
                            world.dimension().location(),
                            outcome.unresolvedParts(),
                            outcome.rebuild().retryingParts()
                    );
                }
            }

            @Override
            public void failed(TransmissionNetworkRebuildJob.Failure failure) {
                PowerGrid.LOGGER.error(
                        "Startup transmission rebuild failed in {}: {}",
                        world.dimension().location(),
                        failure.reason()
                );
            }
        });
        if(start.status() == VerifiedRebuildStartStatus.TOO_MANY_CHUNKS) {
            PowerGrid.LOGGER.error(
                    "Startup transmission rebuild refused in {}: {} unique chunks exceeds the limit of {}",
                    world.dimension().location(),
                    start.requestedChunks(),
                    TransmissionNetworkRebuildJob.MAX_UNIQUE_CHUNKS
            );
        } else if(start.status() == VerifiedRebuildStartStatus.STARTED) {
            var info = Objects.requireNonNull(start.info());
            PowerGrid.LOGGER.info(
                    "Startup transmission rebuild scheduled in {}: {} physical parts, {} unique chunks, {} batches",
                    world.dimension().location(),
                    info.parts(),
                    info.uniqueChunks(),
                    info.batches()
            );
        }
    }

    private void tickVerifiedRebuild() {
        if(verifiedRebuildJob == null)
            return;
        verifiedRebuildJob.tick();
        if(verifiedRebuildJob.isFinished())
            verifiedRebuildJob = null;
    }

    public void postTick() {
        if(verifiedRebuildIsolated)
            return;
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
                        entry.getValue().resetTicks();
                        expectedInChunks.put(chunk, entry.getValue());
                    }
                    checkIter.remove();
                    continue;
                }
                var chunkCheckPos = new BlockPos(
                        chunk.getMinBlockX(),
                        serverWorld.getMinBuildHeight(),
                        chunk.getMinBlockZ()
                );
                if(!serverWorld.isPositionEntityTicking(chunkCheckPos)) {
                    entry.getValue().resetTicks();
                    continue;
                }
                var remove = entry.getValue().advanceEntityLoadCheck();
                var entityIter = entry.getValue().entities.iterator();
                while(entityIter.hasNext()) {
                    var id = entityIter.next();
                    if(id.getEntity(serverWorld) == null) {
                        if(remove) {
                            // Doesn't exist even after chunk has been loaded.
                            var part = lineParts.get(id);
                            if (part == null) {
                                entityIter.remove();
                                continue;
                            }
                            // Only the missing physical segment is invalid. Removing the
                            // entire optimized line would orphan every surviving segment.
                            part.remove();
                            entityIter.remove();
                        }
                    } else {
                        entityIter.remove();
                    }
                }
                if(remove || entry.getValue().entities.isEmpty()) {
                    checkIter.remove();
                }
            }
            // Synchronize state with clients
            if(syncTicks % 5 == 0) {
                syncStates.clear();
                var trackerEntryIter = trackers.entrySet().iterator();
                while(trackerEntryIter.hasNext()) {
                    var entry = trackerEntryIter.next();
                    var playerIter = entry.getValue().iterator();
                    while(playerIter.hasNext()) {
                        var player = playerIter.next();
                        if(player.isRemoved()) {
                            playerIter.remove();
                            continue;
                        }
                        var endpoint = entry.getKey();
                        if(endpoint instanceof BlockWireEndpoint bwe) {
                            var eb = bwe.getElectricBehaviour(world);
                            if (eb == null)
                                continue;
                            var ebPos = eb.getPos();
                            var syncState = new SyncState((int) (Math.sqrt(player.distanceToSqr(ebPos.getX(), ebPos.getY(), ebPos.getZ())) / 24 + 1));
                            if(eb.blockEntity instanceof WindingBlockEntity winding) {
                                winding.forSync(sync -> {
                                    if(sync == null)
                                        return;
                                    syncStates.computeIfAbsent(player, $ -> new HashMap<>())
                                            .put(sync, syncState);
                                });
                            } else {
                                syncStates.computeIfAbsent(player, $ -> new HashMap<>())
                                        .put(eb, syncState);
                            }
                        } else if(endpoint instanceof JunctionWireEndpoint je) {
                            var syncEntry = je.makeSyncEntry(world);
                            var jePos = je.getExactPosition(world);
                            if(syncEntry != null)
                                syncStates.computeIfAbsent(player, $ -> new HashMap<>())
                                        .put(syncEntry, new SyncState((int) (Math.sqrt(player.distanceToSqr(jePos)) / 24 + 1)));
                        } else if(endpoint instanceof CircuitBoardEndpoint cbe) {
                            // Circuits might not have external terminals so they need a special tracking entry
                            var eb = cbe.getElectricBehaviour(world);
                            if (eb == null)
                                continue;
                            var ebPos = eb.getPos();
                            syncStates.computeIfAbsent(player, $ -> new HashMap<>())
                                    .put(eb, new SyncState((int) (Math.sqrt(player.distanceToSqr(ebPos.getX(), ebPos.getY(), ebPos.getZ())) / 24 + 1)));
                        }
                    }
                    if(entry.getValue().isEmpty()) {
                        trackerEntryIter.remove();
                    }
                }
            }
            for(var entry : syncStates.entrySet()) {
                boolean useDoubles = NegotiateSyncC2SPacket.useDoubles(entry.getKey());
                var packet = new StateS2CPacket(useDoubles);
                var wrapper = packet.wrapper();
                var behaviours = entry.getValue();
                for(var pair : behaviours.entrySet()) {
                    if(syncTicks % pair.getValue().lod() != 0)
                        continue;
                    if(pair.getKey() == null)
                        continue;
                    packet.begin(pair.getKey());
                    pair.getKey().writeToSync(wrapper, useDoubles, this::findLineMiddle);
                    packet.end();
                }
                ModdedPackets.sendToClient(packet, entry.getKey());
            }
            final int syncInterval = ModdedConfigs.common().stateSynchronization.get();
            if(syncInterval > 0) {
                if (syncTicks >= syncInterval) {
                    // TODO: Perhaps we should avoid sending ALL subnetworks at once and instead
                    //  split the sync up to avoid generating a lot of intermittent network traffic.
                    for (var network : subnetworks) {
                        for (var node : network.getNodes()) {
                            if (!(node instanceof OwnedFloatingNode owned))
                                continue;
                            if (!(owned.endpoint instanceof BlockWireEndpoint bwe))
                                continue;
                            var behaviour = bwe.getElectricBehaviour(world);
                            if (behaviour != null)
                                behaviour.blockEntity.sendData();
                        }
                    }
                    syncTicks = 0;
                }
            }
            ++syncTicks;
        }
    }

    public ElectricalNetwork newNetwork() {
        var cSolver = ModdedConfigs.server().electricity.solver;
        var backend = cSolver.solverBackend.get();
        if(!backend.isSupported())
            backend = CSolver.SolverBackend.JAVA;
        var network = new GraphedElectricalNetwork(globalGraph, true, backend::create);
        network.maxIterations = hooks -> hooks
                ? cSolver.solverComplexMaxIterations.get()
                : cSolver.solverSimpleMaxIterations.get();
        var rA = cSolver.solverAbsolutePrecision.get();
        var rR = cSolver.solverRelativePrecision.get();
        var rM = cSolver.solverAbsoluteMinimumPrecision.get();
        var sA = cSolver.solverMaxSearchAlpha.get();
        network.setPrecision(rA, rR, rM, sA);
        network.bjtSmoothAlpha = cSolver.bjtLimAlpha.getF();
        network.diodeSmoothAlpha = cSolver.diodeLimAlpha.getF();
        network.triodeLimCathode = cSolver.triodeLimCathode.getF();
        network.triodeLimAnode = cSolver.triodeLimAnode.getF();
        network.triodeLimGrid = cSolver.triodeLimGrid.getF();
        subnetworks.add(network);
        return network;
    }

    public void add(IWireEndpoint endpoint) {
        var node = endpoint.getNode(world);
        globalGraph.addNode(node);
        addAndMigrateNode(endpoint);
    }

    public void putInNetwork(@NotNull IWireEndpoint endpoint) {
        var node = endpoint.getNode(world);
        add(endpoint);
        var line = findLineMiddle(node);
        if(line != null)
            return;
        if(node.getNetwork() == null)
            endpoint.joinNetwork(world, newNetwork());
    }

    public int connectionCount(IWireEndpoint endpoint) {
        return globalGraph.connectionCount(endpoint.getNode(world));
    }

    @Nullable
    public TransmissionLine findLineMiddle(OwnedFloatingNode node) {
        var parts = partNodeMap.get(node);
        if(parts == null)
            return null;
        for(var part1 : parts) {
            for(var part2 : parts) {
                if(part1 == part2)
                    continue;
                if(part1.getLine() == part2.getLine() && part1.getLine() != null)
                    return part1.getLine();
            }
        }
        return null;
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
        var line1 = findLineMiddle(node1);
        if(line1 != null)
            line1.splitAt(node1);
        var line2 = findLineMiddle(node2);
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

    public ElectricalNetwork prepareForTransmissionLine(@NotNull OwnedFloatingNode node1, @NotNull OwnedFloatingNode node2, TransmissionLine line, Runnable callback) {
        var endpoint1 = node1.endpoint;
        var endpoint2 = node2.endpoint;

        if(node1 == node2)
            return null;

        var nNode1 = endpoint1.getNode(world);
        if(node1 != nNode1) {
            addAndMigrateNode(node1, nNode1);
            node1 = nNode1;
        }
        var nNode2 = endpoint2.getNode(world);
        if(node2 != nNode2) {
            addAndMigrateNode(node2, nNode2);
            node2 = nNode2;
        }

        add(endpoint1);
        add(endpoint2);

        var net1 = node1.getNetwork();
        var net2 = node2.getNetwork();

        ElectricalNetwork network = line.getNetwork();
        if(network != null) {
            inNetwork(network, node1);
            inNetwork(network, node2);
            callback.run();
            return network;
        }
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
        // We must disconnect the line or else graph will be broken.
        globalGraph.disconnect(line.getNode1(), line.getNode2(), line);
        callback.run();
        network.addWire(line);
        return network;
    }

    /**
     * Connect a newly assembled transmission line only after rebinding it to the
     * endpoint nodes selected during connection preparation.
     */
    public void addPreparedTransmissionLine(
            @NotNull ElectricalNetwork network,
            @NotNull TransmissionLine line
    ) {
        if(line.getNetwork() != null)
            throw new IllegalStateException("Transmission line is already connected");

        line.refreshDetachedEndpointNodes();
        inNetwork(network, line.getNode1(), line.getEndpoint1());
        inNetwork(network, line.getNode2(), line.getEndpoint2());
        if(!network.ownsNode(line.getNode1()) || !network.ownsNode(line.getNode2())) {
            throw new IllegalStateException(
                    "Prepared transmission line endpoints do not belong to the target network"
                            + " (endpoint1=" + line.getEndpoint1()
                            + ", node1=" + line.getNode1()
                            + ", node1Network=" + line.getNode1().getNetwork()
                            + ", endpoint2=" + line.getEndpoint2()
                            + ", node2=" + line.getNode2()
                            + ", node2Network=" + line.getNode2().getNetwork()
                            + ", targetNetwork=" + network + ")"
            );
        }
        network.addWire(line);
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
        var line1 = findLineMiddle(node1);
        if(line1 != null)
            line1.splitAt(node1);
        var line2 = findLineMiddle(node2);
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
    protected TransmissionLinePart tryGrabUnloadedPart(IWireEndpoint endpoint1, IWireEndpoint endpoint2, BaseWireEntity forEntity, PartId id) {
        // Try to resolve trees for correct merging of lines
        resolveTree(endpoint1);
        resolveTree(endpoint2);

        var existingPart = lineParts.get(id);
        if(existingPart != null) {
            if(!existingPart.endpointsMatch(endpoint1, endpoint2)) {
                PowerGrid.LOGGER.warn(
                        "Replacing stale transmission part {} from physical owner: stored=({}, {}), current=({}, {})",
                        id,
                        existingPart.getConnectionEndpoint1(),
                        existingPart.getConnectionEndpoint2(),
                        endpoint1,
                        endpoint2
                );
                // Remove only the stale part. Removing its optimized
                // TransmissionLine would unregister every healthy segment.
                existingPart.remove();
                return null;
            }
            if(existingPart.grab(forEntity, id))
                return existingPart;
            return null;
        }

        return null;
    }

    private boolean makeTransmissionLine(TransmissionLinePart linePart) {
        if(!transmissionTopologyResolutionAllowed())
            return false;
        if(linePart.getLine() != null)
            return true;

        var endpoint1 = linePart.getEndpoint1();
        var endpoint2 = linePart.getEndpoint2();
        linePart.refreshEndpointNodes();
        var node1 = linePart.getNode1();
        var node2 = linePart.getNode2();

        // This method needs to ensure proper ordering of segments in the transmission line.
        var network = prepareForConnection(node1, node2);
        if(network == null)
            return false;

        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("Creating a transmission line for {}", linePart);

        int nConns1 = connectionCount(endpoint1);
        int nConns2 = connectionCount(endpoint2);
        List<IElectricNode> nodes;
        var connected1 = nConns1 == 1 ? !(nodes = globalGraph.getConnectedNodes(node1)).isEmpty() ? nodes.get(0) : null : null;
        var connected2 = nConns2 == 1 ? !(nodes = globalGraph.getConnectedNodes(node2)).isEmpty() ? nodes.get(0) : null : null;

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
                        if(ModdedConfigs.logsEnabled())
                            PowerGrid.LOGGER.debug("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
                        if(line1.getNode2() != node1)
                            line1.flip();
                        line1.addLastSegment(linePart);
                        // We need to merge lines.
                        if(ModdedConfigs.logsEnabled())
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
                    if(ModdedConfigs.logsEnabled())
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
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("{}: Extending line at end by wire {}, terminating node is now {}", line1, linePart, node2);
            if(line1.getNode2() != node1)
                line1.flip();
            line1.addLastSegment(linePart);
        }
        if(line1 == null && line2 == null) {
            var line = new TransmissionLine(
                    linePart.getResistance(),
                    endpoint1,
                    endpoint2,
                    node1,
                    node2,
                    this
            );
            linePart.setLine(line);
            line.segments.add(linePart);
            addPreparedTransmissionLine(network, line);
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("{}: New transmission line between {} and {}", line, node1, node2);
        }

        setDirty();
        return true;
    }

    @Nullable
    public ElectricWire makeSimpleWire(IWireEndpoint endpoint1, IWireEndpoint endpoint2, float resistance) {
        var network = prepareForConnection(endpoint1, endpoint2);
        if(network == null)
            return null;

        var node1 = endpoint1.getNode(world);
        var node2 = endpoint2.getNode(world);

        var wire = new ElectricWire(resistance, node1, node2);
        network.addWire(wire);

        setDirty();
        return wire;
    }

    @Nullable
    public ElectricWire makeTransmissionLine(IWireEndpoint endpoint1, IWireEndpoint endpoint2, BaseWireEntity forEntity, PartId id) {
        if(verifiedRebuildIsolated && !assemblingTransmissionTopology)
            return registerIsolatedTransmissionPart(endpoint1, endpoint2, forEntity, id);

        add(endpoint1);
        add(endpoint2);

        var linePart = tryGrabUnloadedPart(endpoint1, endpoint2, forEntity, id);
        if(linePart != null && linePart.getLine() != null)
            return linePart;

        if(linePart == null)
            linePart = TransmissionLinePart.uniquePart(forEntity.getResistance(), endpoint1, endpoint2, forEntity, this, id);

        if(makeTransmissionLine(linePart))
            return linePart;
        return null;
    }

    @Nullable
    private TransmissionLinePart registerIsolatedTransmissionPart(
            IWireEndpoint endpoint1,
            IWireEndpoint endpoint2,
            BaseWireEntity forEntity,
            PartId id
    ) {
        var existingPart = lineParts.get(id);
        if(existingPart != null && !existingPart.endpointsMatch(endpoint1, endpoint2)) {
            PowerGrid.LOGGER.warn(
                    "Replacing stale transmission part {} from physical owner while rebuild topology is isolated: stored=({}, {}), current=({}, {})",
                    id,
                    existingPart.getConnectionEndpoint1(),
                    existingPart.getConnectionEndpoint2(),
                    endpoint1,
                    endpoint2
            );
            existingPart.remove();
            existingPart = null;
        }
        if(existingPart != null)
            return existingPart.grab(forEntity, id) ? existingPart : null;
        return TransmissionLinePart.uniquePart(
                forEntity.getResistance(),
                endpoint1,
                endpoint2,
                forEntity,
                this,
                id
        );
    }

    public List<TransmissionLinePart> findConnectedWires(ElectricBehaviour behaviour) {
        var wires = new ArrayList<TransmissionLinePart>();
        for(var node : behaviour.getExternalNodes()) {
            var parts = partNodeMap.get(node);
            if(parts == null)
                continue;
            wires.addAll(parts);
        }
        return wires;
    }

    public List<TransmissionLinePart> findConnectedWires(IWireEndpoint endpoint) {
        var parts = partNodeMap.get(endpoint.getNode(world));
        if(parts == null)
            return null;
        return List.copyOf(parts);
    }

    public void deferredRewire(Collection<TransmissionLinePart> wires) {
        deferredRewireEntities.addAll(wires);
    }

    public void deferredRewire(TransmissionLinePart part) {
        deferredRewireEntities.add(part);
    }

    /**
     * Rebuild the derived transmission-line topology from the persisted physical
     * line parts. Placed blocks, wire entities and saved line parts are preserved.
     * This must run on the owning server thread.
     */
    public RebuildResult rebuildTransmissionNetwork() {
        return rebuildTransmissionNetwork(-1);
    }

    private RebuildResult rebuildTransmissionNetwork(int linesBeforeOverride) {
        if(world.isClientSide)
            throw new IllegalStateException("Transmission networks can only be rebuilt on the server");

        rebuildRetryQueue.clear();
        var parts = new ArrayList<>(lineParts.values());
        parts.sort(Comparator.comparing(part -> part.persistentOwnerId.toString()));

        var detachedLines = detachDerivedTransmissionTopology();
        var linesBefore = linesBeforeOverride >= 0 ? linesBeforeOverride : detachedLines;

        for(var part : parts) {
            part.setLine(null);
            restorePartEndpointNodes(part);
        }

        // Recreate the node index from the persisted source of truth as well. The
        // regular entity reload path will continue to migrate entries afterwards.
        partNodeMap.clear();
        for(var part : parts) {
            partNodeMap.computeIfAbsent(part.getNode1(), $ -> new HashSet<>()).add(part);
            partNodeMap.computeIfAbsent(part.getNode2(), $ -> new HashSet<>()).add(part);
        }
        deferredRewireEntities.clear();

        int retryingParts = 0;
        assemblingTransmissionTopology = true;
        try {
            for(var part : parts) {
                if(!makeTransmissionLine(part)) {
                    scheduleRebuildRetry(part);
                    logRebuildFailure(part, 0, false, null);
                    ++retryingParts;
                }
            }
        } finally {
            assemblingTransmissionTopology = false;
        }

        for(var network : subnetworks)
            network.setDirty();
        setDirty();

        var result = new RebuildResult(
                parts.size(),
                linesBefore,
                transmissionLines.size(),
                retryingParts,
                MAX_AUTOMATIC_REBUILD_RETRIES
        );
        PowerGrid.LOGGER.info(
                "Rebuilt transmission network in {}: {} physical parts, {} lines before, {} lines after, {} parts queued for at most {} automatic retries",
                world.dimension().location(),
                result.parts(),
                result.linesBefore(),
                result.linesAfter(),
                result.retryingParts(),
                result.retryLimit()
        );
        return result;
    }

    private int detachDerivedTransmissionTopology() {
        var oldLines = Collections.newSetFromMap(new IdentityHashMap<TransmissionLine, Boolean>());
        oldLines.addAll(transmissionLines.values());
        for(var part : lineParts.values()) {
            if(part.getLine() != null)
                oldLines.add(part.getLine());
        }

        for(var line : oldLines)
            line.detachForRebuild();
        transmissionLines.clear();
        for(var part : lineParts.values())
            part.setLine(null);
        return oldLines.size();
    }

    private void beginVerifiedRebuildIsolation() {
        if(verifiedRebuildIsolated)
            throw new IllegalStateException("Verified rebuild topology is already isolated");

        verifiedRebuildIsolated = true;
        islandDiscoveryQueue.clear();
        rebuildRetryQueue.clear();
        try {
            verifiedRebuildLinesBefore = detachDerivedTransmissionTopology();
        } catch(RuntimeException exception) {
            verifiedRebuildIsolated = false;
            throw exception;
        }
    }

    RebuildResult completeVerifiedRebuildIsolation() {
        if(!verifiedRebuildIsolated)
            throw new IllegalStateException("Verified rebuild topology is not isolated");

        var result = rebuildTransmissionNetwork(verifiedRebuildLinesBefore);
        verifiedRebuildIsolated = false;
        verifiedRebuildLinesBefore = 0;
        scheduleDiscoveryForAllNetworks();
        return result;
    }

    RebuildResult refreshVerifiedRebuildResult(RebuildResult initialResult) {
        return new RebuildResult(
                lineParts.size(),
                initialResult.linesBefore(),
                transmissionLines.size(),
                rebuildRetryQueue.size(),
                MAX_AUTOMATIC_REBUILD_RETRIES
        );
    }

    void abortVerifiedRebuildIsolation() {
        if(!verifiedRebuildIsolated)
            return;

        try {
            rebuildTransmissionNetwork(verifiedRebuildLinesBefore);
        } catch(RuntimeException exception) {
            PowerGrid.LOGGER.error(
                    "Failed to restore transmission topology after an aborted verified rebuild in {}; leaving transmission lines disconnected",
                    world.dimension().location(),
                    exception
            );
            try {
                detachDerivedTransmissionTopology();
            } catch(RuntimeException detachException) {
                exception.addSuppressed(detachException);
            }
        } finally {
            verifiedRebuildIsolated = false;
            verifiedRebuildLinesBefore = 0;
            scheduleDiscoveryForAllNetworks();
        }
    }

    private void scheduleDiscoveryForAllNetworks() {
        islandDiscoveryQueue.clear();
        islandDiscoveryQueue.addAll(subnetworks);
    }

    private void scheduleRebuildRetry(TransmissionLinePart part) {
        rebuildRetryQueue.put(
                part.persistentOwnerId,
                new RebuildRetry(part, world.getGameTime() + REBUILD_RETRY_INTERVAL_TICKS)
        );
    }

    private void processRebuildRetries() {
        if(rebuildRetryQueue.isEmpty())
            return;

        var currentTick = world.getGameTime();
        int attemptsThisTick = 0;
        var iterator = rebuildRetryQueue.entrySet().iterator();
        while(iterator.hasNext() && attemptsThisTick < MAX_REBUILD_RETRIES_PER_TICK) {
            var entry = iterator.next();
            var retry = entry.getValue();
            var part = retry.part;

            if(lineParts.get(entry.getKey()) != part) {
                iterator.remove();
                continue;
            }
            if(part.getLine() != null) {
                PowerGrid.LOGGER.info(
                        "Transmission part {} resolved before its queued retry in {}",
                        part.persistentOwnerId,
                        world.dimension().location()
                );
                iterator.remove();
                continue;
            }
            if(currentTick < retry.nextAttemptTick)
                continue;
            if(!retry.budget.tryAcquire()) {
                iterator.remove();
                continue;
            }

            ++attemptsThisTick;
            restorePartEndpointNodes(part);
            try {
                if(makeTransmissionLine(part)) {
                    PowerGrid.LOGGER.info(
                            "Transmission part {} rebuilt automatically on retry {}/{} in {}",
                            part.persistentOwnerId,
                            retry.budget.attempts(),
                            MAX_AUTOMATIC_REBUILD_RETRIES,
                            world.dimension().location()
                    );
                    iterator.remove();
                    continue;
                }
                logRebuildFailure(part, retry.budget.attempts(), retry.budget.exhausted(), null);
            } catch(RuntimeException exception) {
                logRebuildFailure(part, retry.budget.attempts(), retry.budget.exhausted(), exception);
            }

            if(retry.budget.exhausted()) {
                iterator.remove();
            } else {
                retry.nextAttemptTick = currentTick + REBUILD_RETRY_INTERVAL_TICKS;
            }
        }
    }

    private void restorePartEndpointNodes(TransmissionLinePart part) {
        var endpoint1 = part.getConnectionEndpoint1();
        var endpoint2 = part.getConnectionEndpoint2();
        var node1 = holderOrPlaceholderNode(endpoint1);
        var node2 = holderOrPlaceholderNode(endpoint2);

        movePartMap(part.getNode1(), node1, part);
        part.setNode1(node1);
        movePartMap(part.getNode2(), node2, part);
        part.setNode2(node2);
        part.refreshEndpointNodes();
    }

    private void logRebuildFailure(TransmissionLinePart part, int retryAttempt, boolean exhausted, @Nullable RuntimeException exception) {
        var endpoint1 = part.getConnectionEndpoint1();
        var endpoint2 = part.getConnectionEndpoint2();
        var node1 = part.getNode1();
        var node2 = part.getNode2();
        String reason;
        if(endpoint1.equals(endpoint2)) {
            reason = "identical endpoints";
        } else if(node1 == node2) {
            reason = "both endpoints resolve to the same node";
        } else {
            reason = "connection preparation returned no network";
        }

        var attempt = retryAttempt == 0
                ? "initial rebuild"
                : "automatic retry " + retryAttempt + "/" + MAX_AUTOMATIC_REBUILD_RETRIES;
        var state = exhausted ? "retry limit exhausted" : "retry pending";
        if(exception == null) {
            PowerGrid.LOGGER.warn(
                    "Failed to rebuild transmission part {} during {} in {} ({}; {}; endpoint1={}, endpoint2={}, node1={}, node2={}, lastKnownChunk={}, ownerLoaded={})",
                    part.persistentOwnerId,
                    attempt,
                    world.dimension().location(),
                    reason,
                    state,
                    endpoint1,
                    endpoint2,
                    node1,
                    node2,
                    part.lastKnownChunk,
                    part.owner != null && !part.owner.isRemoved()
            );
        } else {
            PowerGrid.LOGGER.error(
                    "Exception while rebuilding transmission part {} during {} in {} ({}; {}; endpoint1={}, endpoint2={}, node1={}, node2={}, lastKnownChunk={}, ownerLoaded={})",
                    part.persistentOwnerId,
                    attempt,
                    world.dimension().location(),
                    reason,
                    state,
                    endpoint1,
                    endpoint2,
                    node1,
                    node2,
                    part.lastKnownChunk,
                    part.owner != null && !part.owner.isRemoved(),
                    exception
            );
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var partList = new ListTag();
        for(var part : lineParts.values()) {
            partList.add(part.toNbt());
        }
        tag.put("Parts", partList);
        return tag;
    }

    protected void readNbt(CompoundTag nbt) {
        var partList = nbt.getList("Parts", Tag.TAG_COMPOUND);
        for(var entryGeneric : partList) {
            var partEntry = (CompoundTag) entryGeneric;
            TransmissionLinePart.uniquePart(partEntry, this);
        }
    }

    public void nodeHolderUnloaded(@NotNull OwnedFloatingNode ownedNode) {
        var affectedNetwork = ownedNode.getNetwork();
        // An ordinary chunk unload must not tear down a healthy derived line.
        // The verified rebuild deliberately assembles the complete passive
        // topology from persisted parts, including unloaded chunks. Preserve
        // that topology and replace only the departing block-owned node.
        //
        // TransmissionLine#setNodeN rewires the existing line in both the
        // solver network and the global graph, while addAndMigrateNode also
        // moves every physical-part index entry and endpoint alias.
        addAndMigrateNode(ownedNode, new OwnedFloatingNode(ownedNode.endpoint));
        scheduleIslandDiscovery(affectedNetwork);
        setDirty();
    }

    public void nodeHolderRemoved(@NotNull OwnedFloatingNode ownedNode) {
        // Connections should already be broken by endpoint removal stuff.
        var parts = partNodeMap.remove(ownedNode);
        if(parts != null && !parts.isEmpty()) {
            // But just in case, remove any part that still exists.
            for(var part : parts) {
                if(part.owner != null) {
                    part.owner.kill();
                } else {
                    part.remove();
                }
            }
        }
        scheduleIslandDiscovery(ownedNode.getNetwork());
        ownedNode.remove();
        globalExternalNodes.entrySet().removeIf(entry -> entry.getValue() == ownedNode);
    }

    private boolean traceTree(OwnedFloatingNode endpointNode, Set<OwnedFloatingNode> visited) {
        if(!visited.add(endpointNode))
            return false;
        Collection<TransmissionLinePart> parts = partNodeMap.get(endpointNode);
        if(parts == null)
            return false;
        parts = List.copyOf(parts);
        boolean continueResolving = false;
        for(var part : parts) {
            var side = transmissionPartSide(
                    part.getNode1(),
                    part.getNode2(),
                    endpointNode
            );
            if(part.getLine() != null) {
                // This branch connects to a valid line, back-trace and resolve all line parts.
                continueResolving = true;
            }
            if(parts.size() == 1) {
                if(ModdedConfigs.logsEnabled())
                    PowerGrid.LOGGER.debug("Found edge line at {}", endpointNode);
                if(side == TransmissionPartSide.FIRST) {
                    // Check endpoint2
                    if(part.getConnectionEndpoint2().isValid(world)) {
                        // Resolve segment
                        makeTransmissionLine(part);
                        return true;
                    }
                } else if(side == TransmissionPartSide.SECOND) {
                    // Check endpoint1
                    if(part.getConnectionEndpoint1().isValid(world)) {
                        // Resolve segment
                        makeTransmissionLine(part);
                        return true;
                    }
                } else {
                    PowerGrid.LOGGER.warn(
                            "Transmission part {} was indexed under a node it does not contain: indexedNode={}, node1={}, node2={}",
                            part.persistentOwnerId,
                            endpointNode,
                            part.getNode1(),
                            part.getNode2()
                    );
                }
                break;
            }
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("Continuing line trace through {}", endpointNode);
            if(side == TransmissionPartSide.FIRST) {
                if(traceTree(part.getNode2(), visited)) {
                    makeTransmissionLine(part);
                    continueResolving = true;
                }
            } else if(side == TransmissionPartSide.SECOND) {
                if(traceTree(part.getNode1(), visited)) {
                    makeTransmissionLine(part);
                    continueResolving = true;
                }
            } else {
                PowerGrid.LOGGER.warn(
                        "Transmission part {} was indexed under a node it does not contain while tracing: indexedNode={}, node1={}, node2={}",
                        part.persistentOwnerId,
                        endpointNode,
                        part.getNode1(),
                        part.getNode2()
                );
            }
        }
        return continueResolving;
    }

    static TransmissionPartSide transmissionPartSide(
            @NotNull OwnedFloatingNode node1,
            @NotNull OwnedFloatingNode node2,
            @NotNull OwnedFloatingNode indexedNode
    ) {
        if(node1 == indexedNode)
            return TransmissionPartSide.FIRST;
        if(node2 == indexedNode)
            return TransmissionPartSide.SECOND;
        return TransmissionPartSide.NONE;
    }

    private void resolveTree(@NotNull IWireEndpoint endpoint) {
        if(!transmissionTopologyResolutionAllowed())
            return;
        var endpointNode = holderOrPlaceholderNode(endpoint);
        var unresolvedLines = partNodeMap.get(endpointNode);
        if(unresolvedLines == null)
            return;
        // We need to trace the graph to all terminating nodes and see if any are loaded,
        // if so, we need to resolve all lines between them to ensure correct unloaded chunk behaviour.
        var visited = Collections.newSetFromMap(
                new IdentityHashMap<OwnedFloatingNode, Boolean>()
        );
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug(
                    "Starting line trace at connection endpoint {} resolved as {}",
                    endpoint,
                    endpointNode
            );
        traceTree(endpointNode, visited);
    }

    public void addAndMigrateNode(IWireEndpoint endpoint) {
        var newNode = endpoint.getNode(world);
        if(newNode == null)
            return;
        addAndMigrateNode(endpoint, newNode, NodeBindingMode.BIND_ONLY);
    }

    public void addAndMigrateNode(OwnedFloatingNode newNode) {
        bindEndpointNode(newNode.endpoint, newNode);
    }

    public void addAndMigrateNode(IWireEndpoint endpointAlias, OwnedFloatingNode newNode) {
        addAndMigrateNode(endpointAlias, newNode, NodeBindingMode.BIND_AND_SPLIT);
    }

    private void addAndMigrateNode(
            IWireEndpoint endpointAlias,
            OwnedFloatingNode newNode,
            NodeBindingMode mode
    ) {
        if(newNode == null)
            return;
        bindCanonicalAndAlias(endpointAlias, newNode);

        if(shouldSplitAfterNodeBinding(mode, transmissionTopologyResolutionAllowed())) {
            var line = findLineMiddle(newNode);
            if(line != null)
                line.splitAt(newNode);
        }
    }

    static boolean shouldSplitAfterNodeBinding(
            NodeBindingMode mode,
            boolean topologyResolutionAllowed
    ) {
        return mode == NodeBindingMode.BIND_AND_SPLIT
                && topologyResolutionAllowed;
    }

    private void bindCanonicalAndAlias(
            @NotNull IWireEndpoint endpointAlias,
            @NotNull OwnedFloatingNode node
    ) {
        bindEndpointNode(node.endpoint, node);
        if(!node.endpoint.equals(endpointAlias))
            bindEndpointNode(endpointAlias, node);
    }

    private void bindEndpointNode(
            @NotNull IWireEndpoint endpoint,
            @NotNull OwnedFloatingNode node
    ) {
        var previousNode = globalExternalNodes.put(endpoint, node);
        addAndMigrateNode(previousNode, node);
    }

    public void removeEndpointAlias(
            @NotNull IWireEndpoint endpointAlias,
            @NotNull OwnedFloatingNode expectedNode
    ) {
        globalExternalNodes.remove(endpointAlias, expectedNode);
    }

    public void addAndMigrateNode(OwnedFloatingNode oldNode, OwnedFloatingNode newNode) {
        if(newNode == null)
            return;
        var endpoint = newNode.endpoint;
        if(oldNode != null && oldNode != newNode) {
            // Migrate connections into the new node.
            // This happens when a block entity is loaded but its terminal was acting as a transmission line junction.
            if(ModdedConfigs.logsEnabled())
                PowerGrid.LOGGER.debug("Migrating external node from {} to {}", oldNode, newNode);
            rebindNodeAliasesByIdentity(globalExternalNodes, oldNode, newNode);
            var parts = moveNodeIndex(partNodeMap, oldNode, newNode);
            if(!parts.isEmpty()) {
                for(TransmissionLinePart part : parts) {
                    if(ModdedConfigs.logsEnabled())
                        PowerGrid.LOGGER.debug("Migrating node for part {}", part);
                    if(part.getEndpoint1().equals(endpoint) || part.getNode1() == oldNode) {
                        part.setNode1(newNode);
                        if(ModdedConfigs.logsEnabled())
                            PowerGrid.LOGGER.debug("Part {} has had its node migrated", part);
                        var line = part.getLine();
                        if(line != null) {
                            if(line.getNode1() == oldNode) {
                                inNetwork(line.getNetwork(), newNode);
                                line.setNode1(newNode);
                                if(ModdedConfigs.logsEnabled())
                                    PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                            }
                        }
                    }
                    if(part.getEndpoint2().equals(endpoint) || part.getNode2() == oldNode) {
                        part.setNode2(newNode);
                        if(ModdedConfigs.logsEnabled())
                            PowerGrid.LOGGER.debug("Part {} has had its node migrated", part);
                        var line = part.getLine();
                        if(line != null) {
                            if(line.getNode2() == oldNode) {
                                inNetwork(line.getNetwork(), newNode);
                                line.setNode2(newNode);
                                if(ModdedConfigs.logsEnabled())
                                    PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                            }
                        }
                    }
                }
            }
            if(oldNode.getNetwork() != null) {
                inNetwork(oldNode.getNetwork(), newNode);
                var unified = oldNode.getNetwork();
                var lines = List.copyOf(globalGraph.getConnectedLines(oldNode));
                for (var line : lines) {
                    if (line.getNode1() == oldNode) {
                        line.setNode1(newNode);
                        if(ModdedConfigs.logsEnabled())
                            PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                    } else if (line.getNode2() == oldNode) {
                        line.setNode2(newNode);
                        if(ModdedConfigs.logsEnabled())
                            PowerGrid.LOGGER.debug("Line {} has had its node migrated", line);
                    } else {
                        PowerGrid.LOGGER.warn("Line connected to old node in graph, but doesn't have it as an endpoint?");
                    }
                }
                unified.removeNode(oldNode);
            } else {
                // TODO: This is also redundant, segments already have their nodes updated.
                var line = findLineMiddle(oldNode);
                if(line != null) {
                    for (var segment : line.segments) {
                        if (segment.getNode1() == oldNode || segment.getEndpoint1().equals(endpoint)) {
                            movePartMap(segment.getNode1(), newNode, segment);
                            segment.setNode1(newNode);
                            if(ModdedConfigs.logsEnabled())
                                PowerGrid.LOGGER.debug("Line {} has had its internal node migrated", line);
                        } else if (segment.getNode2() == oldNode || segment.getEndpoint2().equals(endpoint)) {
                            movePartMap(segment.getNode2(), newNode, segment);
                            segment.setNode2(newNode);
                            if(ModdedConfigs.logsEnabled())
                                PowerGrid.LOGGER.debug("Line {} has had its internal node migrated", line);
                        }
                    }
                }
            }
        }
    }

    static void rebindNodeAliasesByIdentity(
            Map<IWireEndpoint, OwnedFloatingNode> endpointBindings,
            OwnedFloatingNode oldNode,
            OwnedFloatingNode newNode
    ) {
        endpointBindings.replaceAll((endpoint, node) -> node == oldNode ? newNode : node);
        endpointBindings.put(oldNode.endpoint, newNode);
    }

    static <T> List<T> moveNodeIndex(
            Map<OwnedFloatingNode, Set<T>> nodeIndex,
            OwnedFloatingNode oldNode,
            OwnedFloatingNode newNode
    ) {
        var moved = nodeIndex.remove(oldNode);
        if(moved == null || moved.isEmpty())
            return List.of();
        nodeIndex.computeIfAbsent(newNode, $ -> new HashSet<>()).addAll(moved);
        return List.copyOf(moved);
    }

    public void nodeHolderAdded(@NotNull OwnedFloatingNode ownedNode, boolean hasInternals) {
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("Node holder added, {}", ownedNode);
        addAndMigrateNode(ownedNode.endpoint);
        if(!transmissionTopologyResolutionAllowed())
            return;
        // Try to resolve an end of a transmission line
        resolveTree(ownedNode.endpoint);
        if(hasInternals) {
            var line = findLineMiddle(ownedNode);
            if(line != null) {
                line.splitAt(ownedNode);
            }
        }
    }

    // TODO: When pausing we can simplify transmission lines.
    public void prepareUnpaused(OwnedFloatingNode node) {
        if(ModdedConfigs.logsEnabled())
            PowerGrid.LOGGER.debug("Preparing node for unpaused internal connections");
        if(!transmissionTopologyResolutionAllowed())
            return;
        var line = findLineMiddle(node);
        if(line != null)
            line.splitAt(node);
    }

    private boolean transmissionTopologyResolutionAllowed() {
        return transmissionTopologyResolutionAllowed(
                verifiedRebuildIsolated,
                assemblingTransmissionTopology
        );
    }

    static boolean transmissionTopologyResolutionAllowed(
            boolean verifiedRebuildIsolated,
            boolean assemblingTransmissionTopology
    ) {
        return !verifiedRebuildIsolated || assemblingTransmissionTopology;
    }

    /**
     * Save an entity to be recovered into the transmission line once its chunk is loaded back into the world.
     *
     * @param entityId       Transmission line part entity id
     * @param lastKnownChunk Last known chunk of the entity
     */
    public void bounty(PartId entityId, ChunkPos lastKnownChunk) {
        if(world.hasChunk(lastKnownChunk.x, lastKnownChunk.z)) {
            checkForExistence.computeIfAbsent(lastKnownChunk, $ -> new CheckChunk()).add(entityId);
            return;
        }
        expectedInChunks.computeIfAbsent(lastKnownChunk, $ -> new CheckChunk()).add(entityId);
    }

    public void unloadPartsOwnedBy(BaseWireEntity owner) {
        for(var part : lineParts.values()) {
            if(part.owner == owner)
                part.unload();
        }
    }

    public void tracking(ServerPlayer tracker, IWireEndpoint endpoint, boolean end) {
        if(!end) {
            trackers.computeIfAbsent(endpoint, $ -> new HashSet<>()).add(tracker);
//            var lines = globalGraph.getConnectedLines(endpoint.getNode(world));
//            ModdedPackets.sendToClient(new TransmissionLineManagementS2CPacket(endpoint, lines), tracker);
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
        var indexedNode = globalExternalNodes.get(endpoint);

        OwnedFloatingNode loadedNode = null;
        if(endpoint.isValid(world)) {
            loadedNode = endpoint.getNode(world);
        }

        var selectedNode = selectLoadedOrIndexedNode(endpoint, indexedNode, loadedNode);
        bindCanonicalAndAlias(endpoint, selectedNode);
        return selectedNode;
    }

    static OwnedFloatingNode selectLoadedOrIndexedNode(
            @NotNull IWireEndpoint endpoint,
            @Nullable OwnedFloatingNode indexedNode,
            @Nullable OwnedFloatingNode loadedNode
    ) {
        // A proxy endpoint may legitimately resolve to a node owned by the main
        // block of a multiblock, so the node's canonical endpoint can differ
        // from the endpoint used as the index key.
        if(loadedNode != null)
            return loadedNode;
        if(indexedNode != null)
            return indexedNode;
        return new OwnedFloatingNode(endpoint);
    }

    public void registerPart(PartId persistentOwnerId, TransmissionLinePart part) {
        lineParts.put(persistentOwnerId, part);
        partNodeMap.computeIfAbsent(part.getNode1(), $ -> new HashSet<>()).add(part);
        partNodeMap.computeIfAbsent(part.getNode2(), $ -> new HashSet<>()).add(part);
        setDirty();
    }

    public void unregisterPart(PartId persistentOwnerId, TransmissionLinePart part) {
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
    public TransmissionLinePart getPart(PartId persistentOwnerId) {
        return lineParts.get(persistentOwnerId);
    }

    Map<PartId, TransmissionLinePart> linePartsSnapshot() {
        return Map.copyOf(lineParts);
    }

    boolean removeStalePartAfterVerifiedScan(PartId id) {
        var part = lineParts.get(id);
        if(part == null || part.owner != null)
            return false;
        PowerGrid.LOGGER.warn(
                "Removing stale transmission registration {} after its owner and endpoint chunks were entity-ticking and no physical owner was found",
                id
        );
        part.remove();
        return true;
    }

    void removePartForVerifiedRefresh(PartId id, BaseWireEntity physicalOwner) {
        if(id.getEntity((ServerLevel) world) != physicalOwner)
            throw new IllegalArgumentException("Physical owner does not match the transmission part id");
        var part = lineParts.get(id);
        if(part != null)
            part.remove();
    }

    public void inNetwork(@Nullable ElectricalNetwork network, @NotNull OwnedFloatingNode node) {
        inNetwork(network, node, node.endpoint);
    }

    public void inNetwork(
            @Nullable ElectricalNetwork network,
            @NotNull OwnedFloatingNode node,
            @NotNull IWireEndpoint connectionEndpoint
    ) {
        if(network == null)
            return;
        var currentNetwork = node.getNetwork();
        if(currentNetwork != null && !currentNetwork.ownsNode(node))
            node.setNetwork(null);
        if(node.getNetwork() == null)
            connectionEndpoint.joinNetwork(world, network);
        addOrMergeExactNode(network, node);
    }

    static void addOrMergeExactNode(
            @NotNull ElectricalNetwork network,
            @NotNull OwnedFloatingNode node
    ) {
        var currentNetwork = node.getNetwork();
        if(currentNetwork == network && network.ownsNode(node))
            return;
        if(currentNetwork != null && currentNetwork.ownsNode(node)) {
            network.merge(currentNetwork);
            return;
        }
        if(currentNetwork != null)
            node.setNetwork(null);
        network.addNode(node);
    }

    public void movePartMap(OwnedFloatingNode oldNode, OwnedFloatingNode newNode, TransmissionLinePart part) {
        if(newNode == null)
            return;
        if(oldNode != null && oldNode != newNode) {
            var parts = partNodeMap.get(oldNode);
            if(parts != null) {
                parts.remove(part);
                if(parts.isEmpty())
                    partNodeMap.remove(oldNode);
            }
        }
        // Re-add even if the old index entry was already missing. This keeps the
        // node-to-part index self-healing during entity reloads.
        partNodeMap.computeIfAbsent(newNode, $ -> new HashSet<>()).add(part);
    }

    public void removeFromNetwork(OwnedFloatingNode node) {
        var checked = new HashSet<IElectricNode>();
        var toCheck = new ArrayList<IElectricNode>();
        toCheck.add(node);
        while(!toCheck.isEmpty()) {
            var check = toCheck.remove(0);
            if(!checked.add(check))
                continue;
            var nodes = globalGraph.getConnectedNodes(check);
            var couplings = globalGraph.getCouplings(node);
            couplings.forEach(INode::remove);
            for(var connected : nodes) {
                var wires = List.copyOf(globalGraph.getWires(node, connected));
                for(var wire : wires)
                    wire.remove();
                toCheck.add(connected);
            }
            node.remove();
        }
    }

    static class CheckChunk {
        public final Set<PartId> entities = Sets.newConcurrentHashSet();
        private int ticks = 0;

        public void add(PartId id) {
            entities.add(id);
            resetTicks();
        }

        public void addAll(Set<PartId> ids) {
            entities.addAll(ids);
            resetTicks();
        }

        public void resetTicks() {
            ticks = 0;
        }

        public boolean advanceEntityLoadCheck() {
            return ticks++ >= ENTITY_LOAD_GRACE_TICKS;
        }
    }

    public interface PartId {
        BaseWireEntity getEntity(ServerLevel level);
    }

    public record SimpleId(UUID id) implements PartId {
        @Override
        public BaseWireEntity getEntity(ServerLevel level) {
            var entity = level.getEntity(id);
            if(entity instanceof BaseWireEntity wire)
                return wire;
            return null;
        }
    }

    public record ComplexId(UUID id, int sub) implements PartId {
        @Override
        public BaseWireEntity getEntity(ServerLevel level) {
            var entity = level.getEntity(id);
            if(entity instanceof BaseWireEntity wire)
                return wire;
            return null;
        }
    }

    public record CouplingKey(Island island1, Island island2) {
        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;
            if(obj instanceof CouplingKey other) {
                return (island1 == other.island1 && island2 == other.island2) ||
                        (island1 == other.island2 && island2 == other.island1);
            }
            return false;
        }

        @Override
        public int hashCode() {
            // Symmetric hash code
            return Objects.hashCode(island1) + Objects.hashCode(island2);
        }

        public boolean has(Island island) {
            return island == island1 || island == island2;
        }

        public static CouplingKey of(Island island1, Island island2) {
            return new CouplingKey(island1, island2);
        }

        public Island other(Island island) {
            if(island == island1)
                return island2;
            if(island == island2)
                return island1;
            return null;
        }
    }

    public static class Island {
        private final Set<INetworkElement> elements = new HashSet<>();
        private final Map<CouplingKey, Set<TransmissionLine>> couplings;

        public Island(Map<CouplingKey, Set<TransmissionLine>> couplings) {
            this.couplings = couplings;
        }

        public void addAll(Island island) {
            elements.addAll(island.elements);
            var merged = couplings.remove(CouplingKey.of(this, island));
            if(merged != null) {
                elements.addAll(merged);
            }

            var newlyAdded = new HashMap<CouplingKey, Set<TransmissionLine>>();
            couplings.entrySet().removeIf(entry -> {
                if(entry.getKey().has(island)) {
                    var key = CouplingKey.of(this, entry.getKey().other(island));
                    if(couplings.containsKey(key))  {
                        couplings.get(key).addAll(entry.getValue());
                    } else {
                        // Avoid concurrent modification
                        newlyAdded.put(key, entry.getValue());
                    }
                    return true;
                }
                return false;
            });
            couplings.putAll(newlyAdded);
        }

        public void add(INetworkElement element) {
            elements.add(element);
        }

        public boolean contains(INetworkElement element) {
            return elements.contains(element);
        }

        public void addCoupling(Island connectedIsland, TransmissionLine line) {
            couplings.computeIfAbsent(CouplingKey.of(this, connectedIsland), $ -> new HashSet<>())
                    .add(line);
        }
    }
}
