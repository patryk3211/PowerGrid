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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.mixin.KineticBlockEntityAccessor;

import java.util.List;

public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    protected ElectricBehaviour electricBehaviour;
    @Nullable
    protected ThermalBehaviour thermalBehaviour;

    private ElectricWire coil;

    private float generatedSpeed = 0;

    public ElectricMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public float resistance() {
        return ResistanceValues.get(getBlockState().getBlock());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        thermalBehaviour = ThermalBehaviour.simple(this, 3.5f, 1.75f);
        if(thermalBehaviour != null)
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
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        generatedSpeed = compound.getFloat("GeneratedSpeed");
        updateGeneratedRotation();
        updateDissipation();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("GeneratedSpeed", generatedSpeed);
    }

    @Override
    public void tick() {
        super.tick();

        applyLostPower(coil.power());
        var voltage = coil.potentialDifference();
        if(!level.isClientSide || isVirtual()) {
            var newSpeed = (int) (voltage * ElectricMotorBlock.rpmPerVolt());
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
    public void applyNewSpeed(float prevSpeed, float speed) {
        super.applyNewSpeed(prevSpeed, speed);
        if(Math.signum(prevSpeed) == Math.signum(speed)) {
            // HACK: To prevent varying voltage from annihilating the network through flickering speed,
            // the electric motor removes the score it added through its speed update.
            for (var entry : getOrCreateNetwork().members.keySet()) {
                ((KineticBlockEntityAccessor) entry).setFlickerTally(Math.max(entry.getFlickerScore() - 5, 0));
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
        if(thermalBehaviour != null)
            thermalBehaviour.setDissipationFactor(Math.max(Math.abs(getSpeed()) * 0.2f, 0.3f));
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(ElectricMotorBlock.FACING));
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        coil = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }
}
