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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.calculation.Precalculated;
import org.patryk3211.powergrid.electricity.sim.calculation.StampedSupplier;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlockEntity;

public class LargeInductionRotorBlockEntity extends InductionRotorBlockEntity {
    public LargeInductionRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        fieldMultiplier = 4;
    }

    @Override
    public void neighborsChanged() {
        assert level != null;
        var state = getBlockState();
        int index = -1;
        var deps = new StampedSupplier[12];
        var rotorAxis = state.getValue(InductionRotorBlock.AXIS);
        for(var dir : Direction.values()) {
            if(dir.getAxis() == rotorAxis)
                continue;
            ++index;
            Direction.Axis axis2;
            if(dir.getAxis() != Direction.Axis.X && rotorAxis != Direction.Axis.X) {
                axis2 = Direction.Axis.X;
            } else if(dir.getAxis() != Direction.Axis.Y && rotorAxis != Direction.Axis.Y) {
                axis2 = Direction.Axis.Y;
            } else {
                axis2 = Direction.Axis.Z;
            }
            for(int i = -1; i <= 1; ++i) {
                var pos = worldPosition.relative(dir, 2).relative(axis2, i);
                deps[index * 3 + i + 1] = new LazyWindingSupplier(pos, dir.getAxis());
            }
        }
        totalField.updateDependency(deps);
    }

    @Override
    protected AABB createRenderBoundingBox() {
        var axis = getBlockState().getValue(LargeInductionRotorBlock.AXIS);
        return new AABB(worldPosition)
                .inflate(axis == Direction.Axis.X ? 0 : 1,
                        axis == Direction.Axis.Y ? 0 : 1,
                        axis == Direction.Axis.Z ? 0 : 1);
    }

    private class LazyWindingSupplier implements StampedSupplier<Precalculated<Float>> {
        private static final Precalculated<Float> ZERO = new Precalculated<>(0.0f) {
            @Override
            public Float get() {
                return 0f;
            }

            @Override
            public int getStamp() {
                return -1;
            }

            @Override
            public void invalidate() {

            }
        };

        private final BlockPos pos;
        private final Direction.Axis expectedAxis;

        public LazyWindingSupplier(BlockPos pos, Direction.Axis expectedAxis) {
            this.pos = pos;
            this.expectedAxis = expectedAxis;
        }

        @Nullable
        private WindingBlockEntity getWinding() {
            assert level != null;
            var otherState = level.getBlockState(pos);
            if(otherState.getBlock() instanceof WindingBlock winding) {
                var magnetic = winding.getMagneticAxis(otherState);
                if(magnetic != expectedAxis)
                    return null;
                var be = level.getBlockEntity(pos);
                return be instanceof WindingBlockEntity wbe ? wbe : null;
            }
            return null;
        }

        @Override
        public Precalculated<Float> get() {
            var winding = getWinding();
            return winding == null ? null : winding.fieldStrengthCalc();
        }
    }
}
