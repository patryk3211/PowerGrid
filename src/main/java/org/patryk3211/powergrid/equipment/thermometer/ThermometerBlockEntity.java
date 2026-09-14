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
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class ThermometerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public float maxState;
    public float maxTemperature;

    public float dialTarget;
    public float prevDialState;
    public float dialState;

    public int redstoneOutput;

    public ThermometerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public float temperature() {
        var facing = getBlockState().getValue(ThermometerBlock.FACING);
        var thermal = BlockEntityBehaviour.get(level, worldPosition.relative(facing), AThermalBehaviour.TYPE);
        if(thermal != null) {
            return thermal.getTemperature();
        }
        return AThermalBehaviour.getAmbientTemperature(level, worldPosition);
    }

    @Override
    public void tick() {
        assert level != null;
        super.tick();
        var temperature = temperature();
        if(temperature > maxTemperature)
            maxTemperature = temperature;
        dialTarget = Mth.clamp((temperature - 22f) / (175 - 22), 0, 1.125f);
        if(!Float.isNaN(dialTarget)) {
            prevDialState = dialState;

            dialState += (dialTarget - dialState) * .125f;
            if (dialState > 1 && level.random.nextFloat() < 1 / 2f)
                dialState -= (dialState - 1) * level.random.nextFloat();
            if(maxState < dialState) {
                maxState = dialState;
                level.blockEntityChanged(worldPosition);
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
        maxTemperature = temperature();
        setChanged();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("Max", maxTemperature);
        tag.putFloat("MaxState", maxState);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        maxTemperature = tag.getFloat("Max");
        maxState = tag.getFloat("MaxState");
    }

    private ChatFormatting color(float temperature) {
        var color = ChatFormatting.GREEN;
        if(temperature > 150) {
            color = ChatFormatting.RED;
        } else if(temperature > 125) {
            color = ChatFormatting.YELLOW;
        }
        return color;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.translate("gui.gauge.info_header").forGoggles(tooltip);

        Lang.translate("gui.thermometer.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var temperature = temperature();
        var temperatureText = Lang.numberConstant(temperature);
        if(temperature > 175) {
            temperatureText = Lang.text(">175.0");
        }
        temperatureText
                .add(Component.literal(" "))
                .add(Unit.TEMPERATURE.get())
                .style(color(temperature))
                .forGoggles(tooltip);

        Lang.translate("gui.thermometer.max_title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var maxTemperatureText = Lang.numberConstant(maxTemperature);
        if(maxTemperature > 175) {
            maxTemperatureText = Lang.text(">175.0");
        }
        maxTemperatureText
                .add(Component.literal(" "))
                .add(Unit.TEMPERATURE.get())
                .style(color(maxTemperature))
                .forGoggles(tooltip);

        Lang.translate("gui.thermometer.click")
                .style(ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip);
        return true;
    }
}
