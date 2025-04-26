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
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
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
    private boolean nbtChanged = false;
    private boolean destroying = false;

    public <T extends SmartBlockEntity & IElectricEntity> ElectricBehaviour(T be) {
        super(be);
        this.element = be;

        element.initializeNodes();
        element.addExternalNodes(externalNodes);
        element.addInternalNodes(internalNodes);
        element.addInternalWires(internalWires);

        connections = new ArrayList<>();
        for(int i = 0; i < externalNodes.size(); ++i)
            connections.add(new LinkedList<>());
    }

    public void joinNetwork(ElectricalNetwork network) {
        assert externalNodes.isEmpty() || externalNodes.get(0).getNetwork() == null;
        internalNodes.forEach(network::addNode);
        externalNodes.forEach(network::addNode);
        internalWires.forEach(network::addWire);
    }

    @Override
    public boolean isSafeNBT() {
        return true;
    }

    @Override
    public void unload() {
        for(var terminalConnections : connections) {
            for(var connection : terminalConnections) {
                if(connection.wire != null)
                    connection.wire.remove();
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
                var entity = getConnectionEntity(connection);
                entity.refreshTerminalPositions();
            }
        }
    }

    private boolean buildConnection(int sourceTerminal, Connection connection) {
        if(connection.wire != null)
            return true;
        var world = getWorld();
        if(!world.isChunkLoaded(getPos()))
            // Keep the connection but don't build a wire.
            return true;

        var target = getWorld().getBlockEntity(connection.target);
        if(!(target instanceof SmartBlockEntity smartEntity))
            return false;

        var targetBehaviour = smartEntity.getBehaviour(TYPE);
        if(targetBehaviour == null)
            return false;

        var targetConnection = targetBehaviour.getConnection(connection.targetTerminal, getPos(), sourceTerminal);
        if(targetConnection == null)
            return false;

        if(targetConnection.wire != null) {
            // Everything should be fine if the other connection already has a wire.
            connection.wire = targetConnection.wire;
            return true;
        }

        var targetNode = targetBehaviour.getTerminal(connection.targetTerminal);
        if(targetNode == null)
            return false;

        float R = connection.resistance;
        var wire = GlobalElectricNetworks.makeConnection(this, getTerminal(sourceTerminal), targetBehaviour, targetNode, R);

        connection.wire = wire;
        targetConnection.wire = wire;
        return true;
    }

    private void connectionRefresh() {
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            var sourceConnections = connections.get(sourceTerminal);
            List<Connection> removed = new LinkedList<>();
            for(var connection : sourceConnections) {
                if(!buildConnection(sourceTerminal, connection) && !getWorld().isClient) {
                    removed.add(connection);
                }
            }
            sourceConnections.removeAll(removed);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if(nbtChanged) {
            connectionRefresh();
            nbtChanged = false;
        }
    }

    public void addConnection(int sourceTerminal, Connection connection) {
        connections.get(sourceTerminal).add(connection);
        blockEntity.notifyUpdate();
    }

    public Connection getConnection(int sourceTerminal, BlockPos target, int targetTerminal) {
        var sourceConnections = connections.get(sourceTerminal);
        for (Connection connection : sourceConnections) {
            if(connection.target.equals(target) && connection.targetTerminal == targetTerminal)
                return connection;
        }
        return null;
    }

    public WireEntity getConnectionEntity(Connection connection) {
        var world = getWorld();
        var entities = world.getNonSpectatingEntities(WireEntity.class, new Box(getPos(), connection.target).expand(1));
        for(var entity : entities) {
            if(entity.getUuid().equals(connection.wireEntityId) && !entity.isRemoved()) {
                return entity;
            }
        }
        return null;
    }

    private void removeConnectionEntity(Connection connection) {
        var world = getWorld();
        if(!world.isClient) {
            var entity = getConnectionEntity(connection);
            if(entity != null)
                entity.kill();
        }
    }

    public void removeConnection(int sourceTerminal, BlockPos target, int targetTerminal) {
        if(destroying)
            return;
        var sourceConnections = connections.get(sourceTerminal);
        for (Connection connection : sourceConnections) {
            if(connection.target.equals(target) && connection.targetTerminal == targetTerminal) {
                sourceConnections.remove(connection);
                blockEntity.notifyUpdate();
                removeConnectionEntity(connection);
                if(connection.wire != null) {
                    connection.wire.remove();
                }
                return;
            }
        }
    }

    private void updateResistance(int sourceTerminal, Connection connection, float resistance) {
        if(connection.wire != null)
            connection.wire.setResistance(resistance);
        connection.resistance = resistance;

        var targetEntity = getWorld().getBlockEntity(connection.target);
        if(targetEntity instanceof SmartBlockEntity smartEntity) {
            var behaviour = smartEntity.getBehaviour(TYPE);
            var complementaryConnection = behaviour.getConnection(connection.targetTerminal, getPos(), sourceTerminal);
            complementaryConnection.resistance = resistance;
        }
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        var connTag = nbt.getCompound("connections");
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            // Create a copy of source connections for keeping track
            // which connections are defined by the new NBT data.
            List<Connection> sourceConnections = new LinkedList<>(connections.get(sourceTerminal));

            var nbtConnList = connTag.getList(Integer.toString(sourceTerminal), NbtElement.COMPOUND_TYPE);
            if(nbtConnList == null) {
                // Delete all connections from this terminal.
                sourceConnections.forEach(connection -> {
                    if(connection.wire != null)
                        connection.wire.remove();
                });
                connections.get(sourceTerminal).clear();
                continue;
            }

            // Check for equivalent connections.
            for(int j = 0; j < nbtConnList.size(); ++j) {
                var nbtConnection = Connection.fromNbt(nbtConnList.getCompound(j));
                Connection equivalent = null;
                for(var connection : sourceConnections) {
                    if(connection.isEquivalent(nbtConnection)) {
                        equivalent = connection;
                        break;
                    }
                }
                if(equivalent != null) {
                    sourceConnections.remove(equivalent);
                    updateResistance(sourceTerminal, equivalent, nbtConnection.resistance);
                } else {
                    // Add connection, wire will be populated by lazy tick,
                    // because world might not be valid here.
                    connections.get(sourceTerminal).add(nbtConnection);
                    nbtChanged = true;
                }
            }

            // Remove non-existent connections.
            for(var connection : sourceConnections) {
                // Remove wire of non-existent connection if one exists.
                if(connection.wire != null)
                    connection.wire.remove();
                // TODO: Might have to remove the entity here as well.
                // This is mostly used by the client when syncing data,
                // which means that entities are handled on the server side.
                // Only when loading data from disk will this be run on the
                // server. Hopefully this means that we don't actually need
                // to care about entities here.
            }
            connections.get(sourceTerminal).removeAll(sourceConnections);
        }

        if(nbtChanged && blockEntity.hasWorld()) {
            connectionRefresh();
            if(!blockEntity.getWorld().isClient)
                nbtChanged = false;
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
        nbt.put("connections", nbtConnectionMap);
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
            if(connection.target.equals(targetPos) && connection.targetTerminal == targetTerminal)
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
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            var sourceConnections = connections.get(sourceTerminal);
            for(var connection : sourceConnections) {
                if(connection.target.equals(getPos())) {
                    removeConnectionEntity(connection);
                }
                if(getWorld().getBlockEntity(connection.target) instanceof SmartBlockEntity entity) {
                    var behaviour = entity.getBehaviour(TYPE);
                    if(behaviour == null)
                        continue;
                    // Remove the complementary connection.
                    behaviour.removeConnection(connection.targetTerminal, getPos(), sourceTerminal);
                }
                if(connection.wire != null)
                    connection.wire.remove();
            }
            sourceConnections.clear();
        }
        blockEntity.notifyUpdate();
        destroying = false;
    }

    public static class Connection {
        public final BlockPos target;
        public final int targetTerminal;
        public ElectricWire wire;
        public float resistance;
        public final UUID wireEntityId;

        public Connection(BlockPos target, int targetTerminal, ElectricWire wire, UUID wireEntityId) {
            this.target = target;
            this.targetTerminal = targetTerminal;
            this.wire = wire;
            this.resistance = wire.getResistance();
            this.wireEntityId = wireEntityId;
        }

        private Connection(BlockPos target, int targetTerminal, UUID wireEntityId, float resistance) {
            this.target = target;
            this.targetTerminal = targetTerminal;
            this.resistance = resistance;
            this.wireEntityId = wireEntityId;
        }

        NbtCompound serialize() {
            var tag = new NbtCompound();
            tag.putIntArray("position", new int[] { target.getX(), target.getY(), target.getZ() });
            tag.putInt("terminal", targetTerminal);
            tag.putUuid("entity", wireEntityId);
            tag.putFloat("resistance", resistance);
            return tag;
        }

        public boolean isEquivalent(Connection other) {
            return other.target.equals(target) && other.targetTerminal == targetTerminal;
        }

        public static Connection fromNbt(NbtCompound tag) {
            var posArray = tag.getIntArray("position");
            var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            var terminal = tag.getInt("terminal");
            var entity = tag.getUuid("entity");

            float resistance = 1;
            if(tag.contains("resistance"))
                resistance = tag.getFloat("resistance");

            return new Connection(pos, terminal, entity, resistance);
        }
    }
}
