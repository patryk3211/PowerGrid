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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import com.simibubi.create.content.kinetics.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class RotorBlockEntity extends KineticBlockEntity {
    private RotorBehaviour rotorBehaviour;
    private float currentImpact;

    public RotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        currentImpact = (float) BlockStressValues.getImpact(state.getBlock());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour = new RotorBehaviour(this);
        behaviours.add(rotorBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
        var angularVelocity = rotorBehaviour.getAngularVelocity();

        float delta = getTheoreticalSpeed() - angularVelocity;
        if(getTheoreticalSpeed() < 0)
            delta = -delta;
        delta = Math.max(0, delta);
        float theoreticalForce = delta * 20f * rotorBehaviour.getInertia();

        float maxForce = 20f;
        if(hasNetwork()) {
            var network = getOrCreateNetwork();
            var newImpact = currentImpact;
            if(theoreticalForce <= maxForce / 4) {
                newImpact = 2f;
            } else if(theoreticalForce <= maxForce / 2) {
                newImpact = 4f;
            } else {
                newImpact = 8f;
            }
            if(newImpact != currentImpact) {
                if(!world.isClient)
                    network.updateStressFor(this, newImpact);
                currentImpact = newImpact;
            }
        }

        var speed = getSpeed();
        if(delta > 0 && !isOverStressed()) {
            var force = delta * 20f * rotorBehaviour.getInertia();
            force = Math.min(Math.abs(force), maxForce) * Math.signum(speed);
            rotorBehaviour.applyTickForce(force);
        }
    }

    public RotorBehaviour getRotorBehaviour() {
        return rotorBehaviour;
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = currentImpact;
        return currentImpact;
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        if(getCachedState().get(RotorBlock.SHAFT_DIRECTION) == ShaftDirection.NONE)
            return false;
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
    }
}
