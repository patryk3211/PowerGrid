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
package org.patryk3211.powergrid.electricity.heater;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.node.FloatingNode;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.Collection;
import java.util.List;

public class HeaterBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    public enum State {
        COLD,
        SMOKING,
        BLASTING
    }

    private IElectricNode node1;
    private IElectricNode node2;
    private ElectricWire wire;
    private State state;

    public HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = State.COLD;
    }

    @Override
    public void tick() {
        super.tick();
        float voltage = wire.potentialDifference();
        float power = voltage * voltage / HeaterBlock.resistance();
        applyLostPower(power);

        var temperature = thermalBehaviour.getTemperature();
        if(temperature < 200f) {
            updateState(State.COLD);
        } else if(temperature < 400f) {
            updateState(State.SMOKING);
        } else {
            updateState(State.BLASTING);
        }
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return new ThermalBehaviour(this, 1.0f, 0.3f, 550f);
    }

    private void updateState(State state) {
        assert world != null;
        if(this.state != state) {
            this.state = state;
            world.updateNeighbors(pos, getCachedState().getBlock());
        }
    }

    public State getState() {
        return state;
    }

    @Override
    public void initializeNodes() {
        node1 = new FloatingNode();
        node2 = new FloatingNode();
        wire = new ElectricWire(HeaterBlock.resistance(), node1, node2);
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

    protected Formatting temperatureColor(float value) {
        if(value < 200f)
            return Formatting.DARK_GRAY;
        else if(value < 400f)
            return Formatting.GREEN;
        else if(value < 500f)
            return Formatting.YELLOW;
        else
            return Formatting.RED;
    }

    @Override
    public boolean addToGoggleTooltip(List<Text> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.heater.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.heater.title")
                .style(Formatting.GRAY)
                .forGoggles(tooltip);

        var temperature = thermalBehaviour.getTemperature();
        temperature = Math.round(temperature * 100f) / 100f;
        var temperatureText = String.format("%.2f", temperature);
        Lang.builder()
                .text(temperatureText)
                .add(Text.of(" "))
                .add(Unit.TEMPERATURE.get())
                .style(temperatureColor(temperature))
                .forGoggles(tooltip, 1);

        return true;
    }
}
