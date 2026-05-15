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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

import java.util.Objects;

public class AutoCordEndpoint implements ICordEndpoint {
    private BlockPos pos;
    private int terminal1;
    private int terminal2;
    private Vec3 placement;
    private Direction plugFacing;

    public AutoCordEndpoint() {
        this(null, -1, -1, null, null);
    }

    public AutoCordEndpoint(BlockPos pos, int terminal1, int terminal2, Vec3 placement, @Nullable Direction plugFacing) {
        this.pos = pos;
        this.terminal1 = terminal1;
        this.terminal2 = terminal2;
        this.placement = placement;
        this.plugFacing = plugFacing;
    }

    @Override
    public IWireEndpoint makeOffset(BlockPos offset) {
        return new AutoCordEndpoint(pos.offset(offset), terminal1, terminal2,
                placement.add(offset.getX(), offset.getY(), offset.getZ()), plugFacing);
    }

    @Override
    public IWireEndpoint makeOffset(BlockPos blockOffset, Vec3 offset) {
        return new AutoCordEndpoint(pos.offset(blockOffset), terminal1, terminal2,
                placement.add(offset.x, offset.y, offset.z), plugFacing);
    }

    @Override
    public BlockWireEndpoint getEndpoint1() {
        return new BlockWireEndpoint(pos, terminal1);
    }

    @Override
    public BlockWireEndpoint getEndpoint2() {
        return new BlockWireEndpoint(pos, terminal2);
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.AUTO_CORD;
    }

    @Override
    public void read(CompoundTag nbt) {
        pos = NbtUtils.readBlockPos(nbt, "Position").orElseThrow();
        terminal1 = nbt.getInt("Terminal1");
        terminal2 = nbt.getInt("Terminal2");
        placement = new Vec3(nbt.getDouble("X"), nbt.getDouble("Y"), nbt.getDouble("Z"));
        if(nbt.contains("Plug")) {
            plugFacing = Direction.values()[nbt.getByte("Plug")];
        } else {
            plugFacing = null;
        }
    }

    @Override
    public void write(CompoundTag nbt) {
        nbt.put("Position", NbtUtils.writeBlockPos(pos));
        nbt.putInt("Terminal1", terminal1);
        nbt.putInt("Terminal2", terminal2);
        nbt.putDouble("X", placement.x);
        nbt.putDouble("Y", placement.y);
        nbt.putDouble("Z", placement.z);
        if(plugFacing != null) {
            nbt.putByte("Plug", (byte) plugFacing.ordinal());
        }
    }

    public IElectric getElectricBlock(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        return IElectric.getAt(world, pos);
    }

    @Nullable
    public ElectricBehaviour getElectricBehaviour(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        var electric = getElectricBlock(world);
        if(electric == null)
            return null;
        var state = world.getBlockState(pos);
        return electric.getBehaviour(world, pos, state);
    }

    @Override
    public boolean isValid(Level world) {
        if(!world.isLoaded(pos))
            return false;
        var behaviour = getElectricBehaviour(world);
        if(behaviour == null)
            return false;
        return behaviour.hasTerminal(terminal1) && behaviour.hasTerminal(terminal2);
    }

    @Override
    public @NotNull Vec3 getExactPosition(Level world) {
        return placement;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof AutoCordEndpoint other) {
            return Objects.equals(pos, other.pos)
                    && terminal1 == other.terminal1
                    && terminal2 == other.terminal2;
        }
        return false;
    }

    public BlockPos getPosition() {
        return pos;
    }

    @Nullable
    public Direction getPlugFacing() {
        return plugFacing;
    }

    public AutoCordEndpoint rotate(Rotation rotation) {
        return new AutoCordEndpoint(pos, terminal1, terminal2, placement, plugFacing == null ? null : rotation.rotate(plugFacing));
    }
}
