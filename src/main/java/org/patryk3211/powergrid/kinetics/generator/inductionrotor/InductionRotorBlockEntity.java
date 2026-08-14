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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.sim.calculation.Precalculated;
import org.patryk3211.powergrid.electricity.sim.calculation.PrecalculatedN;
import org.patryk3211.powergrid.electricity.sim.calculation.StampedSupplier;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlockEntity;

public class InductionRotorBlockEntity extends RotorBlockEntity {
    public final PrecalculatedN<Float, StampedSupplier<Precalculated<Float>>> totalField = new PrecalculatedN<>(this::recalculateField, 0.0f);
    protected float fieldMultiplier = 1;

    public InductionRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void initialize() {
        super.initialize();
        neighborsChanged();
    }

    @Override
    protected float damageRadius() {
        return 0.55f;
    }

    public void neighborsChanged() {
        assert level != null;
        var state = getBlockState();
        int index = -1;
        var deps = new StampedSupplier[4];
        for(var dir : Direction.values()) {
            if(dir.getAxis() == state.getValue(InductionRotorBlock.AXIS))
                continue;
            ++index;
            var otherState = level.getBlockState(worldPosition.relative(dir));
            if(otherState.getBlock() instanceof WindingBlock winding) {
                var magnetic = winding.getMagneticAxis(otherState);
                if(magnetic != dir.getAxis())
                    continue;
                var be = level.getBlockEntity(worldPosition.relative(dir));
                if(be instanceof WindingBlockEntity wbe) {
                    deps[index] = wbe::fieldStrengthCalc;
                }
            }
        }
        totalField.updateDependency(deps);
    }

    private void recalculateField(StampedSupplier<Precalculated<Float>>[] deps, Precalculated<Float>.ValueHandler handler) {
        float sum = 0;
        // Average field around sides of the rotor.
        for(int i = 0; i < deps.length; ++i) {
            if(deps[i] == null)
                continue;
            var calc = deps[i].get();
            if(calc != null)
                sum += calc.get();
        }
        handler.emit(sum / deps.length * fieldMultiplier);
    }
}
