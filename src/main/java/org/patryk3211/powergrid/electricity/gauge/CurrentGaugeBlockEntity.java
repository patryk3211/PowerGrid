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

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class CurrentGaugeBlockEntity extends GaugeBlockEntity {
    private ElectricWire wire;

    public CurrentGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        var current = Math.abs(getValue());
        if(current > maxValue) {
            dialTarget = 1.125f;
        } else {
            dialTarget = current / maxValue;
        }

        applyLostPower(wire.power());
        super.tick();
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 5.0f, 0.1f);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        float resistance = ((CurrentGaugeBlock) getCachedState().getBlock()).getResistance();
        var node1 = builder.addExternalNode();
        var node2 = builder.addExternalNode();
        wire = builder.connect(resistance, node1, node2);
    }

    @Override
    public float getValue() {
        return wire.current();
    }

    protected Formatting measurementColor(float value) {
        if(value < maxValue * 0.01)
            return Formatting.DARK_GRAY;
        else if(value < maxValue * 0.5)
            return Formatting.GREEN;
        else if(value < maxValue * 0.75)
            return Formatting.YELLOW;
        else
            return Formatting.RED;
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        Lang.builder().translate("gui.current_meter.title")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var current = getValue();
        current = Math.round(current * 100f) / 100f;
        var currentText = String.format("%.2f", current);
        if(Math.abs(current) > maxValue) {
            if(current > 0)
                currentText = String.format("> %.2f", maxValue);
            else
                currentText = String.format("< %.2f", -maxValue);
        }

        Lang.builder()
                .text(currentText)
                .add(Text.of(" "))
                .add(Unit.CURRENT.get())
                .style(measurementColor(Math.abs(current)))
                .forGoggles(tooltip, 1);

        return true;
    }
}
