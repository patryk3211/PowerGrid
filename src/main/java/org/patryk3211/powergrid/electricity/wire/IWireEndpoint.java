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

public interface IWireEndpoint {
    WireEndpointType type();

    void read(NbtCompound nbt);
    void write(NbtCompound nbt);

    Vec3d getExactPosition(World world);

    IElectricNode getNode(World world);
    void joinNetwork(World world, ElectricalNetwork network);

    void assignWireEntity(World world, BlockPos position, UUID id);
    void removeWireEntity(World world, UUID id);

    default NbtCompound serialize() {
        return type().serialize(this);
    }
}
