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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.UUID;

public class DeferredJunctionWireEndpoint implements IWireEndpoint {
    private BlockPos entityPos;
    private UUID entityId;
    private int segmentIndex;
    private int segmentPoint;

    public DeferredJunctionWireEndpoint() {

    }

    public DeferredJunctionWireEndpoint(BlockWireEntity entity, int segmentIndex, int segmentPoint) {
        this.entityPos = entity.getBlockPos();
        this.entityId = entity.getUuid();
        this.segmentIndex = segmentIndex;
        this.segmentPoint = segmentPoint;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.DEFERRED_JUNCTION;
    }

    @Override
    public void read(NbtCompound nbt) {
        var posArray = nbt.getIntArray("Pos");
        entityPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        entityId = nbt.getUuid("Id");
        segmentIndex = nbt.getInt("Index");
        segmentPoint = nbt.getInt("Point");
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putIntArray("Pos", new int[] { entityPos.getX(), entityPos.getY(), entityPos.getZ() });
        nbt.putUuid("Id", entityId);
        nbt.putInt("Index", segmentIndex);
        nbt.putInt("Point", segmentPoint);
    }

    public BlockWireEntity getEntity(World world) {
        var entities = world.getEntitiesByClass(BlockWireEntity.class, new Box(entityPos), e -> entityId.equals(e.getUuid()));
        if(entities.isEmpty())
            return null;
        return entities.get(0);
    }

    @Override
    public Vec3d getExactPosition(World world) {
        var wire = getEntity(world);
        var segment = wire.segments.get(segmentIndex);
        return segment.start.offset(segment.direction, segmentPoint / 16f);
    }

    public JunctionWireEndpoint resolve(World world) {
        var entity = getEntity(world);
        return entity.split(segmentIndex, segmentPoint);
    }

    @Override
    public IElectricNode getNode(World world) {
        throw new IllegalStateException("Cannot fetch node");
    }

    @Override
    public void joinNetwork(World world, ElectricalNetwork network) {
        throw new IllegalStateException("Cannot join network");
    }

    @Override
    public void assignWireEntity(WireEntity entity) {
        throw new IllegalStateException("Cannot assign a wire entity");
    }

    @Override
    public void removeWireEntity(WireEntity entity) {
        throw new IllegalStateException("Cannot remove a wire entity");
    }
}
