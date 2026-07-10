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
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

import static org.patryk3211.powergrid.PowerGrid.maxRPM;
import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity.CONVERSION_CONSTANT;
import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity.calculateSpeed;

public class ConstantSpeedMotorBlockEntity extends GeneratingKineticBlockEntity implements IElectricEntity {
    public static final int AVERAGING_TICKS = 5;

    protected ElectricBehaviour electricBehaviour;
    @Nullable
    protected ThermalBehaviour thermalBehaviour;

    private SpeedScrollValueBehaviour scrollValue;

    private ElectricWire coil;

    private float generatedSU = 0;

    private float avgSpeed;
    private float load;

    public ConstantSpeedMotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(AVERAGING_TICKS - 1);
    }

    public float torque() {
        return (float) (BlockStressValues.getCapacity(getBlockState().getBlock()) * ModdedConfigs.server().kinetics.torqueForStress.getF());
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        if(ModdedConfigs.server().electricity.motorDynamicResistance.get()) {
            if (maxStress != 0) {
                load = Math.max(currentStress / maxStress, 0.05f);
            } else {
                load = 0.05f;
            }
            coil.setResistance(resistance() / load);
        }
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

        Integer max = AllConfigs.server().kinetics.maxRotationSpeed.get();
        scrollValue = new SpeedScrollValueBehaviour(Lang.translateDirect("devices.motor.speed"), this, new Box());
        scrollValue.between(0, max);
        scrollValue.value = 16;
        scrollValue.withCallback(i -> this.updateGeneratedRotation());
        behaviours.add(scrollValue);
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
        generatedSU = compound.getFloat("GeneratedStress");
        updateGeneratedRotation();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("GeneratedStress", generatedSU);
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
            newSpeed *= (int) BlockStressValues.getCapacity(getBlockState().getBlock());

            // Update speed from average power.
            if(newSpeed != generatedSU) {
                generatedSU = newSpeed;
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
            var V = coil.potentialDifference();
            avgSpeed += (float) (calculateSpeed(V * V / resistance(), torque()) * Math.signum(coil.current()));
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
        if(Math.abs(generatedSU) < 64)
            return 0;
        return convertToDirection(scrollValue.getValue() * (generatedSU < 0 ? -1 : 1), getBlockState().getValue(ElectricMotorBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        if(Math.abs(generatedSU) < 64)
            return 0;
        return Math.abs(generatedSU) / scrollValue.getValue();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        coil = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }

    public static class Box extends CenteredSideValueBoxTransform {
        public Box() {
            super((state, dir) -> {
                var facing = state.getValue(ConstantSpeedMotorBlock.FACING);
                if(facing.getAxis() == Direction.Axis.Y)
                    return dir.getAxis() == Direction.Axis.Z;
                return dir == Direction.UP;
            });
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0f, 8.0f, 12.5f);
        }
    }
}
