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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Contract;
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
        return getNode(world, id, false).node;
    }

    @Override
    public void joinNetwork(World world, ElectricalNetwork network) {
        var node = getNode(world);
        if(node.getNetwork() == null)
            network.addNode(node);
    }

    @Override
    public void assignWireEntity(WireEntity entity) {
        if(!(entity instanceof BlockWireEntity))
            throw new IllegalArgumentException("Wire junction must receive block wire entities");
        var entry = getNode(entity.getWorld(), this.id, false);
        entry.holders.add(entity);
    }

    @Override
    public void removeWireEntity(WireEntity entity) {
        var entry = getNode(entity.getWorld(), this.id, true);
        if(entry == null)
            return;
        entry.holders.remove(entity);
        if(entry.holders.size() == 2) {
            if(entity.getWorld().isClient)
                return;
            // Two holders remaining, we can merge them.
            BlockWireEntity wire1 = null, wire2 = null;
            for(var holder : entry.holders) {
                if(wire1 == null) {
                    wire1 = (BlockWireEntity) holder;
                } else {
                    wire2 = (BlockWireEntity) holder;
                }
            }
            if(wire1 == null || wire2 == null) {
                // Since holders' size was 2 we must get 2 wires, otherwise the set was altered before the loop started.
                throw new ConcurrentModificationException();
            }
            assert wire1.getWireItem() == wire2.getWireItem();
            var wire1End = this.equals(wire1.getEndpoint2());
            var wire2End = this.equals(wire2.getEndpoint2());
            // Preemptively remove entry since it is going to be discarded anyway.
            entry.holders.clear();
            removeEntry(entity.getWorld(), this.id);

            boolean flipped = false, targetFlipped = false;
            BlockWireEntity target, source;
            if(wire1End || !wire2End) {
                source = wire2;
                if(!wire1End) {
                    // New entity must be made with flipped wire1
                    target = wire1.flip();
                    targetFlipped = true;
                } else {
                    // We can append wire2 into wire1
                    target = wire1;
                }
                if(wire2End)
                    flipped = true;
            } else {
                // Append wire1 onto wire2
                source = wire1;
                target = wire2;
            }

            var lastIndex = target.segments.size() - 1;
            var last = target.segments.get(lastIndex);
            if(!targetFlipped)
                target.segments.set(lastIndex, new BlockWireEntity.Point(last.direction, last.gridLength + 1));

            if(flipped) {
                var segments = new ArrayList<BlockWireEntity.Point>();
                for(var segment : source.segments) {
                    segments.add(0, new BlockWireEntity.Point(segment.direction.getOpposite(), segment.gridLength));
                }
                target.setEndpoint2(source.getEndpoint1());
                target.extend(segments, source.getWireCount());
            } else {
                target.setEndpoint2(source.getEndpoint2());
                target.extend(source.segments, source.getWireCount());
            }
            source.discard();
            target.makeWire();
        } else if(entry.holders.size() == 1) {
            // One holder remaining, remove junction from it and drop the entry.
            for(var holder : entry.holders) {
                // TODO: Use WireEntity::endpointRemoved here once block wire endpointEndpoint removed handler is improved.
                if(this.equals(holder.getEndpoint1()))
                    holder.setEndpoint1(null);
                if(this.equals(holder.getEndpoint2()))
                    holder.setEndpoint2(null);
            }
            // removeEntry is called by setEndpointN in holder entity.
        } else if(entry.holders.isEmpty()) {
            // Last entity dropped this junction.
            removeEntry(entity.getWorld(), this.id);
        }
    }

    @Contract("_, _, false -> !null")
    private static NodeEntry getNode(World world, int id, boolean nullable) {
        if(id < 0)
            throw new IllegalArgumentException("Invalid id passed to junction node map");
        if(!nullable) {
            var worldNodeMap = JUNCTION_NODES.computeIfAbsent(world, k -> new HashMap<>());
            return worldNodeMap.computeIfAbsent(id, k -> new NodeEntry());
        } else {
            var worldNodeMap = JUNCTION_NODES.get(world);
            if(worldNodeMap == null)
                return null;
            return worldNodeMap.get(id);
        }
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

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof JunctionWireEndpoint other) {
            // Note: We don't compare position since it might be slightly different due to imprecision.
            return this.id == other.id;
        }
        return false;
    }

    private static class NodeEntry {
        public final FloatingNode node = new FloatingNode();
        public final Set<WireEntity> holders = new HashSet<>();
    }
}
