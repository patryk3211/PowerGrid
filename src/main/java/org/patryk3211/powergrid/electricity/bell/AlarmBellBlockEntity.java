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
package org.patryk3211.powergrid.electricity.bell;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class AlarmBellBlockEntity extends ElectricBlockEntity {
    private ElectricWire wire;

    private boolean hasSoundInstance = false;

    public AlarmBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable ThermalBehaviour specifyThermalBehaviour() {
        return ThermalBehaviour.fromConfig(this);
    }

    @Environment(EnvType.CLIENT)
    public void tickAudio() {
        if(getVolume() > 0 && !hasSoundInstance) {
            Minecraft.getInstance().getSoundManager().play(new AlarmBellSoundInstance(this));
            hasSoundInstance = true;
        } else if(getVolume() == 0 && hasSoundInstance) {
            hasSoundInstance = false;
        }
    }

    @Override
    public void electricalTick() {
        applyPower(wire);
    }

    public float getVolume() {
        double I = Math.abs(wire.current());
        if(I < 0.25)
            return 0;
        return (float) (I * 2.0);
    }

    public float getPitch() {
        double I = Math.abs(wire.current());
        if(I < 0.5)
            return 0.75f;
        return (float) Math.min(0.5 + I * 0.5, 1.25);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }
}
