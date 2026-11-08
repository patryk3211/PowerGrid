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
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
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
        canPlace();
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

    public void canPlace() {
        assert level != null;
        var state = getBlockState();
        var thisBlock = state.getBlock();
        var rotorAxis = state.getValue(InductionRotorBlock.AXIS);
        boolean canPlace = true;
        for(var dir : Direction.values()) {
            if(dir.getAxis() == rotorAxis)
                continue;
            Direction.Axis axis2;
            if(dir.getAxis() != Direction.Axis.X && rotorAxis != Direction.Axis.X) {
                axis2 = Direction.Axis.X;
            } else if(dir.getAxis() != Direction.Axis.Y && rotorAxis != Direction.Axis.Y) {
                axis2 = Direction.Axis.Y;
            } else {
                axis2 = Direction.Axis.Z;
            }
            for(int i = -1; i <= 1; ++i) {
                var posInner = worldPosition.relative(dir, 1).relative(axis2, i);
                var posOuter = worldPosition.relative(dir, 2).relative(axis2, i);

                if (level.getBlockState(posInner).getBlock() == ModdedBlocks.GENERATOR_INDUCTION_ROTOR.get()) {
                    if (thisBlock == ModdedBlocks.GENERATOR_LARGE_INDUCTION_ROTOR.get()) {
                        canPlace = false;
                    }
                }
                if (level.getBlockState(posInner).getBlock() == ModdedBlocks.GENERATOR_LARGE_INDUCTION_ROTOR.get()) {
                    canPlace = false;
                }
                if (level.getBlockState(posOuter).getBlock() == ModdedBlocks.GENERATOR_LARGE_INDUCTION_ROTOR.get()) {
                    if (this instanceof LargeInductionRotorBlockEntity) {
                        canPlace = false;
                    }
                }
                if (!canPlace) {
                    level.destroyBlock(this.getBlockPos(), true);
                    break;
                }
            }
        }
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
