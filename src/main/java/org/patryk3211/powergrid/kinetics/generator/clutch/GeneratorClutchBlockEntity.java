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
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBehaviour;

import java.util.List;

public class GeneratorClutchBlockEntity extends KineticBlockEntity {
    protected RotorBehaviour rotorBehaviour;
    private float currentImpact;

    private int currentRedstonePower;

    public float load;

    public GeneratorClutchBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        currentImpact = (float) BlockStressValues.getImpact(state.getBlock());
        currentRedstonePower = 0;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour = new RotorBehaviour(this, ModdedConfigs.server().kinetics.generatorClutchInertia.getF());
        rotorBehaviour.noField();
        behaviours.add(rotorBehaviour);
    }

    public void updateStrength(int receivedRedstonePower) {
        if(currentRedstonePower != receivedRedstonePower) {
            currentRedstonePower = receivedRedstonePower;
            notifyUpdate();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(getTheoreticalSpeed() == 0)
            return;

        var angularVelocity = rotorBehaviour.getAngularVelocity();
        float couplingStrength = (15 - currentRedstonePower) / 15f;
        int segmentCount = rotorBehaviour.getSegmentCount();

        float delta = getTheoreticalSpeed() - angularVelocity;
        if(getTheoreticalSpeed() < 0)
            delta = -delta;
        delta = Math.max(0, delta);

        float maxForce = ModdedConfigs.server().kinetics.generatorClutchForcePerSegment.getF() * segmentCount;
        final var defaultImpact = (float) ModdedConfigs.server().kinetics.stressValues.getImpact(getBlockState().getBlock()).getAsDouble();
        if(hasNetwork()) {
            var network = getOrCreateNetwork();
            var newImpact = defaultImpact * segmentCount;
            if(newImpact != currentImpact) {
                currentImpact = newImpact;
                if(!level.isClientSide)
                    network.updateStressFor(this, newImpact);
            }
        }

        float force = 0;
        if(delta > 0 && !isOverStressed()) {
            force = delta * 20f * rotorBehaviour.getInertia();
            force = Math.min(Math.abs(force), maxForce) * Math.signum(getSpeed()) * couplingStrength;
            rotorBehaviour.applyTickForce(force);
        }
        load = force / maxForce;
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
        this.lastStressApplied = currentImpact;
        return currentImpact;
    }
}
