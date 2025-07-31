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

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.febridge.FEBridgeEnergyStorage;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.Optional;
import java.util.function.Supplier;

public class BridgeElectricBehaviour extends ElectricBehaviour {
    private final BlockPos behaviourPosition;
    private ElectricBehaviour mainBehaviour;
    private FEBridgeEnergyStorage bridgeBehaviour;
    private long readEnergy;
    private long currentRate;
    private boolean fetched = false;
    private final Supplier<SwitchedWire> converterWire;

    public <T extends SmartBlockEntity & IElectricEntity> BridgeElectricBehaviour(T be, BlockPos behaviourPosition, Supplier<SwitchedWire> converterWire) {
        super(be);
        this.behaviourPosition = behaviourPosition;
        this.converterWire = converterWire;
    }

    private void constructBehaviours() {
        fetched = true;
        var world = getWorld();
        mainBehaviour = get(world, behaviourPosition, TYPE);
        if(mainBehaviour != null)
            return;
        var facing = blockEntity.getCachedState().get(DeviceConnectorBlock.FACING);
        var energyStorage = EnergyStorage.SIDED.find(world, getPos().offset(facing), facing.getOpposite());
        if(energyStorage != null) {
            bridgeBehaviour = new FEBridgeEnergyStorage(blockEntity);
            bridgeBehaviour.amount = readEnergy;
        } else {
            world.breakBlock(getPos(), true);
        }
    }

    public Optional<ElectricBehaviour> getMainBehaviour() {
        if(!fetched)
            constructBehaviours();
        return Optional.ofNullable(mainBehaviour);
    }

    @Nullable
    public FEBridgeEnergyStorage getBridgeBehaviour() {
        if(!fetched)
            constructBehaviours();
        return bridgeBehaviour;
    }

    @Override
    public void joinNetwork(ElectricalNetwork network) {
        getMainBehaviour().ifPresentOrElse(
                b -> b.joinNetwork(network),
                () -> super.joinNetwork(network)
        );
    }

    @Override
    public @Nullable IElectricNode getTerminal(int index) {
        return getMainBehaviour()
                .map(b -> b.getTerminal(index))
                .orElseGet(() -> super.getTerminal(index));
    }

    @Override
    public boolean hasTerminal(int terminal) {
        return getMainBehaviour()
                .map(b -> b.hasTerminal(terminal))
                .orElseGet(() -> super.hasTerminal(terminal));
    }

    @Override
    public void read(NbtCompound nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);
        if(bridgeBehaviour != null) {
            bridgeBehaviour.amount = nbt.getLong("Energy");
        } else {
            readEnergy = nbt.getLong("Energy");
        }
        if(clientPacket)
            currentRate = nbt.getLong("Rate");
    }

    @Override
    public void write(NbtCompound nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        if(bridgeBehaviour != null) {
            nbt.putLong("Energy", bridgeBehaviour.amount);
            if(clientPacket)
                nbt.putLong("Rate", currentRate);
        }
    }

    @Override
    public void tick() {
        super.tick();
        var energyStorage = getBridgeBehaviour();
        if(energyStorage == null)
            return;

        var wire = converterWire.get();
        var world = getWorld();

        energyStorage.charge(wire);

        if(energyStorage.amount > 0) {
            // Try to move energy
            var facing = blockEntity.getCachedState().get(Properties.FACING);
            var sideStorage = EnergyStorage.SIDED.find(world, getPos().offset(facing), facing.getOpposite());
            var moved = EnergyStorageUtil.move(energyStorage, sideStorage, Long.MAX_VALUE, null);
            if(!world.isClient) {
                if(moved != currentRate) {
                    currentRate = moved;
                    blockEntity.sendData();
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
}
