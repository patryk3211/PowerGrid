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
package org.patryk3211.powergrid.equipment.thermometer;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

import java.util.List;

public class ThermometerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public float maxState;

    public float dialTarget;
    public float prevDialState;
    public float dialState;

    public int redstoneOutput;

    public ThermometerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        var facing = getBlockState().getValue(ThermometerBlock.FACING);
        var thermal = BlockEntityBehaviour.get(level, worldPosition.relative(facing), ThermalBehaviour.TYPE);
        if(thermal != null) {
            dialTarget = (thermal.getTemperature() - 22f) / (175 - 22);
        }
        if(!Float.isNaN(dialTarget)) {
            prevDialState = dialState;
            dialState += (dialTarget - dialState) * .125f;
            if (dialState > 1 && level.random.nextFloat() < 1 / 2f)
                dialState -= (dialState - 1) * level.random.nextFloat();
            if(maxState < dialState) {
                maxState = dialState;
                setChanged();
            }
            var newOutput = Mth.floor(Mth.clamp(dialTarget * 15, 0, 15));
            if(newOutput != redstoneOutput) {
                redstoneOutput = newOutput;
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {

    }

    public void resetMax() {
        maxState = dialState;
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("Max", maxState);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        maxState = tag.getFloat("Max");
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {

        return true;
    }
}
