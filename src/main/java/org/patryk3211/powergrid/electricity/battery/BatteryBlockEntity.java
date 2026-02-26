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
package org.patryk3211.powergrid.electricity.battery;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

public class BatteryBlockEntity extends ElectricBlockEntity {
    protected VoltageSourceCoupling sourceCoupling;

    protected final BatterySpec spec;
    protected double capacity;
    protected double energy;
    protected boolean refreshLinks;

    public BatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        spec = ((AbstractBatteryBlock<?>) state.getBlock()).getSpec();
        capacity = spec.getMaxCharge();
        energy = spec.getInitialCharge();
        updateParameters();
        setLazyTickRate(20);
    }

    @Override
    public boolean isNoisy() {
        return false;
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    public void updateParameters() {
        if(energy <= 0)
            return;
        float chargeLevel = (float) (energy / capacity);
        sourceCoupling.setVoltage(spec.calculateVoltage(chargeLevel));
        sourceCoupling.setResistance(spec.calculateResistance(chargeLevel));
    }

    /**
     * Calculate power draw from the battery
     * @return Positive power draws energy, negative power recharges
     */
    public float calculatePower() {
        return -sourceCoupling.getCurrent() * sourceCoupling.getVoltage();
    }

    @Override
    public void electricalTick() {
        if(sourceCoupling != null) {
            // Internal resistive losses
            var I = sourceCoupling.getCurrent();
            if(thermalBehaviour != null && sourceCoupling.isConverged())
                thermalBehaviour.applyTickPower(I * I * sourceCoupling.getResistance());
        }

        if(sourceCoupling == null)
            return;

        // Extracted energy
        var power = calculatePower();
        if(Math.abs(power) > 0.05f) {
            refreshLinks = true;
        }
        energy -= power * 0.05f;
        if(energy <= 0) {
            // If a battery reaches zero volts it is probably dead.
            // This should be simulated with a high resistance.
            energy = 0;
            sourceCoupling.setVoltage(0);
            return;
        } else if(energy >= capacity) {
            energy = capacity;
            // TODO: Convert excess energy into heat
        }
        setChanged();

        // Calculate new parameters
        updateParameters();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        sendData();
        if(refreshLinks) {
            refreshLinks = false;
            DisplayLinkBlock.notifyGatherers(level, worldPosition);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("Energy", energy);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        energy = tag.getDouble("Energy");
        updateParameters();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        sourceCoupling = builder.addInternalNode(VoltageSourceCoupling.class, builder.terminalNode(0), builder.terminalNode(1), 0.5f);
    }

    public void setEnergy(double energy) {
        this.energy = energy;
        updateParameters();
        if(!level.isClientSide)
            notifyUpdate();
    }

    public double getCapacity() {
        return capacity;
    }

    public double getEnergy() {
        return energy;
    }
}
