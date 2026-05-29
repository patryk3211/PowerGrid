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
package org.patryk3211.powergrid.kinetics.generator.clutch;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class GeneratorClutchBlockEntity extends GeneratingKineticBlockEntity implements RotorBehaviour.IForceSource {
    protected RotorBehaviour rotorBehaviour;

    private ScrollOptionBehaviour<ClutchMode> mode;

    private int currentRedstonePower;

    public float load;
    private float motorLoad;
    private boolean recalculateStress = false;
    private int generatedSpeed;
    private int prevRedstoneOut;

    public GeneratorClutchBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        currentRedstonePower = 0;
        setLazyTickRate(10);
    }

    public void changeMode() {
        if(hasNetwork()) {
            var network = getOrCreateNetwork();
            network.remove(this);
            if(mode.get() == ClutchMode.GENERATOR)
                generatedSpeed = 0;
            network.add(this);
            updateGeneratedRotation();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour = new RotorBehaviour(this, ModdedConfigs.server().kinetics.generatorControls.generatorClutchInertia.getF());
        rotorBehaviour.forceSource(this);
        rotorBehaviour.setChangeCallback(this::assemblyChanged);
        behaviours.add(rotorBehaviour);

        mode = new ScrollOptionBehaviour<>(ClutchMode.class, Lang.translateDirect("gui.clutch_mode"), this, new Box());
        mode.setValue(0);
        mode.withCallback(i -> changeMode());
        behaviours.add(mode);
    }

    private void assemblyChanged() {
        recalculateStress = true;
    }

    public float torqueForStress() {
        return ModdedConfigs.server().kinetics.torqueForStress.getF();
    }

    @Override
    public float sourceForce(float velocity) {
        if(mode.get() == ClutchMode.MOTOR)
            return 0;
        if(getTheoreticalSpeed() == 0 || isOverStressed())
            return 0;
        return (float) (torqueForStress() * lastStressApplied / 30 * Math.PI);
    }

    @Override
    public float forceSpeed() {
        return getTheoreticalSpeed();
    }

    @Override
    public void receiveUsedForce(float percent) {
        load = percent;
    }

    public void updateStrength(int receivedRedstonePower) {
        if(currentRedstonePower != receivedRedstonePower) {
            currentRedstonePower = receivedRedstonePower;
            recalculateStress = true;
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return generatedSpeed;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if(mode.get() == ClutchMode.MOTOR && !level.isClientSide) {
            var newSpeed = (int) rotorBehaviour.getAngularVelocity();
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
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        motorLoad = currentStress / maxStress;
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if(hasNetwork())
            updateFromNetwork(capacity, stress, getOrCreateNetwork().getSize());
    }

    public int getRedstoneOutput() {
        float load = getLoad();
        return Mth.floor(load * 14.0f) + (load > 0.001 ? 1 : 0);
    }

    public float getLoad() {
        float load;
        if(mode.get() == ClutchMode.MOTOR) {
            load = this.motorLoad;
        } else {
            load = this.load;
        }
        return load;
    }

    @Override
    public void tick() {
        super.tick();
        if(recalculateStress) {
            if(hasNetwork() && !level.isClientSide) {
                var network = getOrCreateNetwork();
                network.updateStressFor(this, calculateStressApplied());
                network.updateCapacityFor(this, calculateAddedStressCapacity());
                notifyUpdate();
            }
            recalculateStress = false;
        }
        if(mode.get() == ClutchMode.MOTOR) {
            var force = (float) (torqueForStress() * Mth.clamp(motorLoad, 0, 1) * lastCapacityProvided / 30 * Math.PI);
            var maxForce = rotorBehaviour.getAngularVelocity() * rotorBehaviour.getInertia() * 20f;
            if(force > Math.abs(maxForce)) {
                force = Math.abs(maxForce);
            }
            rotorBehaviour.applyTickForce(-force * Math.signum(rotorBehaviour.getAngularVelocity()));
        }
        if(getRedstoneOutput() != prevRedstoneOut) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            prevRedstoneOut = getRedstoneOutput();
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putByte("Power", (byte) currentRedstonePower);
        if(generatedSpeed != 0)
            compound.putInt("GeneratedSpeed", generatedSpeed);
        if(clientPacket && mode.get() == ClutchMode.MOTOR) {
            compound.putFloat("MotorLoad", motorLoad);
        }
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        currentRedstonePower = compound.getByte("Power");
        if(compound.contains("GeneratedSpeed")) {
            generatedSpeed = compound.getInt("GeneratedSpeed");
        } else {
            generatedSpeed = 0;
        }
        if(clientPacket && mode.get() == ClutchMode.MOTOR) {
            motorLoad = compound.getFloat("MotorLoad");
        }
    }

    public float stressSum() {
        var totalImpact = new MutableFloat(0.0f);
        rotorBehaviour.forEachSegment(segment ->
                totalImpact.add(BlockStressValues.getImpact(segment.blockEntity.getBlockState().getBlock()))
        );
        return totalImpact.getValue();
    }

    @Override
    public float calculateStressApplied() {
        if(mode.get() == ClutchMode.GENERATOR) {
            float couplingStrength = (15 - currentRedstonePower) / 15f;
            this.lastStressApplied = stressSum() * couplingStrength;
            return lastStressApplied;
        } else {
            return 0;
        }
    }

    @Override
    public float calculateAddedStressCapacity() {
        if(mode.get() == ClutchMode.MOTOR) {
            float couplingStrength = (15 - currentRedstonePower) / 15f;
            this.lastCapacityProvided = stressSum() * couplingStrength;
            return lastCapacityProvided;
        } else {
            return 0;
        }
    }

    @Override
    public void remove() {
        super.remove();
        rotorBehaviour.remove();
    }

    public enum ClutchMode implements INamedIconOptions {
        GENERATOR, MOTOR;

        @Override
        public AllIcons getIcon() {
            return switch(this) {
                case GENERATOR -> ModIcons.I_GENERATOR;
                case MOTOR -> ModIcons.I_MOTOR;
            };
        }

        @Override
        public String getTranslationKey() {
            return switch(this) {
                case GENERATOR -> "powergrid.gui.clutch_mode.generator";
                case MOTOR -> "powergrid.gui.clutch_mode.motor";
            };
        }
    }

    public static class Box extends CenteredSideValueBoxTransform {
        public Box() {
            super((state, dir) -> state.getValue(GeneratorClutchBlock.FACING).getAxis() != dir.getAxis());
        }
    }
}
