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
package org.patryk3211.powergrid.electricity.deviceconnector;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.febridge.IFEBridgeHandler;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

public class FEBridgeEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage, IFEBridgeHandler {
    private final BlockEntity be;
    public long capacity;
    public long amount;

    public FEBridgeEnergyStorage(BlockEntity be) {
        this.be = be;
    }

    public static float voltToFE() {
        return ModdedConfigs.server().electricity.forgeEnergyPerVolt.getF();
    }

    public static float ampToFE() {
        return ModdedConfigs.server().electricity.forgeEnergyPerAmp.getF();
    }

    @Override
    protected Long createSnapshot() {
        return this.amount;
    }

    @Override
    protected void readSnapshot(Long snapshot) {
        this.amount = snapshot;
    }

    @Override
    protected void onFinalCommit() {
        be.setChanged();
    }

    @Override
    public boolean supportsInsertion() {
        return false;
    }

    @Override
    public long insert(long l, TransactionContext transactionContext) {
        // Insertion not allowed
        return 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        long extracted = Math.min(maxAmount, this.amount);
        if(extracted > 0L) {
            this.updateSnapshots(transaction);
            this.amount -= extracted;
            return extracted;
        } else {
            return 0L;
        }
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public long getCapacity() {
        return capacity;
    }

    @Override
    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public void charge(SwitchedWire wire) {
        float ampToFe = FEBridgeEnergyStorage.ampToFE();
        if(wire.getState()) {
            amount += Math.round(wire.current() * ampToFe);
            be.setChanged();
        }
    }

    @Override
    public long moveEnergy() {
        if(amount > 0) {
            // Try to move energy
            var facing = be.getBlockState().getValue(BlockStateProperties.FACING);
            var sideStorage = EnergyStorage.SIDED.find(be.getLevel(), be.getBlockPos().relative(facing), facing.getOpposite());
            return EnergyStorageUtil.move(this, sideStorage, Long.MAX_VALUE, null);
        }
        return 0;
    }

    @Override
    public void manageWire(SwitchedWire wire) {
        long maxCharge = (long) (wire.potentialDifference() * FEBridgeEnergyStorage.voltToFE());
        capacity = maxCharge;
        long missingCharge = maxCharge - amount;
        if(missingCharge <= 0) {
            wire.setState(false);
            return;
        }

        float targetAmps = missingCharge / ampToFE();
        float resistance = wire.potentialDifference() / targetAmps;
        wire.setResistance(resistance);
        wire.setState(true);
    }
}
