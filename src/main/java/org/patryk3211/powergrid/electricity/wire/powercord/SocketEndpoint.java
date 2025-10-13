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
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.ISocketElectric;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

public class SocketEndpoint implements ICordEndpoint {
    private BlockPos pos;

    public SocketEndpoint() {
        this(null);
    }

    public SocketEndpoint(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.SOCKET;
    }

    public ISocketElectric getSocketBlock(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        return ISocketElectric.getAt(world, pos);
    }

    public ElectricBehaviour getElectricBehaviour(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        return BlockEntityBehaviour.get(world, pos, ElectricBehaviour.TYPE);
    }

    @Override
    public void read(CompoundTag nbt) {
        pos = NbtUtils.readBlockPos(nbt.getCompound("Pos"));
    }

    @Override
    public void write(CompoundTag nbt) {
        nbt.put("Pos", NbtUtils.writeBlockPos(pos));
    }

    @Override
    public @NotNull Vec3 getExactPosition(Level world) {
        var socketed = getSocketBlock(world);
        var placement = socketed.socket(world.getBlockState(pos));
        var origin = placement.getOrigin();
        return new Vec3(pos.getX() + origin.x, pos.getY() + origin.y, pos.getZ() + origin.z);
    }

    @Override
    public IWireEndpoint getEndpoint1() {
        return new BlockWireEndpoint(pos, 0);
    }

    @Override
    public IWireEndpoint getEndpoint2() {
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
}
