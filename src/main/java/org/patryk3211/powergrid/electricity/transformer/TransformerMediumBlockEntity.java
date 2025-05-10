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
package org.patryk3211.powergrid.electricity.transformer;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

public class TransformerMediumBlockEntity extends TransformerBlockEntity {
    public TransformerMediumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 8.0f, 2.5f);
    }

    private void updateState(BlockPos pos, int coils) {
        var state = world.getBlockState(pos);
        world.setBlockState(pos, state.with(TransformerBlock.COILS, coils));
    }

    @Override
    public void updateCoilBlockState() {
        int coilCount = secondaryCoil.isDefined() ? 2 : primaryCoil.isDefined() ? 1 : 0;

        var axis = getCachedState().get(TransformerMediumBlock.HORIZONTAL_AXIS);
        updateState(pos, coilCount);
        updateState(pos.offset(axis, 1), coilCount);
        updateState(pos.offset(Direction.Axis.Y, 1), coilCount);
        updateState(pos.offset(axis, 1).offset(Direction.Axis.Y, 1), coilCount);
    }
}
