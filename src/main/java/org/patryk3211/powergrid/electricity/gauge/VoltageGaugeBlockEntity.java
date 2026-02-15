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
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class VoltageGaugeBlockEntity extends GaugeBlockEntity {
    private static final float[] MAX_VALUES = new float[] { 2, 20, 200, 2000 };

    private IElectricNode node1;
    private IElectricNode node2;

    public VoltageGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        maxValue = MAX_VALUES[0];
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        gaugeValue = new GaugeValueBehaviour(Component.translatable("powergrid.devices.gauge.voltage"),
                Unit.VOLTAGE.get().component(), MAX_VALUES, this, new BoxTransform());
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
    public void tick() {
        var potential = Math.abs(getValue());
        if(potential > maxValue) {
            dialTarget = 1.125f;
        } else {
            dialTarget = potential / maxValue;
        }
        super.tick();
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        // 20 kilo-ohm "impedance".
        builder.setTerminalCount(2);
        node1 = builder.terminalNode(0);
        node2 = builder.terminalNode(1);
        builder.connect(20e3f, node1, node2);
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
        return node1.getVoltage() - node2.getVoltage();
    }

    @Override
    public Unit getUnit() {
        return Unit.VOLTAGE;
    }

    public static ChatFormatting measurementColor(float value, float maxValue) {
        if(value < maxValue * 0.01)
            return ChatFormatting.DARK_GRAY;
        else if(value < maxValue * 0.5)
            return ChatFormatting.GREEN;
        else if(value < maxValue * 0.75)
            return ChatFormatting.BLUE;
        else
            return ChatFormatting.LIGHT_PURPLE;
    }

    public static void addTooltip(List<Component> tooltip, float potential, float maxValue) {
        Lang.builder().translate("gui.voltage_meter.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        potential = Math.round(potential * 100f) / 100f;
        var voltageText = String.format("%.2f", potential);
        if(potential >= 0) {
            voltageText = " " + voltageText;
        }
        if(Math.abs(potential) > maxValue) {
            if(potential > 0)
                voltageText = String.format("> %.2f", maxValue);
            else
                voltageText = String.format("< %.2f", -maxValue);
        }

        Lang.builder()
                .text(voltageText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.VOLTAGE.get())
                .style(measurementColor(Math.abs(potential), maxValue))
                .forGoggles(tooltip, 1);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Lang.builder().translate("gui.voltage_meter.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        display.format(getValue()).forGoggles(tooltip, 1);
        return true;
    }
}
