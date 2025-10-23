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
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

import java.util.List;

public class GeneratorClutchBlockEntity extends KineticBlockEntity implements RotorBehaviour.IForceSource {
    protected RotorBehaviour rotorBehaviour;

    private int currentRedstonePower;

    public float load;
    private boolean recalculateStress = false;

    public GeneratorClutchBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        currentRedstonePower = 0;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour = new RotorBehaviour(this, ModdedConfigs.server().kinetics.generatorControls.generatorClutchInertia.getF());
        rotorBehaviour.forceSource(this);
        rotorBehaviour.noField();
        rotorBehaviour.setChangeCallback(this::assemblyChanged);
        behaviours.add(rotorBehaviour);
    }

    private void assemblyChanged() {
        recalculateStress = true;
    }

    public float torqueForStress() {
        return ModdedConfigs.server().kinetics.torqueForStress.getF();
    }

    @Override
    public float sourceForce(float velocity) {
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
    public void tick() {
        super.tick();
        if(recalculateStress) {
            if (hasNetwork() && !level.isClientSide) {
                var network = getOrCreateNetwork();
                network.remove(this);
                network.add(this);
            }
            recalculateStress = false;
            notifyUpdate();
        }
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putByte("Power", (byte) currentRedstonePower);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        currentRedstonePower = compound.getByte("Power");
    }

    @Override
    public float calculateStressApplied() {
        var totalImpact = new MutableFloat(0.0f);
        rotorBehaviour.forEachSegment(segment ->
            totalImpact.add(BlockStressValues.getImpact(segment.blockEntity.getBlockState().getBlock()))
        );
        float couplingStrength = (15 - currentRedstonePower) / 15f;
        this.lastStressApplied = totalImpact.getValue() * couplingStrength;
        return lastStressApplied;
    }

    @Override
    public void remove() {
        super.remove();
        rotorBehaviour.remove();
    }
}
