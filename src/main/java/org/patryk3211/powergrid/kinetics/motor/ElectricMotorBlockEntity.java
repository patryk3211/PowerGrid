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
package org.patryk3211.powergrid.kinetics.motor;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import java.util.List;

public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    protected ElectricBehaviour electricBehaviour;
    protected ThermalBehaviour thermalBehaviour;

    private ElectricWire coil;

    private float generatedSpeed = 0;

    public ElectricMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        thermalBehaviour = new ThermalBehaviour(this, 3.5f, 0.75f);
        behaviours.add(thermalBehaviour);
    }

    protected void applyLostPower(float power) {
        if(thermalBehaviour != null)
            thermalBehaviour.applyTickPower(power);
    }

    @Override
    public void remove() {
        super.remove();
        if(electricBehaviour != null) {
            electricBehaviour.breakConnections();
        }
    }

    @Override
    protected void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        generatedSpeed = compound.getFloat("GeneratedSpeed");
        updateGeneratedRotation();
        updateDissipation();
    }

    @Override
    protected void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("GeneratedSpeed", generatedSpeed);
    }

    @Override
    public void tick() {
        super.tick();

        var voltage = coil.potentialDifference();
        applyLostPower(voltage * voltage / coil.getResistance());
        if(!world.isClient || isVirtual()) {
            var newSpeed = (int) (voltage * 2.0f);
            // Max speed constraints.
            if(newSpeed > 256)
                newSpeed = 256;
            if(newSpeed < -256)
                newSpeed = -256;

            // Update speed from average applied voltage.
            var diffPercentage = Math.abs((newSpeed - generatedSpeed) / generatedSpeed);
            if(diffPercentage >= 0.02) {
                // Update if speed difference larger than 2%.
                // This should make the motor easier to control and prevent excessive updates.
                generatedSpeed = newSpeed;
                updateGeneratedRotation();
            }
        }
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        updateDissipation();
    }

    public void updateDissipation() {
        // Simulate a fan moving more air and providing more cooling
        thermalBehaviour.setDissipationFactor(Math.max(Math.abs(getSpeed()) * 0.2f, 0.3f));
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getCachedState().get(ElectricMotorBlock.FACING));
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        coil = builder.connect(10, builder.terminalNode(0), builder.terminalNode(1));
    }
}
