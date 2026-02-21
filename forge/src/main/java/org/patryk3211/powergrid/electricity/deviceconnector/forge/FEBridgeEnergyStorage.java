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
package org.patryk3211.powergrid.electricity.deviceconnector.forge;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.patryk3211.powergrid.electricity.febridge.IFEBridgeHandler;

public class FEBridgeEnergyStorage implements IEnergyStorage, IFEBridgeHandler {
    private final BlockEntity be;
    public long capacity;
    public long amount;

    public FEBridgeEnergyStorage(BlockEntity be) {
        this.be = be;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        // Insertion not allowed
        return 0;
    }

    @Override
    public int extractEnergy(int maxAmount, boolean simulate) {
        if(maxAmount <= 0)
            return 0;
        int extracted = Math.toIntExact(Math.min(maxAmount, this.amount));
        if(extracted > 0 && !simulate) {
            this.amount -= extracted;
        }
        be.setChanged();
        return extracted;
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public int getEnergyStored() {
        return Math.toIntExact(amount);
    }

    @Override
    public int getMaxEnergyStored() {
        return Math.toIntExact(capacity);
    }

    @Override
    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public long moveEnergy() {
        if(amount > 0) {
            // Try to move energy
            var facing = be.getBlockState().getValue(BlockStateProperties.FACING);
            var neighbour = be.getLevel().getBlockEntity(be.getBlockPos().relative(facing));
            if(neighbour == null)
                return 0;
            final long[] amounts = new long[1];
            var handler = be.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, neighbour.getBlockPos(), facing.getOpposite());
            if (handler != null) {
                var received = handler.receiveEnergy((int) amount, false);
                extractEnergy(received, false);
                amounts[0] = received;
            }
            return amounts[0];
        }
        return 0;
    }

    @Override
    public void setChanged() {
        be.setChanged();
    }
}
