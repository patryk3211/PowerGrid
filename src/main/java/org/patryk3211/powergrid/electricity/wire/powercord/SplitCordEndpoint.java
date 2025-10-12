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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

public class SplitCordEndpoint implements ICordEndpoint {
    private BlockWireEndpoint endpoint1;
    private BlockWireEndpoint endpoint2;

    public SplitCordEndpoint() {
        endpoint1 = null;
        endpoint2 = null;
    }

    public SplitCordEndpoint(BlockWireEndpoint endpoint1, BlockWireEndpoint endpoint2) {
        this.endpoint1 = endpoint1;
        this.endpoint2 = endpoint2;
    }

    @Override
    public WireEndpointType type() {
        return WireEndpointType.SPLIT_CORD;
    }

    @Override
    public void read(CompoundTag nbt) {
        if(nbt.contains("Endpoint1")) {
            endpoint1 = new BlockWireEndpoint();
            endpoint1.read(nbt.getCompound("Endpoint1"));
        } else {
            endpoint1 = null;
        }
        if(nbt.contains("Endpoint2")) {
            endpoint2 = new BlockWireEndpoint();
            endpoint2.read(nbt.getCompound("Endpoint2"));
        } else {
            endpoint2 = null;
        }
    }

    @Override
    public void write(CompoundTag nbt) {
        if(endpoint1 != null) {
            nbt.put("Endpoint1", endpoint1.serialize());
        }
        if(endpoint2 != null) {
            nbt.put("Endpoint2", endpoint2.serialize());
        }
    }

    @Override
    public @NotNull Vec3 getExactPosition(Level world) {
        if(endpoint1 != null && endpoint2 != null) {
            return endpoint1.getExactPosition(world).add(endpoint2.getExactPosition(world)).scale(0.5);
        } else {
            return Vec3.ZERO;
        }
    }

    @Override
    public boolean isValid(Level world) {
        if(endpoint1 == null || endpoint2 == null)
            return false;
        return endpoint1.isValid(world) && endpoint2.isValid(world);
    }

    @Override
    public IWireEndpoint getEndpoint1() {
        return endpoint1;
    }

    @Override
    public IWireEndpoint getEndpoint2() {
        return endpoint2;
    }
}
