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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.*;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.network.packets.StateS2CPacket;

import java.util.*;
import java.util.function.Function;

public class ElectricBehaviour extends BlockEntityBehaviour implements ISynchronizedElement {
    public static final BehaviourType<ElectricBehaviour> TYPE = new BehaviourType<>();

    private final IElectricEntity element;

    // Order of these lists should be the same on server and client.
    private final List<INode> internalNodes = new ArrayList<>();
    private final List<OwnedFloatingNode> externalNodes = new ArrayList<>();
    private final List<AbstractElectricWire> internalWires = new ArrayList<>();

    private final Map<BlockWireEndpoint, Set<BaseWireEntity>> connections = new HashMap<>();
    private boolean destroying = false;
    private byte rebuildOnClient = 0;
    private boolean removed = false;
    private boolean paused = true;
    private boolean reducedSync = false;

    private SyncAppender syncAppender;

    public <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be) {
        this(be, true);
    }

    protected <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be, boolean buildCircuit) {
        super(be);
        this.element = be;
        if(buildCircuit) {
            var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
            element.buildCircuit(builder);
        }
    }

    public void reducedSync() {
        reducedSync = true;
    }

    public void joinNetwork(@NotNull ElectricalNetwork network, int externalIndex) {
        if(externalIndex < 0 || externalIndex >= externalNodes.size())
            return;
        var node = externalNodes.get(externalIndex);
        tracedAdd(network, node);
    }

    public void tracedAdd(ElectricalNetwork network, IElectricNode node) {
        addOrMerge(node, network);
        var list = new LinkedList<INode>();
        list.add(node);
        tracedAdd(list);
    }

    public void rebuildCircuit(boolean rebuildExternal) {
        var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
        var networkList = externalNodes.stream().map(INode::getNetwork).toList();
        if(rebuildExternal) {
            for (var endpointConnections : connections.values()) {
                for (var entity : endpointConnections) {
                    entity.dropWire();
                }
            }
        }
        builder.rebuildExternal(rebuildExternal);
        if(paused)
            builder.paused();
        builder.clear();
        element.buildCircuit(builder);
        // Make sure external (and internal) nodes are in the correct networks.
        for(int i = 0; i < networkList.size(); ++i) {
            if(networkList.get(i) == null)
                continue;
            joinNetwork(networkList.get(i), i);
        }

        if(rebuildExternal) {
            // Break connections if external node was removed.
            var iter = connections.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                var endpoint = entry.getKey();
                if (endpoint.getTerminal() < externalNodes.size() && externalNodes.get(endpoint.getTerminal()) != null) {
                    // Rewire
                    for (var entity : entry.getValue())
                        entity.makeWire();
                    continue;
                }
                var connCopy = List.copyOf(entry.getValue());
                for (BaseWireEntity entity : connCopy) {
                    entity.endpointRemoved(endpoint);
                }
                iter.remove();
            }
            if(getWorld() != null)
                GlobalElectricNetworks.nodeHolderAdded(this);
        }

        var world = getWorld();
        if(world != null && !world.isClientSide)
            rebuildOnClient = rebuildExternal ? (byte) 2 : (byte) 1;
    }

    public List<INode> getInternalNodes() {
        return internalNodes;
    }

    public List<OwnedFloatingNode> getExternalNodes() {
        return externalNodes;
    }

    public void pause() {
        if(!paused) {
            paused = true;
            element.paused();
            internalWires.forEach(AbstractElectricWire::remove);
            // Remove nodes in reverse order.
            for(int i = internalNodes.size() - 1; i >= 0; --i) {
                var node = internalNodes.get(i);
                node.remove();
            }
        }
    }

    private static void addOrMerge(IElectricNode node, ElectricalNetwork network) {
        if(node.getNetwork() == network)
            return;
        if(node.getNetwork() == null) {
            network.addNode(node);
            return;
        }
        network.merge(node.getNetwork());
    }

    public void tracedAdd(List<INode> scanNodes) {
        if(paused)
            return;
        var handled = new HashSet<INetworkElement>();
        while(!scanNodes.isEmpty()) {
            var node = scanNodes.remove(0);
            var network = node.getNetwork();
            if(network == null)
                continue;
            handled.add(node);
            for(var wire : internalWires) {
                if(wire.coupledNodes().contains(node)) {
                    // Wire connects here
                    if(handled.add(wire)) {
                        wire.coupledNodes().forEach(other -> {
                            addOrMerge(other, network);
                            if(handled.add(other))
                                scanNodes.add(other);
                        });
                        network.addWire(wire);
                    }
                }
            }
            for(var inode : internalNodes) {
                if(!(inode instanceof ICouplingNode coupling))
                    continue;
                if(coupling.coupledNodes().contains(node)) {
                    if(handled.add(coupling)) {
                        coupling.coupledNodes().forEach(other -> {
                            addOrMerge(other, network);
                            if(handled.add(other))
                                scanNodes.add(other);
                        });
                        network.addNode(coupling);
                    }
                }
            }
        }
    }

    public void unpause() {
        if(paused) {
            paused = false;
            // External nodes weren't removed so they don't have to be added.
            if(hasInternals()) {
                GlobalElectricNetworks.prepareUnpaused(this);
                tracedAdd(new LinkedList<>(externalNodes));
                element.unpaused();
            }
        }
    }

    @Override
    public void unload() {
        if(!removed) {
            pause();
            // Unload doesn't remove external nodes since they might be utilized by transmission lines.
            // Wires are not dropped either since they could be forming an important transmission line junction.
            GlobalElectricNetworks.nodeHolderUnloaded(this);
        }
    }

    public void remove() {
        breakConnections();
        pause();
        GlobalElectricNetworks.nodeHolderRemoved(this);
        removed = true;
    }

    public void refreshConnectionEntities() {
        for(var entry : connections.entrySet()) {
            for(var entity : entry.getValue()) {
                if(entity instanceof HangingWireEntity wire)
                    wire.refreshTerminalPositions();
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        GlobalElectricNetworks.nodeHolderAdded(this);
        unpause();
    }

    public void addConnection(BlockWireEndpoint endpoint, BaseWireEntity wire) {
        var sourceConnections = connections.computeIfAbsent(endpoint, key -> new HashSet<>());
        // Check for stale wires here
        sourceConnections.removeIf(Entity::isRemoved);
        sourceConnections.add(wire);
    }

    public void removeConnection(BlockWireEndpoint endpoint, BaseWireEntity wire) {
        if(!destroying && connections.containsKey(endpoint)) {
            var list = connections.get(endpoint);
            list.remove(wire);
            if(list.isEmpty())
                connections.remove(endpoint);
        }
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Nullable
    public OwnedFloatingNode getTerminal(int index) {
        if(index >= externalNodes.size())
            return null;
        return externalNodes.get(index);
    }

    public boolean hasConnection(BlockWireEndpoint source, BlockWireEndpoint destination) {
        if(!connections.containsKey(source))
            return false;
        var sourceConnections = connections.get(source);
        for(var entity : sourceConnections) {
            if(entity.isConnectedTo(destination.getPos(), destination.getTerminal()))
                return true;
        }
        return false;
    }

    public Map<BlockWireEndpoint, Set<BaseWireEntity>> getConnections() {
        return connections;
    }

    public boolean hasTerminal(int terminal) {
        return terminal >= 0 && terminal < externalNodes.size() && externalNodes.get(terminal) != null;
    }

    public void breakConnections() {
        if(destroying)
            return;
        destroying = true;
        var world = getWorld();
        if(!world.isClientSide) {
            for(var entry : connections.entrySet()) {
                var endpoint = entry.getKey();
                for(var entity : entry.getValue()) {
                    entity.endpointRemoved(endpoint);
                }
            }
            connections.clear();
        }
        destroying = false;
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if(clientPacket) {
            var level = nbt.getByte("Rebuild");
            if(level > 0)
                rebuildCircuit(level > 1);
            var list = nbt.getList("Nodes", Tag.TAG_FLOAT);
            int index = 0;
            for(var node : externalNodes) {
                node.setStateValue(list.getFloat(index++) * 0.5f + node.getStateValue() * 0.5f);
            }
            for(var node : internalNodes) {
                node.setStateValue(list.getFloat(index++) * 0.5f + node.getStateValue() * 0.5f);
            }
        }
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(clientPacket) {
            if(rebuildOnClient != 0) {
                nbt.putByte("Rebuild", rebuildOnClient);
                rebuildOnClient = 0;
            }
            var list = new ListTag();
            for(var node : externalNodes) {
                list.add(FloatTag.valueOf((float) node.getStateValue()));
            }
            for(var node : internalNodes) {
                list.add(FloatTag.valueOf((float) node.getStateValue()));
            }
            nbt.put("Nodes", list);
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if(paused)
            unpause();
    }

    public void inheritConnections(ElectricBehaviour otherBehaviour) {
        for(var entry : otherBehaviour.connections.entrySet()) {
            var thisList = connections.computeIfAbsent(entry.getKey(), key -> new HashSet<>());
            thisList.addAll(entry.getValue());
        }
        otherBehaviour.connections.clear();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean hasInternals() {
        return !internalNodes.isEmpty() || !internalWires.isEmpty();
    }

    public static void handleTicketChange(int newLevel, @NotNull ChunkHolder holder, int oldLevel) {
        var chunk = holder.getTickingChunk();
        if(chunk == null)
            return;
        if(!ChunkLevel.isBlockTicking(newLevel) && ChunkLevel.isBlockTicking(oldLevel)) {
            // Block entities no longer ticking.
            // Above level 33 the entities get completely unloaded so no need to pause them.
            for(var be : chunk.getBlockEntities().values()) {
                if(be instanceof SmartBlockEntity smart) {
                    var electric = smart.getBehaviour(ElectricBehaviour.TYPE);
                    if(electric == null)
                        continue;
                    electric.pause();
                }
            }
        } else if(ChunkLevel.isBlockTicking(newLevel)) {
            // Block entities ticking again.
            for(var be : chunk.getBlockEntities().values()) {
                if(be instanceof SmartBlockEntity smart) {
                    var electric = smart.getBehaviour(ElectricBehaviour.TYPE);
                    if(electric == null)
                        continue;
                    electric.unpause();
                }
            }
        }
    }

    @Override
    public void writeToSync(FriendlyByteBuf buffer, Function<OwnedFloatingNode, TransmissionLine> lineGetter) {
        var thermal = blockEntity.getBehaviour(ThermalBehaviour.TYPE);
        if(thermal != null) {
            buffer.writeFloat(thermal.getTemperature());
        }
            for (var node : externalNodes) {
                if (node.getNetwork() == null) {
                    // Potentially part of a transmission line.
                    var line = lineGetter.apply(node);
                    buffer.writeFloat(line == null ? 0 : line.voltageFor(node));
                } else {
                    buffer.writeFloat((float) node.getStateValue());
                }
            }
        if(!reducedSync) {
            for (var node : internalNodes) {
                buffer.writeFloat((float) node.getStateValue());
            }
            for (var wire : internalWires) {
                if (wire instanceof SwitchedWire switched)
                    buffer.writeBoolean(switched.getState());
            }
        }
        if(syncAppender != null)
            syncAppender.writeToSync(buffer);
    }

    @Override
    public void readFromSync(FriendlyByteBuf buffer) {
        var thermal = blockEntity.getBehaviour(ThermalBehaviour.TYPE);
        if(thermal != null) {
            thermal.setTemperature(buffer.readFloat());
        }
        for (var node : externalNodes) {
            node.setStateValue(buffer.readFloat());
        }
        if(!reducedSync) {
            for (var node : internalNodes) {
                node.setStateValue(buffer.readFloat());
            }
            for (var wire : internalWires) {
                if (wire instanceof SwitchedWire switched)
                    switched.setState(buffer.readBoolean());
            }
        }
        if(syncAppender != null)
            syncAppender.readFromSync(buffer);
    }

    @Override
    public StateS2CPacket.Key getKey() {
        return new StateS2CPacket.PosKey(getPos());
    }

    public void setSyncAppender(SyncAppender syncAppender) {
        this.syncAppender = syncAppender;
    }

    public interface SyncAppender {
        void writeToSync(FriendlyByteBuf buffer);
        void readFromSync(FriendlyByteBuf buffer);
    }
}
