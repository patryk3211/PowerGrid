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

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.node.*;

public class BatteryBlockEntity extends ElectricBlockEntity {
    protected VoltageSourceNode sourceNode;
    protected TransformerCoupling coupling;

    protected final BatterySpec spec;
    protected double capacity;
    protected double energy;

    public BatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        spec = ((AbstractBatteryBlock<?>) state.getBlock()).getSpec();
        capacity = spec.getMaxCharge();
        energy = spec.getInitialCharge();
        updateParameters();
        setLazyTickRate(20);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var spec = ((AbstractBatteryBlock<?>) getCachedState().getBlock()).getSpec();
        return new ThermalBehaviour(this, spec.getThermalMass(), spec.getDissipationFactor());
    }

    public void updateParameters() {
        if(energy <= 0)
            return;
        float chargeLevel = (float) (energy / capacity);
        sourceNode.setVoltage(spec.calculateVoltage(chargeLevel));
        coupling.setResistance(spec.calculateResistance(chargeLevel));
    }

    /**
     * Calculate power draw from the battery
     * @return Positive power draws energy, negative power recharges
     */
    public float calculatePower() {
        return sourceNode.getCurrent() * sourceNode.getVoltage();
    }

    @Override
    public void tick() {
        super.tick();

        if(sourceNode == null)
            return;

        // Internal resistive losses
        var I = sourceNode.getCurrent();
        applyLostPower(I * I * coupling.getResistance());

        // Extracted energy
        var power = calculatePower();
        energy -= power * 0.05f;
        if(energy <= 0) {
            // If a battery reaches zero volts it is probably dead.
            // This should be simulated with a high resistance.
            energy = 0;
            sourceNode.setVoltage(0);
            return;
        } else if(energy >= capacity) {
            energy = capacity;
            // TODO: Convert excess energy into heat
        }
        markDirty();

        // Calculate new parameters
        updateParameters();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        sendData();
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putDouble("Energy", energy);
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        energy = tag.getDouble("Energy");
        updateParameters();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        sourceNode = builder.addInternalNode(VoltageSourceNode.class);
        coupling = builder.couple(1, 0.5f, sourceNode, builder.terminalNode(0), builder.terminalNode(1));
    }

    public void setEnergy(double energy) {
        this.energy = energy;
        updateParameters();
        notifyUpdate();
    }

    public double getCapacity() {
        return capacity;
    }

    public double getEnergy() {
        return energy;
    }
}
