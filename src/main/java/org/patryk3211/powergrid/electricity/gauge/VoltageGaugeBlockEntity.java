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

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class VoltageGaugeBlockEntity extends GaugeBlockEntity {
    private IElectricNode node1;
    private IElectricNode node2;

    public VoltageGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    public float getValue() {
        return node1.getVoltage() - node2.getVoltage();
    }

    @Override
    public Unit getUnit() {
        return Unit.VOLTAGE;
    }

    protected ChatFormatting measurementColor(float value) {
        if(value < maxValue * 0.01)
            return ChatFormatting.DARK_GRAY;
        else if(value < maxValue * 0.5)
            return ChatFormatting.GREEN;
        else if(value < maxValue * 0.75)
            return ChatFormatting.BLUE;
        else
            return ChatFormatting.LIGHT_PURPLE;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        Lang.builder().translate("gui.voltage_meter.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var potential = getValue();
        potential = Math.round(potential * 100f) / 100f;
        var voltageText = String.format("%.2f", potential);
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
                .style(measurementColor(Math.abs(potential)))
                .forGoggles(tooltip, 1);

        return true;
    }
}
