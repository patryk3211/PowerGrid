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

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlock;

import java.util.List;

public class ServoBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    public static final float MAX_SPEED = 32.0f;

    protected ElectricBehaviour electricBehaviour;
    protected ThermalBehaviour thermalBehaviour;
    private float generatedSpeed;
    private float currentAngle;
    private float prevTarget;

    private ElectricWire coil;
    private ElectricWire control;

    public ServoBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        thermalBehaviour = ThermalBehaviour.forMaxPower(this, 3.5f, 200);
        behaviours.add(thermalBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
        // 5V is 360 degrees clock-wise. Servo has a [-5V, 5V] range
        float newTarget = MathHelper.clamp(control.potentialDifference() / 5.0f * 360.0f, -360f, 360f);
        newTarget = prevTarget * 0.5f + newTarget * 0.5f;
        prevTarget = newTarget;

        applyLostPower(coil.power());

        // Target coil voltage is 20V
        float maxSpeed = Math.min(Math.abs(coil.potentialDifference()) * 0.05f, 1.0f) * MAX_SPEED;
        if(maxSpeed == 0 && generatedSpeed != 0) {
            generatedSpeed = 0;
            updateGeneratedRotation();
            notifyUpdate();
            return;
        }

        float rotation = (newTarget - currentAngle) / 360.0f;
        if(Math.abs(rotation) < 0.01f)
            rotation = 0;

        var speed = MathHelper.clamp(rotation / 0.05f * 60.0f, -maxSpeed, maxSpeed);
        if(speed != generatedSpeed) {
            generatedSpeed = speed;
            updateGeneratedRotation();
            notifyUpdate();
        }

        if(generatedSpeed != 0) {
            coil.setResistance(ServoBlock.resistanceOn());
        } else {
            coil.setResistance(ServoBlock.resistanceIdle());
        }

        currentAngle += generatedSpeed / 60.0f * 0.05f * 360f;
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
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(3);
        coil = builder.connect(ServoBlock.resistanceIdle(), builder.terminalNode(0), builder.terminalNode(1));
        control = builder.connect(1000f, builder.terminalNode(2), builder.terminalNode(1));
    }

    @Override
    protected void read(NbtCompound compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        generatedSpeed = compound.getFloat("GeneratedSpeed");
        currentAngle = compound.getFloat("Angle");
        updateGeneratedRotation();
    }

    @Override
    protected void write(NbtCompound compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("GeneratedSpeed", generatedSpeed);
        compound.putFloat("Angle", currentAngle);
    }

    @Override
    public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed, getCachedState().get(ElectricMotorBlock.FACING));
    }
}
