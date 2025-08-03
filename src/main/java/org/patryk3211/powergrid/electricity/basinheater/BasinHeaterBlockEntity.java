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

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class BasinHeaterBlockEntity extends ElectricBlockEntity {
    private ElectricWire coil;
    private BlazeBurnerBlock.HeatLevel state;

    public BasinHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.state = state.get(BasinHeaterBlock.HEAT_LEVEL);
    }

    public void setState(BlazeBurnerBlock.HeatLevel newState) {
        assert world != null;
        if(state != newState) {
            state = newState;
            world.setBlockState(pos, getCachedState().with(BasinHeaterBlock.HEAT_LEVEL, state));
        }
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var I = ModdedConfigs.server().electricity.basinHeaterCurrent.get();
        var power = I * I * BasinHeaterBlock.resistance();
        float factor = (float) (power / (600 - 22));
        return new ThermalBehaviour(this, 2.0f, factor, 1400);
    }

    @Override
    public void tick() {
        super.tick();
        applyLostPower(coil.power());
        var T = thermalBehaviour.getTemperature();
        thermalBehaviour.setDissipationFactor(0.005625f * T - 1.625f);
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
        coil = builder.connect(BasinHeaterBlock.resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }
}
