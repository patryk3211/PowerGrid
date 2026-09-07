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

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.AThermalBehaviour;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

import static org.patryk3211.powergrid.kinetics.motor.ElectricMotorBlockEntity.CONVERSION_CONSTANT;

public class BasinHeaterBlockEntity extends ElectricBlockEntity {
    private ElectricWire coil;
    private HeatLevel state;

    public BasinHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state.getValue(BasinHeaterBlock.HEAT_LEVEL);
    }

    public void setState(HeatLevel newState) {
        assert level != null;
        if(state != newState) {
            state = newState;
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(BasinHeaterBlock.HEAT_LEVEL, state));
        }
    }

    public static float power() {
        // 16384 SU is what the heater outputs if it's used to power a steam engine
        return 256 * 64 * ModdedConfigs.server().kinetics.torqueForStress.getF() / CONVERSION_CONSTANT;
    }

    public static float minPower() {
        float factor = power() / (600 - AThermalBehaviour.STANDARD_TEMPERATURE);
        return factor * (300 - AThermalBehaviour.STANDARD_TEMPERATURE);
    }

    @Override
    public @Nullable AThermalBehaviour specifyThermalBehaviour() {
        float factor = power() / (600 - AThermalBehaviour.STANDARD_TEMPERATURE);
        return ThermalBehaviour.always(this, 2.0f, factor, 1600);
    }

    @Override
    public void electricalTick() {
        applyPower(coil);
        if(thermalBehaviour == null) {
            PowerGrid.LOGGER.warn("Basin heater should always have a thermal behaviour");
            return;
        }
        var T = thermalBehaviour.getTemperature();
        if(T < 300) {
            setState(HeatLevel.NONE);
        } else if(T < 600) {
            setState(HeatLevel.FADING);
        } else if(T < 1200) {
            setState(HeatLevel.KINDLED);
        } else {
            setState(HeatLevel.SEETHING);
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        coil = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }

    public static int boilerHeater(Level level, BlockPos pos, BlockState state) {
        HeatLevel value = state.getValue(BasinHeaterBlock.HEAT_LEVEL);
        if (value == HeatLevel.SEETHING) {
            return 2;
        }
        if (value.isAtLeast(HeatLevel.KINDLED)) {
            return 1;
        }
        return BoilerHeater.NO_HEAT;
    }
}
