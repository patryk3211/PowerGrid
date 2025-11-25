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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.AbstractElectricWire;
import org.patryk3211.powergrid.utility.sound.SoundScapes;

import java.util.List;

public abstract class ElectricBlockEntity extends SmartBlockEntity implements IElectricEntity {
    protected ElectricBehaviour electricBehaviour;
    @Nullable
    protected ThermalBehaviour thermalBehaviour;

    private float power;

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        electricBehaviour = new ElectricBehaviour(this);
        behaviours.add(electricBehaviour);

        thermalBehaviour = specifyThermalBehaviour();
        if(thermalBehaviour != null)
            behaviours.add(thermalBehaviour);
    }

    @Nullable
    public ThermalBehaviour specifyThermalBehaviour() {
        return null;
    }

    protected void applyPower(AbstractElectricWire wire) {
        power += wire.power();
        if(thermalBehaviour != null)
            thermalBehaviour.applyWirePower(wire);
    }

    @Override
    public void tick() {
        if(!level.isClientSide || isVirtual())
            electricalTick();
        super.tick();
        if(level.isClientSide) {
            tickAudio();
        }
        power = 0;
    }

    public void electricalTick() {

    }

    public boolean isNoisy() {
        return true;
    }

    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        if(!isNoisy() || thermalBehaviour == null)
            return;
        var percent = power / thermalBehaviour.maxPower();
        SoundScapes.play(SoundScapes.AmbienceGroup.HUM, worldPosition, 1, percent);
    }

    public ElectricBehaviour getElectricBehaviour() {
        return electricBehaviour;
    }

    @Override
    public void remove() {
        super.remove();
        if(electricBehaviour != null) {
            electricBehaviour.remove();
        }
    }
}
