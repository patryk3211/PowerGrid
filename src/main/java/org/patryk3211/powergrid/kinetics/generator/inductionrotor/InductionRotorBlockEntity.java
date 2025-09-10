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
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class InductionRotorBlockEntity extends RotorBlockEntity {
    private float coilCurrent = 0;

    private List<CommutatorBlockEntity> commutators;

    public InductionRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float inertia() {
        return ModdedConfigs.server().kinetics.generatorInductionRotorInertia.getF();
    }

    private void assemblyChanged() {
        commutators = null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        rotorBehaviour.setChangeCallback(this::assemblyChanged);
    }

    @Override
    public void tick() {
        super.tick();
        coilCurrent = 0;
        if(commutators == null) {
            commutators = new ArrayList<>();
            rotorBehaviour.forEachSegment(segment -> {
                if(segment.blockEntity instanceof CommutatorBlockEntity commutator) {
                    commutators.add(commutator);
                    var current = commutator.getCurrent();
                    if(current > coilCurrent)
                        coilCurrent = current;
                }
            });
        } else {
            for(var commutator : commutators) {
                var current = commutator.getCurrent();
                // TODO: This isn't the best way to handle multiple commutators.
                if(Math.abs(current) > Math.abs(coilCurrent))
                    coilCurrent = current;
            }
        }

        var fieldStrength = coilCurrent * 0.5f;
        if(Math.abs(fieldStrength) < 0.01f)
            fieldStrength = 0;
        rotorBehaviour.setFieldStrength(fieldStrength);
    }
}
