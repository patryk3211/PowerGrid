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
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.INode;
import org.patryk3211.powergrid.electricity.wire.IWire;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ElectricBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<ElectricBehaviour> TYPE = new BehaviourType<>();

    private final IElectricEntity element;

    private final List<INode> internalNodes = new LinkedList<>();
    private final List<IElectricNode> externalNodes = new LinkedList<>();
    private final List<ElectricWire> internalWires = new LinkedList<>();

    private final List<List<Connection>> connections;
    private boolean nbtChanged = false;

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

        setLazyTickRate(10);
    }

    public void joinNetwork(ElectricalNetwork network) {
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
        if(targetConnection != null && targetConnection.wire != null) {
            // Everything should be fine if the other connection already has a wire.
            connection.wire = targetConnection.wire;
            return true;
        }

        var targetNode = targetBehaviour.getTerminal(connection.targetTerminal);
        if(targetNode == null)
            return false;

        float R = ((IWire) connection.usedWire.getItem()).getResistance();
        var wire = GlobalElectricNetworks.makeConnection(this, getTerminal(sourceTerminal), targetBehaviour, targetNode, R);

        connection.wire = wire;
        if(targetConnection != null) {
            targetConnection.wire = wire;
        } else {
            targetBehaviour.addConnection(connection.targetTerminal, new Connection(getPos(), sourceTerminal, wire, connection.usedWire.copy()));
        }

        if(world.isClient) {
            if(targetConnection != null && targetConnection.renderParameters == null) {
                connection.renderParameters = new ConnectionRenderParameters(
                        IElectric.getRenderPosition(getPos(), blockEntity.getCachedState(), sourceTerminal),
                        IElectric.getRenderPosition(connection.target, world.getBlockState(connection.target), connection.targetTerminal)
                );
            }
        }
        return true;
    }

    @Override
    public void lazyTick() {
        if(nbtChanged) {
            for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
                var sourceConnections = connections.get(sourceTerminal);
                List<Connection> removed = new LinkedList<>();
                for(var connection : sourceConnections) {
                    if(!buildConnection(sourceTerminal, connection))
                        removed.add(connection);
                }
                sourceConnections.removeAll(removed);
            }
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

    public void removeConnection(int sourceTerminal, BlockPos target, int targetTerminal) {
        var sourceConnections = connections.get(sourceTerminal);
        for (Connection connection : sourceConnections) {
            if(connection.target.equals(target) && connection.targetTerminal == targetTerminal) {
                sourceConnections.remove(connection);
                blockEntity.notifyUpdate();
                return;
            }
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
                    // TODO: Update resistance if needed.
                } else {
                    // Add connection, wire will be populated by lazy tick,
                    // because world might not be valid here.
                    connections.get(sourceTerminal).add(nbtConnection);
                    nbtChanged = true;
                }
            }

            // Remove non-existent connections.
            connections.get(sourceTerminal).removeAll(sourceConnections);
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
        List<ItemStack> dropped = new LinkedList<>();
        for(int sourceTerminal = 0; sourceTerminal < connections.size(); ++sourceTerminal) {
            var sourceConnections = connections.get(sourceTerminal);
            for(var connection : sourceConnections) {
                if(getWorld().getBlockEntity(connection.target) instanceof SmartBlockEntity entity) {
                    var behaviour = entity.getBehaviour(TYPE);
                    if(behaviour == null)
                        continue;
                    // Remove the complementary connection.
                    behaviour.removeConnection(connection.targetTerminal, getPos(), sourceTerminal);
                }
                if(connection.wire != null)
                    connection.wire.remove();
                dropped.add(connection.usedWire);
            }
            sourceConnections.clear();
        }
        for(ItemStack stack : dropped) {
            Block.dropStack(getWorld(), getPos(), stack);
        }
        blockEntity.notifyUpdate();
    }

    @Override
    public void destroy() {
        breakConnections();
        super.destroy();
    }

    public record ConnectionRenderParameters(Vec3d pos1, Vec3d pos2) { }

    public static class Connection {
        public final BlockPos target;
        public final int targetTerminal;
        public ElectricWire wire;
        public final ItemStack usedWire;
        public ConnectionRenderParameters renderParameters;

        public Connection(BlockPos target, int targetTerminal, ElectricWire wire, ItemStack usedWire) {
            this.target = target;
            this.targetTerminal = targetTerminal;
            this.wire = wire;
            this.usedWire = usedWire;
        }
        public Connection(BlockPos target, int targetTerminal, ElectricWire wire, ItemStack usedWire, ConnectionRenderParameters renderParameters) {
            this(target, targetTerminal, wire, usedWire);
            this.renderParameters = renderParameters;
        }

        NbtCompound serialize() {
            var tag = new NbtCompound();
            tag.putIntArray("position", new int[] { target.getX(), target.getY(), target.getZ() });
            tag.putInt("terminal", targetTerminal);
            tag.put("stack", usedWire.serializeNBT());
            return tag;
        }

        public boolean isEquivalent(Connection other) {
            return other.target.equals(target) && other.targetTerminal == targetTerminal;
        }

        public static Connection fromNbt(NbtCompound tag) {
            var posArray = tag.getIntArray("position");
            var pos = new BlockPos(posArray[0], posArray[1], posArray[2]);
            var terminal = tag.getInt("terminal");

            return new Connection(pos, terminal, null, ItemStack.fromNbt(tag.getCompound("stack")));
        }
    }
}
