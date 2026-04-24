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
package org.patryk3211.powergrid.kinetics.servo;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock;
import org.patryk3211.powergrid.mixin.KineticBlockEntityAccessor;

import java.util.List;

import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity.*;

public class ServoBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    public static final float MAX_SPEED = 32.0f;

    protected ElectricBehaviour electricBehaviour;
    @Nullable
    protected ThermalBehaviour thermalBehaviour;
    private float generatedSpeed;
    private float currentAngle;
//    private float prevTarget;
    private float maxSpeed;
    private float currentTarget;

    private ElectricWire coil;
    private ElectricWire control;

    private float avgSpeed;
    private float avgTarget;

    public ServoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(AVERAGING_TICKS);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        var maxPower = MAX_SPEED * torque() / CONVERSION_CONSTANT;
        var baseFactor = ThermalBehaviour.dissipationFactor(maxPower, 150);
        thermalBehaviour = ThermalBehaviour.simple(this, 3.5f, baseFactor);
        if(thermalBehaviour != null)
            behaviours.add(thermalBehaviour);
    }

    public float torque() {
        return (float) (BlockStressValues.getCapacity(getBlockState().getBlock()) * ModdedConfigs.server().kinetics.torqueForStress.getF());
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if(level.isClientSide)
            return;

        // 5V is 360 degrees clock-wise. Servo has a [-5V, 5V] range
        currentTarget = Mth.clamp((avgTarget / AVERAGING_TICKS) / 5.0f * 360.0f, -360f, 360f);
        avgTarget = 0;

        maxSpeed = Math.min(avgSpeed / AVERAGING_TICKS, MAX_SPEED);
        avgSpeed = 0;
        if(maxSpeed == 0 && generatedSpeed != 0) {
            generatedSpeed = 0;
            updateGeneratedRotation();
            notifyUpdate();
        }
    }

    @Override
    public void tick() {
        if(!level.isClientSide || isVirtual()) {
            applyPower(coil);
            avgSpeed += (float) calculateSpeed(coil.power(), torque());
            avgTarget += (float) control.potentialDifference();
        }
        super.tick();

        if(!level.isClientSide || isVirtual()) {
            float rotation = (currentTarget - currentAngle) / 360.0f;
            if (Math.abs(rotation) < 0.01f)
                rotation = 0;

            var dT = 0.05f;
            var speed = Mth.clamp(rotation / dT * 60.0f, -maxSpeed, maxSpeed);
            if (speed != generatedSpeed) {
                generatedSpeed = speed;
                updateGeneratedRotation();
                notifyUpdate();
            }

            if (generatedSpeed != 0) {
                coil.setResistance(resistance("on"));
            } else {
                coil.setResistance(resistance("idle"));
            }

            currentAngle += generatedSpeed / 60.0f * dT * 360f;
        }
    }

    protected void applyPower(AbstractElectricWire wire) {
        if(thermalBehaviour != null)
            thermalBehaviour.applyWirePower(wire);
    }

    @Override
    public void remove() {
        super.remove();
        if(electricBehaviour != null) {
            electricBehaviour.remove();
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);
        coil = builder.connect(resistance("idle"), builder.terminalNode(0), builder.terminalNode(1));
        control = builder.connect(1000f, builder.terminalNode(2), builder.terminalNode(1));
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        generatedSpeed = compound.getFloat("GeneratedSpeed");
        currentAngle = compound.getFloat("Angle");
        if(generatedSpeed != 0) {
            coil.setResistance(resistance("on"));
        } else {
            coil.setResistance(resistance("idle"));
        }
        updateGeneratedRotation();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("GeneratedSpeed", generatedSpeed);
        compound.putFloat("Angle", currentAngle);
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getBlockState().getValue(ElectricMotorBlock.FACING));
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
}
