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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.*;

public class JunctionWireEndpoint implements IWireEndpoint {
    private static final Map<World, Map<Integer, NodeEntry>> JUNCTION_NODES = new HashMap<>();
    private static int NEXT_ID = 0;

    private int id;
    private Vec3d pos;

    public JunctionWireEndpoint() {
        this(null, -1);
    }

    public JunctionWireEndpoint(Vec3d pos) {
        this(pos, NEXT_ID++);
    }

    private JunctionWireEndpoint(Vec3d pos, int id) {
        this.pos = pos;
        this.id = id;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.JUNCTION;
    }

    @Override
    public void read(NbtCompound nbt) {
        pos = new Vec3d(
                nbt.getFloat("X"),
                nbt.getFloat("Y"),
                nbt.getFloat("Z")
        );
        id = nbt.getInt("Id");
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putFloat("X", (float) pos.x);
        nbt.putFloat("Y", (float) pos.y);
        nbt.putFloat("Z", (float) pos.z);
        nbt.putInt("Id", id);
    }

    @Override
    public Vec3d getExactPosition(World world) {
        return pos;
    }

    @Override
    public IElectricNode getNode(World world) {
        return getNode(world, id).node;
    }

    @Override
    public void joinNetwork(World world, ElectricalNetwork network) {
        var node = getNode(world);
        if(node.getNetwork() == null)
            network.addNode(node);
    }

    @Override
    public void assignWireEntity(World world, BlockPos position, UUID id) {
        var entry = getNode(world, this.id);
        entry.holders.add(id);
    }

    @Override
    public void removeWireEntity(World world, UUID id) {
        var entry = getNode(world, this.id);
        entry.holders.remove(id);
        if(entry.holders.isEmpty()) {
            // Last entity dropped this junction.
            removeEntry(world, this.id);
        }
    }

    private static NodeEntry getNode(World world, int id) {
        if(id < 0)
            throw new IllegalArgumentException("Invalid id passed to junction node map");
        var worldNodeMap = JUNCTION_NODES.computeIfAbsent(world, k -> new HashMap<>());
        return worldNodeMap.computeIfAbsent(id, k -> new NodeEntry());
    }

    private static void removeEntry(World world, int id) {
        var worldNodeMap = JUNCTION_NODES.get(world);
        if(worldNodeMap == null)
            return;
        var entry = worldNodeMap.remove(id);
        if(entry == null)
            return;
        if(!entry.holders.isEmpty()) {
            PowerGrid.LOGGER.error("Tried to remove junction endpoint entry for a junction with holders");
            return;
        }
        var network = entry.node.getNetwork();
        if(network == null)
            return;
        network.removeNode(entry.node);
    }

    private static class NodeEntry {
        public final FloatingNode node = new FloatingNode();
        public final Set<UUID> holders = new HashSet<>();
    }
}
