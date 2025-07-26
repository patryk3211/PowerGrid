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
package org.patryk3211.powergrid.electricity.febridge;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

public class FEBridgeBlockEntity extends ElectricBlockEntity {
    private final FEBridgeEnergyStorage energyStorage = new FEBridgeEnergyStorage(this);
    private SwitchedWire wire;
    private long currentRate;

    public FEBridgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();

        energyStorage.charge(wire);

        if(energyStorage.amount > 0) {
            // Try to move energy
            var facing = getCachedState().get(FEBridgeBlock.FACING);
            var sideStorage = EnergyStorage.SIDED.find(world, pos.offset(facing), facing.getOpposite());
            var moved = EnergyStorageUtil.move(energyStorage, sideStorage, Long.MAX_VALUE, null);
            if(!world.isClient) {
                if(moved != currentRate) {
                    currentRate = moved;
                    sendData();
                }
            } else if(moved == 0) {
                // Since some things might not sync this to client, this is necessary
                // to provide a valid, client-side simulation parameters.
                energyStorage.amount -= currentRate;
                if(energyStorage.amount < 0)
                    energyStorage.amount = 0;
            }
        }

        energyStorage.manageWire(wire);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1));
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putLong("Energy", energyStorage.amount);
        if(clientPacket)
            tag.putLong("Rate", currentRate);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        energyStorage.amount = tag.getLong("Energy");
        if(clientPacket)
            currentRate = tag.getLong("Rate");
    }

    public @Nullable EnergyStorage getEnergyStorage(@Nullable Direction dir) {
        if(dir == null || getCachedState().get(FEBridgeBlock.FACING) == dir)
            return energyStorage;
        return null;
    }
}

