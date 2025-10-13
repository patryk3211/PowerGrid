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
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;

import java.util.*;

public class ElectricBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ElectricBehaviour> TYPE = new BehaviourType<>();

    private final IElectricEntity element;

    // Order of these lists should be the same on server and client.
    private final List<INode> internalNodes = new ArrayList<>();
    private final List<OwnedFloatingNode> externalNodes = new ArrayList<>();
    private final List<AbstractElectricWire> internalWires = new ArrayList<>();

    private final Map<BlockWireEndpoint, Set<BaseWireEntity>> connections = new HashMap<>();
    private boolean destroying = false;
    private boolean rebuildOnClient = false;
    private boolean removed = false;
    private boolean paused = true;

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

    @Nullable
    public ElectricalNetwork getNetwork() {
        if(externalNodes.isEmpty()) {
            if(internalNodes.isEmpty())
                return null;
            // Same as below, only the first node really matters.
            for(var node : internalNodes) {
                return node.getNetwork();
            }
        }
        // Since every node has to have the same network we can
        // just take the network of the first external node and
        // assume that every other node belongs to it.
        for(var node : externalNodes) {
            if(node != null) {
                return node.getNetwork();
            }
        }
        return null;
    }

    public void joinNetwork(ElectricalNetwork network) {
        if(externalNodes.isEmpty() && internalNodes.isEmpty())
            throw new IllegalStateException("Cannot join a network if no nodes are defined");
        if(getNetwork() == null) {
            externalNodes.forEach(node -> {
                if(node != null)
                    network.addNode(node);
            });
            if(!paused) {
                internalNodes.forEach(network::addNode);
                internalWires.forEach(network::addWire);
            }
        } else {
            externalNodes.forEach(node -> {
                if(node != null && node.getNetwork() == null) {
                    network.addNode(node);
                }
            });
            if(!paused) {
                internalNodes.forEach(node -> {
                    if (node.getNetwork() == null)
                        network.addNode(node);
                });
                internalWires.forEach(wire -> {
                    if (wire.getNetwork() == null)
                        network.addWire(wire);
                });
            }
        }
    }

    public void rebuildCircuit() {
        var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
        builder.with(getNetwork());
        if(paused)
            builder.paused();
        builder.clear();
        element.buildCircuit(builder);

        // Break connections if external node was removed.
        var iter = connections.entrySet().iterator();
        while(iter.hasNext()) {
            var entry = iter.next();
            var endpoint = entry.getKey();
            if(endpoint.getTerminal() < externalNodes.size() && externalNodes.get(endpoint.getTerminal()) != null) {
                // Rewire
                for(var entity : entry.getValue())
                    entity.makeWire();
                continue;
            }
            var connCopy = List.copyOf(entry.getValue());
            for(BaseWireEntity entity : connCopy) {
                entity.endpointRemoved(endpoint);
            }
            iter.remove();
        }
        if(getWorld() != null)
            GlobalElectricNetworks.nodeHolderAdded(this);

        var world = getWorld();
        if(world != null && !world.isClientSide)
            rebuildOnClient = true;
    }

    public List<INode> getInternalNodes() {
        return internalNodes;
    }

    public List<OwnedFloatingNode> getExternalNodes() {
        return externalNodes;
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    public void pause() {
        if(!paused) {
            paused = true;
            element.paused();
            internalWires.forEach(AbstractElectricWire::remove);
            var network = getNetwork();
            if(network != null) {
                // Remove nodes in reverse order.
                for(int i = internalNodes.size() - 1; i >= 0; --i) {
                    var node = internalNodes.get(i);
                    network.removeNode(node);
                }
            }
        }
    }

    public void unpause() {
        if(paused) {
            paused = false;
            var network = getNetwork();
            if (network == null)
                return;
            // External nodes weren't removed so they don't have to be added.
            if(hasInternals()) {
                GlobalElectricNetworks.prepareUnpaused(this);
                internalNodes.forEach(network::addNode);
                internalWires.forEach(network::addWire);
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
            if(nbt.getBoolean("Rebuild"))
                rebuildCircuit();
        }
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(clientPacket) {
            if(rebuildOnClient) {
                nbt.putBoolean("Rebuild", true);
                rebuildOnClient = false;
            }
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
}
