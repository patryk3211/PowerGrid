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
package org.patryk3211.powergrid.electricity.wire.powercord;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ISocketElectric;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

import java.util.Objects;

public class SocketEndpoint implements ICordEndpoint {
    private BlockPos pos;

    public SocketEndpoint() {
        this(null);
    }

    public SocketEndpoint(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public IWireEndpoint makeOffset(BlockPos offset) {
        return new SocketEndpoint(pos.offset(offset));
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.SOCKET;
    }

    @Nullable
    public ISocketElectric getSocketBlock(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        return ISocketElectric.getAt(world, pos);
    }

    @Nullable
    public ElectricBehaviour getElectricBehaviour(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        return BlockEntityBehaviour.get(world, pos, ElectricBehaviour.TYPE);
    }

    @NotNull
    public Direction getFacing(Level world) {
        var state = world.getBlockState(pos);
        if(state.hasProperty(BlockStateProperties.FACING))
            return state.getValue(BlockStateProperties.FACING);
        if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return Direction.NORTH;
    }

    @Override
    public void read(CompoundTag nbt) {
        var optionalPos = NbtUtils.readBlockPos(nbt,"Pos");
        if (optionalPos.isPresent())
            pos = NbtUtils.readBlockPos(nbt,"Pos").get();
    }

    @Override
    public void write(CompoundTag nbt) {
        nbt.put("Pos", NbtUtils.writeBlockPos(pos));
    }

    @Override
    public @NotNull Vec3 getExactPosition(Level world) {
        var socketed = getSocketBlock(world);
        if(socketed == null)
            return Vec3.atCenterOf(pos);
        var placement = socketed.socket(world.getBlockState(pos));
        var origin = placement.getOrigin();
        return new Vec3(pos.getX() + origin.x, pos.getY() + origin.y, pos.getZ() + origin.z)
                .relative(getFacing(world), -3 / 16f);
    }

    @Override
    public BlockWireEndpoint getEndpoint1() {
        return new BlockWireEndpoint(pos, 0);
    }

    @Override
    public BlockWireEndpoint getEndpoint2() {
        return new BlockWireEndpoint(pos, 1);
    }

    @Override
    public boolean isValid(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return false;
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return false;
        return behaviour.hasTerminal(0) && behaviour.hasTerminal(1);
    }

    public BlockPos getPosition() {
        return pos;
    }

    public boolean hasConnection(Level world) {
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return false;
        var connections1 = behaviour.getConnections().get(getEndpoint1());
        var connections2 = behaviour.getConnections().get(getEndpoint2());
        if(connections1 == null || connections2 == null)
            return false;
        for(var wire : connections2) {
            if(!(wire instanceof CordEntity))
                continue;
            if(connections1.contains(wire)) {
                // Same wire connected to both endpoints,
                // if either endpoint of this wire is this endpoint,
                // then this socket is occupied.
                if(equals(wire.getEndpoint1()) || equals(wire.getEndpoint2()))
                    return true;
            }
        }
        return false;
    }

    @Nullable
    public CordEntity getConnection(Level world) {
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return null;
        var connections1 = behaviour.getConnections().get(getEndpoint1());
        var connections2 = behaviour.getConnections().get(getEndpoint2());
        if(connections1 == null || connections2 == null)
            return null;
        for(var wire : connections2) {
            if(!(wire instanceof CordEntity cord))
                continue;
            if(connections1.contains(wire)) {
                // Same wire connected to both endpoints,
                // if either endpoint of this wire is this endpoint,
                // then this socket is occupied.
                if(equals(wire.getEndpoint1()) || equals(wire.getEndpoint2()))
                    return cord;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof SocketEndpoint other) {
            return Objects.equals(pos, other.pos);
        }
        return false;
    }

    @Override
    public MoveAction shouldMove(Level level, Iterable<BlockPos> allBlocks) {
        for(var block : allBlocks) {
            if(block.equals(pos))
                return MoveAction.MOVE;
        }
        return MoveAction.STAY;
    }
}
