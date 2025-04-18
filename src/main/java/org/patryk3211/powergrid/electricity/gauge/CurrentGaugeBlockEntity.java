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
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;

import java.util.Collection;
import java.util.List;

public class CurrentGaugeBlockEntity extends GaugeBlockEntity {
    private IElectricNode node1;
    private IElectricNode node2;
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
        super.tick();
    }

    @Override
    public void initializeNodes() {
        float resistance = ((CurrentGaugeBlock) getCachedState().getBlock()).getResistance();
        node1 = new FloatingNode();
        node2 = new FloatingNode();
        wire = new ElectricWire(resistance, node1, node2);
    }

    @Override
    public void addExternalNodes(List<IElectricNode> nodes) {
        nodes.add(node1);
        nodes.add(node2);
    }

    @Override
    public void addInternalWires(Collection<ElectricWire> wires) {
        wires.add(wire);
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

        var unit = Lang.builder().translate("generic.unit.amp");
        Lang.builder()
                .text(currentText)
                .add(unit)
                .style(measurementColor(Math.abs(current)))
                .forGoggles(tooltip, 1);

        return true;
    }
}
