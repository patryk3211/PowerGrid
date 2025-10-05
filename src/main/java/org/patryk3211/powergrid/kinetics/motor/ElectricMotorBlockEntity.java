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

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.mixin.KineticBlockEntityAccessor;

import java.util.List;

public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    public static final int AVERAGING_TICKS = 5;

    protected ElectricBehaviour electricBehaviour;
    @Nullable
    protected ThermalBehaviour thermalBehaviour;

    private ElectricWire coil;

    private float generatedSpeed = 0;

    private float avgSpeed;

    public ElectricMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(AVERAGING_TICKS);
    }

    public float torque() {
        return (float) (BlockStressValues.getCapacity(getBlockState().getBlock()) * ModdedConfigs.server().kinetics.torqueForStress.getF());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        var maxPower = 256 * torque() * Math.PI / 30;
        var baseFactor = ThermalBehaviour.dissipationFactor((float) maxPower, 150);
        thermalBehaviour = ThermalBehaviour.simple(this, 3.5f, baseFactor);
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
            electricBehaviour.remove();
        }
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        generatedSpeed = compound.getFloat("GeneratedSpeed");
        updateGeneratedRotation();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("GeneratedSpeed", generatedSpeed);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        var newSpeed = (int) (avgSpeed / AVERAGING_TICKS);
        avgSpeed = 0;
        if(!level.isClientSide || isVirtual()) {
            // Max speed constraints.
            if(newSpeed > 256)
                newSpeed = 256;
            if(newSpeed < -256)
                newSpeed = -256;

            // Update speed from average power.
            if(newSpeed != generatedSpeed) {
                generatedSpeed = newSpeed;
                updateGeneratedRotation();
            }
        }
    }

    @Override
    public void tick() {
        applyLostPower(coil.power());

        if(!level.isClientSide || isVirtual()) {
            var speedFromPower = (coil.power() / torque()) * 30 / Math.PI;
            avgSpeed += (float) speedFromPower * Math.signum(coil.current());
        }
        super.tick();
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
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(ElectricMotorBlock.FACING));
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        coil = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }
}
