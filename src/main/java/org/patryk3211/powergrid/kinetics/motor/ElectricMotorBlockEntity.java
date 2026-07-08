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
import org.patryk3211.powergrid.advancements.PGAdvancementBehaviour;
import org.patryk3211.powergrid.collections.ModdedAdvancements;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.mixin.KineticBlockEntityAccessor;

import java.util.List;

import static org.patryk3211.powergrid.PowerGrid.maxRPM;

public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    public static final int AVERAGING_TICKS = 5;

    public static final float CONVERSION_CONSTANT = (float) (60 * Math.PI / 2);

    protected ElectricBehaviour electricBehaviour;
    @Nullable
    protected ThermalBehaviour thermalBehaviour;

    private ElectricWire coil;

    private float generatedSpeed = 0;

    private float avgSpeed;

    public ElectricMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(AVERAGING_TICKS - 1);
    }

    public float torque() {
        return (float) (BlockStressValues.getCapacity(getBlockState().getBlock()) * ModdedConfigs.server().kinetics.torqueForStress.getF());
    }

    public static double calculateSpeed(double power, double torque) {
        return power / torque * CONVERSION_CONSTANT;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        var awards = new PGAdvancementBehaviour(this, ModdedAdvancements.ELECTRIC_MOTOR);
        behaviours.add(awards);

        var maxPower = maxRPM() * torque() / CONVERSION_CONSTANT;
        var baseFactor = ThermalBehaviour.dissipationFactor(maxPower, 150);
        thermalBehaviour = ThermalBehaviour.simple(this, 3.5f, baseFactor);
        if(thermalBehaviour != null) {
            behaviours.add(thermalBehaviour);
            awards.add(ModdedAdvancements.BLOW_UP);
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
        assert level != null;
        super.lazyTick();
        var newSpeed = (int) (avgSpeed / AVERAGING_TICKS);
        avgSpeed = 0;
        if(!level.isClientSide || isVirtual()) {
            // Max speed constraints.
            if(newSpeed > maxRPM())
                newSpeed = maxRPM();
            if(newSpeed < -maxRPM())
                newSpeed = -maxRPM();

            // Update speed from average power.
            if(newSpeed != generatedSpeed) {
                generatedSpeed = newSpeed;
                updateGeneratedRotation();
                if(newSpeed != 0) {
                    var awards = getBehaviour(PGAdvancementBehaviour.TYPE);
                    if(awards != null)
                        awards.awardPlayer(ModdedAdvancements.ELECTRIC_MOTOR);
                }
            }
        }
    }

    @Override
    public void tick() {
        assert level != null;
        if(!level.isClientSide || isVirtual()) {
            applyPower(coil);
            avgSpeed += (float) (calculateSpeed(coil.power(), torque()) * Math.signum(coil.current()));
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
