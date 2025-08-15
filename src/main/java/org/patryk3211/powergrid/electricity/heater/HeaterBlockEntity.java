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

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;

import java.util.List;

public class HeaterBlockEntity extends ElectricBlockEntity implements IHaveGoggleInformation {
    public enum State {
        COLD,
        SMOKING,
        BLASTING
    }

    private ElectricWire wire;
    private State state;

    public HeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = State.COLD;
    }

    @Override
    public void tick() {
        super.tick();
        if(thermalBehaviour == null) {
            PowerGrid.LOGGER.warn("Heating coil should always have a thermal behaviour");
            return;
        }
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
        return ThermalBehaviour.always(this, 1.5f, 0.1f, 600f);
    }

    private void updateState(State state) {
        assert level != null;
        if(this.state != state) {
            this.state = state;
            level.blockUpdated(worldPosition, getBlockState().getBlock());
        }
    }

    public State getState() {
        return state;
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        var node1 = builder.addExternalNode();
        var node2 = builder.addExternalNode();
        wire = builder.connect(HeaterBlock.resistance(), node1, node2);
    }

    protected ChatFormatting temperatureColor(float value) {
        if(value < 200f)
            return ChatFormatting.DARK_GRAY;
        else if(value < 400f)
            return ChatFormatting.GREEN;
        else if(value < 550f)
            return ChatFormatting.YELLOW;
        else
            return ChatFormatting.RED;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Lang.translate("gui.heater.info_header").forGoggles(tooltip);
        Lang.builder().translate("gui.heater.title")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        var temperature = thermalBehaviour.getTemperature();
        temperature = Math.round(temperature * 100f) / 100f;
        var temperatureText = String.format("%.2f", temperature);
        Lang.builder()
                .text(temperatureText)
                .add(Component.nullToEmpty(" "))
                .add(Unit.TEMPERATURE.get())
                .style(temperatureColor(temperature))
                .forGoggles(tooltip, 1);

        return true;
    }
}
