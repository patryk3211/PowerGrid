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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEntity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class ElectricBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ElectricBehaviour> TYPE = new BehaviourType<>();

    private final IElectricEntity element;

    private final List<INode> internalNodes = new LinkedList<>();
    private final List<IElectricNode> externalNodes = new LinkedList<>();
    private final List<AbstractElectricWire> internalWires = new LinkedList<>();

    // TODO: Consider making this a map
    private final List<List<WireEntity>> connections;
    private boolean destroying = false;
    private boolean rebuildOnClient = false;

    public <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be) {
        super(be);
        this.element = be;

        var builder = new IElectricEntity.CircuitBuilder(externalNodes, internalNodes, internalWires);
        element.buildCircuit(builder);

        connections = new ArrayList<>();
        for(int i = 0; i < externalNodes.size(); ++i)
            connections.add(new ArrayList<>());
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
            internalNodes.forEach(network::addNode);
            externalNodes.forEach(node -> {
                if(node != null)
                    network.addNode(node);
            });
            internalWires.forEach(network::addWire);
        }
    }

    public void rebuildCircuit() {
        var builder = new IElectricEntity.CircuitBuilder(externalNodes, internalNodes, internalWires);
        builder.with(getNetwork());
        builder.clear();
        element.buildCircuit(builder);

        // Break connections if external node was removed.
        var oldConnections = new ArrayList<>(connections);
        connections.clear();
        for(int i = 0; i < oldConnections.size(); ++i) {
            if(i < externalNodes.size() && externalNodes.get(i) != null) {
                // Rewire
                var nodeConnections = oldConnections.get(i);
                connections.add(nodeConnections);
                if(nodeConnections.isEmpty())
                    continue;
                for(var entity : nodeConnections) {
                    entity.makeWire();
                }
                continue;
            }
            if(i < externalNodes.size()) {
                connections.add(new ArrayList<>());
            }
            var nodeConnections = oldConnections.get(i);
            if(nodeConnections.isEmpty())
                continue;
            var endpoint = new BlockWireEndpoint(getPos(), i);
            var connCopy = List.copyOf(nodeConnections);
            for(var wire : connCopy) {
                wire.endpointRemoved(endpoint);
            }
            nodeConnections.clear();
        }
        if(oldConnections.size() < externalNodes.size()) {
            for(int i = 0; i < externalNodes.size() - oldConnections.size(); ++i) {
                connections.add(new ArrayList<>());
            }
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
        var world = getWorld();
        for(var terminalConnections : connections) {
            for(var entity : terminalConnections) {
                entity.dropWire();
            }
        }
        internalWires.forEach(AbstractElectricWire::remove);
        if(externalNodes.isEmpty())
            return;

        var network = getNetwork();
        if(network != null) {
            externalNodes.forEach(node -> {
                if(node != null)
                    network.removeNode(node);
            });
            internalNodes.forEach(network::removeNode);
        }
    }

    public void refreshConnectionEntities() {
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            for(var entity : connections.get(sourceTerminal)) {
                if(entity instanceof HangingWireEntity wire)
                    wire.refreshTerminalPositions();
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    public void addConnection(int sourceTerminal, WireEntity wire) {
        connections.get(sourceTerminal).add(wire);
        blockEntity.notifyUpdate();
    }

    public void removeConnection(int sourceTerminal, WireEntity wire) {
        connections.get(sourceTerminal).remove(wire);
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

    public boolean hasConnection(int sourceTerminal, BlockWireEndpoint endpoint) {
        var sourceConnections = connections.get(sourceTerminal);
        for(var entity : sourceConnections) {
            if(entity.isConnectedTo(endpoint.getPos(), endpoint.getTerminal()))
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
        if(!world.isClient) {
            for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
                var sourceConnections = connections.get(sourceTerminal);
                var endpoint = new BlockWireEndpoint(getPos(), sourceTerminal);
                for(var entity : sourceConnections) {
                    entity.endpointRemoved(endpoint);
                }
                sourceConnections.clear();
            }
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

    public static class Connection {
        public final BlockPos wireEntityPos;
        public final UUID wireEntityId;

        public Connection(BlockPos wireEntityPos, UUID wireEntityId) {
            this.wireEntityPos = wireEntityPos;
            this.wireEntityId = wireEntityId;
        }

        public WireEntity getEntity(World world) {
            var entities = world.getNonSpectatingEntities(WireEntity.class, new Box(wireEntityPos).expand(1));
            for(var entity : entities) {
                if(wireEntityId.equals(entity.getUuid()))
                    return entity;
            }
            return null;
        }

        public void notifyRemoved(World world, IWireEndpoint endpoint) {
            var entity = getEntity(world);
            if(entity != null) {
                entity.endpointRemoved(endpoint);
            }
        }
    }
}
