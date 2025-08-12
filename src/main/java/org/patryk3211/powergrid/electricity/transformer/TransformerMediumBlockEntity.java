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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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
        var state = level.getBlockState(pos);
        level.setBlockAndUpdate(pos, state.setValue(TransformerBlock.COILS, coils));
    }

    @Override
    public void updateCoilBlockState() {
        int coilCount = secondaryCoil.isDefined() ? 2 : primaryCoil.isDefined() ? 1 : 0;

        var axis = getBlockState().getValue(TransformerMediumBlock.HORIZONTAL_AXIS);
        updateState(worldPosition, coilCount);
        updateState(worldPosition.relative(axis, 1), coilCount);
        updateState(worldPosition.relative(Direction.Axis.Y, 1), coilCount);
        updateState(worldPosition.relative(axis, 1).relative(Direction.Axis.Y, 1), coilCount);
    }
}
