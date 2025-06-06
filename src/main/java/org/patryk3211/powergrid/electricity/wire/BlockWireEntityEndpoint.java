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
        this.entityPos = entity.getBlockPos();
        this.entityId = entity.getUuid();
        this.end = end;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.BLOCK_WIRE;
    }

    @Override
    public void read(NbtCompound nbt) {
        entityId = nbt.getUuid("Id");
        var posArray = nbt.getIntArray("Pos");
        entityPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        end = nbt.getBoolean("End");
    }

    @Override
    public void write(NbtCompound nbt) {
        nbt.putUuid("Id", entityId);
        nbt.putIntArray("Pos", new int[] { entityPos.getX(), entityPos.getY(), entityPos.getZ() });
        nbt.putBoolean("End", end);
    }

    public BlockWireEntity getEntity(World world) {
        var entityList = world.getEntitiesByClass(BlockWireEntity.class, new Box(entityPos), e -> e.getUuid().equals(entityId));
        if(entityList.isEmpty())
            return null;
        return entityList.get(0);
    }

    @Override
    public Vec3d getExactPosition(World world) {
        var entity = getEntity(world);
        if(entity == null)
            return entityPos.toCenterPos();
        if(!end)
            return entity.getPos();
        var pos = entity.getPos();
        for(var segment : entity.segments) {
            pos = pos.add(segment.vector());
        }
        return pos;
    }

    public boolean getEnd() {
        return end;
    }

    @Override
    public IElectricNode getNode(World world) {
        return null;
    }

    @Override
    public void joinNetwork(World world, ElectricalNetwork network) {
        throw new IllegalStateException("Cannot join network");
    }

    @Override
    public void assignWireEntity(WireEntity entity) { }
    @Override
    public void removeWireEntity(WireEntity entity) { }
}
