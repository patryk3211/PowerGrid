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
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;

import java.util.UUID;

;

public class BlockWireEntityEndpoint implements IWireEndpoint {
    private BlockPos entityPos;
    private UUID entityId;
    private boolean end;

    public BlockWireEntityEndpoint() {
        entityPos = null;
        entityId = null;
        end = false;
    }

    public BlockWireEntityEndpoint(BlockWireEntity entity, boolean end) {
        this.entityPos = entity.blockPosition();
        this.entityId = entity.getUUID();
        this.end = end;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.BLOCK_WIRE;
    }

    @Override
    public void read(CompoundTag nbt) {
        entityId = nbt.getUUID("Id");
        var posArray = nbt.getIntArray("Pos");
        entityPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        end = nbt.getBoolean("End");
    }

    @Override
    public void write(CompoundTag nbt) {
        nbt.putUUID("Id", entityId);
        nbt.putIntArray("Pos", new int[] { entityPos.getX(), entityPos.getY(), entityPos.getZ() });
        nbt.putBoolean("End", end);
    }

    public BlockWireEntity getEntity(Level world) {
        var entityList = world.getEntitiesOfClass(BlockWireEntity.class, new AABB(entityPos), e -> e.getUUID().equals(entityId));
        if(entityList.isEmpty())
            return null;
        return entityList.get(0);
    }

    @Override
    @NotNull
    public Vec3 getExactPosition(Level world) {
        var entity = getEntity(world);
        if(entity == null)
            return entityPos.getCenter();
        if(!end)
            return entity.position();
        var pos = entity.position();
        for(var segment : entity.segments) {
            pos = pos.add(segment.vector());
        }
        return pos;
    }

    public boolean getEnd() {
        return end;
    }

    @Override
    public OwnedFloatingNode getNode(Level world) {
        return null;
    }

    @Override
    public void joinNetwork(Level world, ElectricalNetwork network) {
        throw new IllegalStateException("Cannot join network");
    }

    @Override
    public void assignWireEntity(WireEntity entity) { }
    @Override
    public void removeWireEntity(WireEntity entity) { }
}
