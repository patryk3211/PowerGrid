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
package org.patryk3211.powergrid.electricity.basinheater;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class BasinHeaterBlockEntity extends ElectricBlockEntity {
    private ElectricWire coil;
    private BlazeBurnerBlock.HeatLevel state;

    public BasinHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state.getValue(BasinHeaterBlock.HEAT_LEVEL);
    }

    public void setState(BlazeBurnerBlock.HeatLevel newState) {
        assert level != null;
        if(state != newState) {
            state = newState;
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BasinHeaterBlock.HEAT_LEVEL, state));
        }
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var I = ModdedConfigs.server().electricity.basinHeaterCurrent.get();
        var power = I * I * resistance("idle");
        float factor = (float) (power / (600 - 22));
        return ThermalBehaviour.always(this, 2.0f, factor, 1400);
    }

    public boolean mixerRunning() {
        var be = level.getBlockEntity(worldPosition.above(3));
        if(!(be instanceof MechanicalMixerBlockEntity mixer))
            return false;
        return mixer.running;
    }

    @Override
    public void electricalTick() {
        applyPower(coil);
        if(thermalBehaviour == null) {
            PowerGrid.LOGGER.warn("Basin heater should always have a thermal behaviour");
            return;
        }
        coil.setResistance(mixerRunning() ? resistance("mixing") : resistance("idle"));
        var T = thermalBehaviour.getTemperature();
        var dissipation = 0.005625f * T - 1.625f;
        if(mixerRunning()) {
            var I = ModdedConfigs.server().electricity.basinHeaterCurrent.get() * 2;
            var power = I * I * (resistance("idle") - resistance("mixing"));
            dissipation += (float) (power / (1000 - 22));
        }
        thermalBehaviour.setDissipationFactor(dissipation);
        if(T < 600) {
            setState(BlazeBurnerBlock.HeatLevel.NONE);
        } else if(T < 1000) {
            setState(BlazeBurnerBlock.HeatLevel.KINDLED);
        } else {
            setState(BlazeBurnerBlock.HeatLevel.SEETHING);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        coil = builder.connect(resistance("idle"), builder.terminalNode(0), builder.terminalNode(1));
    }
}
