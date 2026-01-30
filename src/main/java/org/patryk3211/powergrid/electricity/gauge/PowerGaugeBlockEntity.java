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
package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class PowerGaugeBlockEntity extends GaugeBlockEntity {
    private static final float[] MAX_VALUES = new float[] { 20, 200, 2000, 20000 };
    private ElectricWire series;
    private ElectricWire shunt;

    public PowerGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxValue = MAX_VALUES[0];
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        gaugeValue = new GaugeValueBehaviour(Component.translatable("powergrid.devices.gauge.power"),
                Unit.POWER.get().component(), MAX_VALUES, this, new BoxTransform());
        gaugeValue.withCallback(i -> {
            maxValue = MAX_VALUES[i];
            sendData();
        });
        behaviours.add(gaugeValue);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        maxValue = MAX_VALUES[gaugeValue.getValue()];
    }

    @Override
    public void electricalTick() {
        applyPower(series);
    }

    @Override
    public void tick() {
        var current = Math.abs(getValue());
        if(current > maxValue) {
            dialTarget = 1.125f;
        } else {
            dialTarget = current / maxValue;
        }

        super.tick();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        float resistance = resistance();
        builder.setTerminalCount(3);
        series = builder.connect(resistance, builder.terminalNode(0), builder.terminalNode(1));
        shunt = builder.connect(20e3f, builder.terminalNode(0), builder.terminalNode(2));
    }

    @Override
    public float getMaxValue() {
        return maxValue;
    }

    @Override
    public ChatFormatting getColor(float value) {
        return measurementColor(value, maxValue);
    }

    @Override
    public float getValue() {
        return series.current() * shunt.potentialDifference();
    }

    @Override
    public Unit getUnit() {
        return Unit.POWER;
    }

    public static ChatFormatting measurementColor(float value, float maxValue) {
        if(value < maxValue * 0.01)
            return ChatFormatting.DARK_GRAY;
        else if(value < maxValue * 0.5)
            return ChatFormatting.DARK_AQUA;
        else if(value < maxValue * 0.75)
            return ChatFormatting.GOLD;
        else
            return ChatFormatting.RED;
    }

    public static void addTooltip(List<Component> tooltip, float power, float maxValue) {
        Lang.builder().translate("gui.power_meter.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        power = Math.round(power * 100f) / 100f;
        var powerText = String.format("%.2f", power);
        if(Math.abs(power) > maxValue) {
            if(power > 0)
                powerText = String.format("> %.2f", maxValue);
            else
                powerText = String.format("< %.2f", -maxValue);
        }

        Lang.builder()
                .text(powerText)
                .add(Component.literal(" "))
                .add(Unit.POWER.get())
                .style(measurementColor(Math.abs(power), maxValue))
                .forGoggles(tooltip, 1);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Lang.builder().translate("gui.power_meter.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        display.format(getValue()).forGoggles(tooltip, 1);
        return true;
    }
}
