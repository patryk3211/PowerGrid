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
package org.patryk3211.powergrid.electricity.light.fixture;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import static net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE;

public class LightFixtureBlockEntity extends AbstractLightFixtureBlockEntity {
    private SwitchedWire filament;

    public LightFixtureBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, true);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        filament = builder.connectSwitch(1, builder.terminalNode(0), builder.terminalNode(1), false);
    }

    @Override
    public void electricalTick() {
        super.electricalTick();
        if(bulbState != null)
            bulbState.runSpecialEffects(level, worldPosition, getBlockState().getValue(LightFixtureBlock.FACING));
    }

    public SwitchedWire getFilament() {
        return filament;
    }

    @Override
    public void setPowerLevel(int bulbPower) {
        level.setBlock(worldPosition, getBlockState().setValue(LightFixtureBlock.POWER, bulbPower), UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public int getPowerLevel() {
        return getBlockState().getValue(LightFixtureBlock.POWER);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state) {
        if(bulbState != null)
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, bulbState.getItem());
        return ItemRequirement.NONE;
    }

    public ItemInteractionResult setColor(DyeColor color) {
        if(bulbState == null)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(bulbState.setColor(color)) {
            notifyUpdate();
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
