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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
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
    public double coreAl() {
        return ModdedConfigs.server().electricity.mediumCoreAl.get();
    }

    @Override
    public double couplingFactor() {
        return ModdedConfigs.server().electricity.mediumCoreK.get();
    }

    @Override
    public @Nullable AThermalBehaviour specifyThermalBehaviour() {
        var b = super.specifyThermalBehaviour();
        if(b != null && b instanceof ThermalBehaviour thermal) {
            if (isMain()) {
                thermal.overheatCallback(() -> {
                    // Destroy all 4 blocks of the transformer
                    var axis = getBlockState().getValue(TransformerMediumBlock.HORIZONTAL_AXIS);
                    assert level != null;
                    level.destroyBlock(worldPosition, false);
                    level.destroyBlock(worldPosition.relative(axis, 1), false);
                    level.destroyBlock(worldPosition.above(), false);
                    level.destroyBlock(worldPosition.relative(axis, 1).above(), false);
                });
            } else {
                thermal.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
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
        } else if(thermalBehaviour != null && thermalBehaviour instanceof ThermalBehaviour thermal) {
            getMain().ifPresent(be -> thermal.track((ThermalBehaviour)be.thermalBehaviour));
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
    public ITerminalPlacement connectedTerminal(BlockState state, int index) {
        if(isMain())
            return super.connectedTerminal(state, index);
        return getMain().map(be -> be.connectedTerminal(state, index)).orElse(null);
    }

    @Override
    protected int connectedTerminalIndex(int index) {
        if(isMain())
            return super.connectedTerminalIndex(index);
        return getMain().map(be -> be.connectedTerminalIndex(index)).orElse(-1);
    }

    @Override
    public @NotNull BlockPos connectedTerminalPosition(BlockPos pos, BlockState state, int index) {
        int part = state.getValue(TransformerMediumBlock.PART);
        var axis = state.getValue(TransformerMediumBlock.HORIZONTAL_AXIS);
        int offset = switch(connectedTerminalIndex(index)) {
            case 0, 1 -> part == 2 ? 0 : -1;
            case 2, 3 -> part == 3 ? 0 :  1;
            default -> 0;
        };
        return pos.relative(axis, offset);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if(isMain())
            return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return getMain().map(be -> be.addToGoggleTooltip(tooltip, isPlayerSneaking)).orElse(false);
    }
}
