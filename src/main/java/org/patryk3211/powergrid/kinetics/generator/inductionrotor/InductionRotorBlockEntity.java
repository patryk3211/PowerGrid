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
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlockEntity;

import java.util.List;

public class InductionRotorBlockEntity extends RotorBlockEntity {
    public float field;

    public InductionRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float inertia() {
        return ModdedConfigs.server().kinetics.generatorControls.generatorInductionRotorInertia.getF();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    public float calculateField() {
        assert level != null;
        field = 0;
        var state = getBlockState();
        for(var dir : Direction.values()) {
            if(dir.getAxis() == state.getValue(InductionRotorBlock.AXIS))
                continue;
            var otherState = level.getBlockState(worldPosition.relative(dir));
            if(otherState.getBlock() instanceof WindingBlock winding) {
                var magnetic = winding.getMagneticAxis(otherState);
                if(magnetic != dir.getAxis())
                    continue;
                var be = level.getBlockEntity(worldPosition.relative(dir));
                if(be instanceof WindingBlockEntity wbe) {
                    // Average field around 4 sides of the rotor.
                    field += wbe.fieldStrength() * 0.25f;
                }
            }
        }
        return field;
    }
}
