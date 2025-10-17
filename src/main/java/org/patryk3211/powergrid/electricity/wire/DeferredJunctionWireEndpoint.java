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

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeferredJunctionWireEndpoint implements IWireEndpoint {
    private BlockPos entityPos;
    private UUID entityId;
    private int segmentIndex;
    private int segmentPoint;

    public DeferredJunctionWireEndpoint() {

    }

    public DeferredJunctionWireEndpoint(BlockWireEntity entity, int segmentIndex, int segmentPoint) {
        this.entityPos = entity.blockPosition();
        this.entityId = entity.getUUID();
        this.segmentIndex = segmentIndex;
        this.segmentPoint = segmentPoint;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.DEFERRED_JUNCTION;
    }

    @Override
    public void read(CompoundTag nbt) {
        var posArray = nbt.getIntArray("Pos");
        entityPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        entityId = nbt.getUUID("Id");
        segmentIndex = nbt.getInt("Index");
        segmentPoint = nbt.getInt("Point");
    }

    @Override
    public void write(CompoundTag nbt) {
        nbt.putIntArray("Pos", new int[] { entityPos.getX(), entityPos.getY(), entityPos.getZ() });
        nbt.putUUID("Id", entityId);
        nbt.putInt("Index", segmentIndex);
        nbt.putInt("Point", segmentPoint);
    }

    @Nullable
    public BlockWireEntity getEntity(Level world) {
        var entities = world.getEntitiesOfClass(BlockWireEntity.class, new AABB(entityPos), e -> entityId.equals(e.getUUID()));
        if(entities.isEmpty())
            return null;
        return entities.get(0);
    }

    @Override
    @NotNull
    public Vec3 getExactPosition(Level world) {
        var wire = getEntity(world);
        if(wire == null)
            return entityPos.getCenter();
        if(segmentIndex >= wire.segments.size())
            segmentIndex = wire.segments.size() - 1;
        var segment = wire.segments.get(segmentIndex);
        return segment.start.relative(segment.direction, segmentPoint / 16f);
    }

    @Nullable
    public JunctionWireEndpoint resolve(Level world) {
        var entity = getEntity(world);
        if(entity == null)
            return null;
        return entity.split(segmentIndex, segmentPoint);
    }

    @Override
    public <T extends BaseWireEntity> boolean canAcceptType(Class<T> clazz) {
        return BlockWireEntity.class.isAssignableFrom(clazz);
    }
}
