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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
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
        pos = NbtUtils.readBlockPos(nbt.getCompound("Position"));
        terminal1 = nbt.getInt("Terminal1");
        terminal2 = nbt.getInt("Terminal2");
        placement = new Vec3(nbt.getFloat("X"), nbt.getFloat("Y"), nbt.getFloat("Z"));
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
        nbt.putFloat("X", (float) placement.x);
        nbt.putFloat("Y", (float) placement.y);
        nbt.putFloat("Z", (float) placement.z);
        if(plugFacing != null) {
            nbt.putByte("Plug", (byte) plugFacing.ordinal());
        }
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
}
