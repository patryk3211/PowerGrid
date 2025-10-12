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
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.wire.BaseWireEntity;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

public class SocketEndpoint implements ICordEndpoint {
    private BlockPos pos;
    private int terminal;

    @Override
    public WireEndpointType type() {
        return WireEndpointType.SOCKET;
    }

    public IElectric getElectricBlock(Level world) {
        if(!world.hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ())))
            return null;
        return IElectric.getAt(world, pos);
    }

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
    public void read(CompoundTag nbt) {

    }

    @Override
    public void write(CompoundTag nbt) {

    }

    @Override
    public @NotNull Vec3 getExactPosition(Level world) {
        return null;
    }

    @Override
    public IWireEndpoint getEndpoint1() {
        return null;
    }

    @Override
    public IWireEndpoint getEndpoint2() {
        return null;
    }
}
