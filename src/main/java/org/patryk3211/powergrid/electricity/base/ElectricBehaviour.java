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
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.*;

public class ElectricBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ElectricBehaviour> TYPE = new BehaviourType<>();

    private final IElectricEntity element;

    private final List<INode> internalNodes = new LinkedList<>();
    private final List<IElectricNode> externalNodes = new LinkedList<>();
    private final List<AbstractElectricWire> internalWires = new LinkedList<>();

    private final Map<BlockWireEndpoint, Set<WireEntity>> connections = new HashMap<>();
    private boolean destroying = false;
    private boolean rebuildOnClient = false;

    public <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be) {
        super(be);
        this.element = be;

        var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
        element.buildCircuit(builder);
    }

    @Nullable
    public ElectricalNetwork getNetwork() {
        if(externalNodes.isEmpty())
            return null;
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
        if(externalNodes.isEmpty())
            throw new IllegalStateException("Cannot join a network if no external nodes are defined");
        if(getNetwork() == null) {
            externalNodes.forEach(node -> {
                if(node != null)
                    network.addNode(node);
            });
            internalNodes.forEach(network::addNode);
            internalWires.forEach(network::addWire);
        }
    }

    public void rebuildCircuit() {
        var builder = new IElectricEntity.CircuitBuilder(getPos(), externalNodes, internalNodes, internalWires);
        builder.with(getNetwork());
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
            for(WireEntity entity : connCopy) {
                entity.endpointRemoved(endpoint);
            }
            iter.remove();
        }

        var world = getWorld();
        if(world != null && !world.isClient)
            rebuildOnClient = true;
    }

    public boolean needsRebuild() {
        return rebuildOnClient;
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void unload() {
        for(var terminalConnections : connections.values()) {
            for(var entity : terminalConnections) {
                entity.dropWire();
            }
        }
        internalWires.forEach(AbstractElectricWire::remove);
        if(externalNodes.isEmpty())
            return;

        var network = getNetwork();
        if(network != null) {
            internalNodes.forEach(network::removeNode);
            externalNodes.forEach(node -> {
                if(node != null)
                    network.removeNode(node);
            });
        }
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
    }

    public void addConnection(BlockWireEndpoint endpoint, WireEntity wire) {
        var sourceConnections = connections.computeIfAbsent(endpoint, key -> new HashSet<>());
        // Check for stale wires here
        sourceConnections.removeIf(Entity::isRemoved);
        sourceConnections.add(wire);
        blockEntity.notifyUpdate();
    }

    public void removeConnection(BlockWireEndpoint endpoint, WireEntity wire) {
        if(connections.containsKey(endpoint)) {
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
    public IElectricNode getTerminal(int index) {
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

    public Map<BlockWireEndpoint, Set<WireEntity>> getConnections() {
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
        if(!world.isClient) {
            for(var entry : connections.entrySet()) {
                var endpoint = entry.getKey();
                for(var entity : entry.getValue()) {
                    entity.endpointRemoved(endpoint);
                }
            }
            connections.clear();
        }
        blockEntity.notifyUpdate();
        destroying = false;
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if(clientPacket) {
            if(nbt.getBoolean("Rebuild"))
                rebuildCircuit();
        }
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(clientPacket) {
            if(rebuildOnClient) {
                nbt.putBoolean("Rebuild", true);
                rebuildOnClient = false;
            }
        }
    }

    public void inheritConnections(ElectricBehaviour otherBehaviour) {
        for(var entry : otherBehaviour.connections.entrySet()) {
            var thisList = connections.computeIfAbsent(entry.getKey(), key -> new HashSet<>());
            thisList.addAll(entry.getValue());
        }
        otherBehaviour.connections.clear();
    }
}
