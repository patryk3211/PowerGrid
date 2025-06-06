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
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.UUID;

public class ImaginaryWireEndpoint implements IWireEndpoint {
    private Vec3d pos;

    public ImaginaryWireEndpoint() {
        pos = null;
    }

    public ImaginaryWireEndpoint(Vec3d pos) {
        this.pos = pos;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.IMAGINARY;
    }

    @Override
    public void read(NbtCompound nbt) {
        var tag = nbt.getCompound("Pos");
        pos = new Vec3d(
                tag.getFloat("X"),
                tag.getFloat("Y"),
                tag.getFloat("Z")
        );
    }

    @Override
    public void write(NbtCompound nbt) {
        var tag = new NbtCompound();
        tag.putFloat("X", (float) pos.x);
        tag.putFloat("Y", (float) pos.y);
        tag.putFloat("Z", (float) pos.z);
    }

    @Override
    public Vec3d getExactPosition(World world) {
        return pos;
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
    public void assignWireEntity(WireEntity entity) {
        throw new IllegalStateException("Cannot join network");
    }

    @Override
    public void removeWireEntity(WireEntity entity) {
        throw new IllegalStateException("Cannot join network");
    }
}
