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

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import java.util.List;
import java.util.Optional;

public class TransformerMediumBlockEntity extends TransformerBlockEntity {
    public TransformerMediumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isMain() {
        return getBlockState().getValue(TransformerMediumBlock.PART) == 0;
    }

    public Optional<TransformerMediumBlockEntity> getMain() {
        if(isMain())
            return Optional.of(this);
        return ((TransformerMediumBlock) getBlockState().getBlock()).getBlockEntity(level, worldPosition, getBlockState())
                .map(be -> (TransformerMediumBlockEntity) be);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var b = super.specifyThermalBehaviour();
        if(b != null) {
            if (isMain()) {
                b.overheatCallback(() -> {
                    // Destroy all 4 blocks of the transformer
                    var axis = getBlockState().getValue(TransformerMediumBlock.HORIZONTAL_AXIS);
                    assert level != null;
                    level.destroyBlock(worldPosition, false);
                    level.destroyBlock(worldPosition.relative(axis, 1), false);
                    level.destroyBlock(worldPosition.above(), false);
                    level.destroyBlock(worldPosition.relative(axis, 1).above(), false);
                });
            } else {
                b.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
            }
        }
        return b;
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

    @Override
    public void tick() {
        if(isMain()) {
            super.tick();
        } else if(thermalBehaviour != null) {
            getMain().ifPresent(be -> thermalBehaviour.track(be.thermalBehaviour));
            thermalBehaviour.tick();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if(isMain()) {
            super.addBehaviours(behaviours);
        } else {
            thermalBehaviour = specifyThermalBehaviour();
            if(thermalBehaviour != null) {
                behaviours.add(thermalBehaviour);
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(isMain())
            return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return getMain().map(be -> be.addToGoggleTooltip(tooltip, isPlayerSneaking)).orElse(false);
    }
}
