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
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.wire.HangingWireEntity;
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
    private final List<ElectricWire> internalWires = new LinkedList<>();

    private final List<List<Connection>> connections;
    private boolean destroying = false;

    public <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be) {
        super(be);
        this.element = be;

        var builder = new IElectricEntity.CircuitBuilder(externalNodes, internalNodes, internalWires);
        element.buildCircuit(builder);

        connections = new ArrayList<>();
        for(int i = 0; i < externalNodes.size(); ++i)
            connections.add(new ArrayList<>());
    }

    public void joinNetwork(ElectricalNetwork network) {
        assert externalNodes.isEmpty() || externalNodes.get(0).getNetwork() == null;
        internalNodes.forEach(network::addNode);
        externalNodes.forEach(network::addNode);
        internalWires.forEach(network::addWire);
    }

    public void rebuildCircuit() {
        var builder = new IElectricEntity.CircuitBuilder(externalNodes, internalNodes, internalWires);
        if(!externalNodes.isEmpty()) {
            builder.with(externalNodes.get(0).getNetwork());
        }
        builder.alterExternal(false);
        builder.clear();
        element.buildCircuit(builder);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void unload() {
        var world = getWorld();
        for(var terminalConnections : connections) {
            for(var connection : terminalConnections) {
                var entity = connection.getEntity(world);
                if(entity != null) {
                    entity.dropWire();
                }
            }
        }
        internalWires.forEach(ElectricWire::remove);

        // Since every node has to have the same network we can
        // just take the network of the first external node and
        // assume that every other node belongs to it.
        var firstExternal = externalNodes.get(0);
        if(firstExternal != null) {
            var network = firstExternal.getNetwork();
            if (network != null) {
                externalNodes.forEach(network::removeNode);
                internalNodes.forEach(network::removeNode);
            }
        }
    }

    public void refreshConnectionEntities() {
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            for(var connection : connections.get(sourceTerminal)) {
                var entity = connection.getEntity(getWorld());
                if(entity instanceof HangingWireEntity wire)
                    wire.refreshTerminalPositions();
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
    }

    public void addConnection(int sourceTerminal, Connection connection) {
        connections.get(sourceTerminal).add(connection);
        blockEntity.notifyUpdate();
    }

    // Must be called from WireEntity
    public void removeConnection(int sourceTerminal, UUID entityId) {
        var sourceConnections = connections.get(sourceTerminal);
        for(var conn : sourceConnections) {
            if(conn.wireEntityId.equals(entityId)) {
                sourceConnections.remove(conn);
                blockEntity.notifyUpdate();
                return;
            }
        }
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        var connTag = nbt.getCompound("Connections");
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            var sourceConnections = connections.get(sourceTerminal);
            sourceConnections.clear();

            var nbtConnList = connTag.getList(Integer.toString(sourceTerminal), NbtElement.COMPOUND_TYPE);
            if(nbtConnList == null)
                continue;

            for(int j = 0; j < nbtConnList.size(); ++j) {
                var nbtConnection = Connection.fromNbt(nbtConnList.getCompound(j));
                sourceConnections.add(nbtConnection);
            }
        }
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        var nbtConnectionMap = new NbtCompound();
        for (int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            var sourceConnections = connections.get(sourceTerminal);
            if (sourceConnections.isEmpty())
                continue;
            var connectionTagList = new NbtList();
            for (var connection : sourceConnections) {
                connectionTagList.add(connection.serialize());
            }
            nbtConnectionMap.put(Integer.toString(sourceTerminal), connectionTagList);
        }
        nbt.put("Connections", nbtConnectionMap);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public IElectricNode getTerminal(int index) {
        if(index >= externalNodes.size())
            return null;
        return externalNodes.get(index);
    }

    public boolean hasConnection(int sourceTerminal, BlockPos targetPos, int targetTerminal) {
        var sourceConnections = connections.get(sourceTerminal);
        for(var connection : sourceConnections) {
            var entity = connection.getEntity(getWorld());
            if(entity.isConnectedTo(targetPos, targetTerminal))
                return true;
        }
        return false;
    }

    public List<List<Connection>> getConnections() {
        return connections;
    }

    public void breakConnections() {
        if(destroying)
            return;
        destroying = true;
        var world = getWorld();
        if(!world.isClient) {
            for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
                var sourceConnections = connections.get(sourceTerminal);
                for(var connection : sourceConnections)
                    connection.killEntity(world);
                sourceConnections.clear();
            }
        }
        blockEntity.notifyUpdate();
        destroying = false;
    }

    public static class Connection {
        public final BlockPos wireEntityPos;
        public final UUID wireEntityId;

        public Connection(BlockPos wireEntityPos, UUID wireEntityId) {
            this.wireEntityPos = wireEntityPos;
            this.wireEntityId = wireEntityId;
        }

        NbtCompound serialize() {
            var tag = new NbtCompound();
            tag.putIntArray("Position", new int[] { wireEntityPos.getX(), wireEntityPos.getY(), wireEntityPos.getZ() });
            tag.putUuid("Entity", wireEntityId);
            return tag;
        }

        public WireEntity getEntity(World world) {
            var entities = world.getNonSpectatingEntities(WireEntity.class, new Box(wireEntityPos).expand(1));
            for(var entity : entities) {
                if(wireEntityId.equals(entity.getUuid()))
                    return entity;
            }
            return null;
        }

        public void killEntity(World world) {
            var entity = getEntity(world);
            if(entity != null)
                entity.kill();
        }

        public static Connection fromNbt(NbtCompound tag) {
            var posArray = tag.getIntArray("Position");
            var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            var entity = tag.getUuid("Entity");
            return new Connection(pos, entity);
        }
    }
}
