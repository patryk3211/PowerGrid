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
package org.patryk3211.powergrid.electricity.battery;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;

public class PotatoBatteryBlockEntity extends BatteryBlockEntity {
    public PotatoBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        var b = ThermalBehaviour.simple(this, 0.2f, 0.01f, 100f);
        if(b != null)
            b.behaviourFlags(ThermalBehaviour.OVERHEAT_PARTICLES);
        return b;
    }

    @Override
    public float calculatePower() {
        // No recharging
        return Math.max(super.calculatePower(), 0);
    }

    @Override
    public void tick() {
        if(getBlockState().getValue(PotatoBatteryBlock.BAKED)) {
            sourceNode.setVoltage(0);
            energy = 0;
            return;
        }
        super.tick();
        if(thermalBehaviour != null && thermalBehaviour.isOverheated() && !level.isClientSide) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PotatoBatteryBlock.BAKED, true));
            notifyUpdate();
        }
    }
}
