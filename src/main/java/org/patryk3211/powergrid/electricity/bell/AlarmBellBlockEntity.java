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
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class AlarmBellBlockEntity extends ElectricBlockEntity {
    private ElectricWire wire;

    private boolean hasSoundInstance = false;

    public AlarmBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    public void tick() {
        super.tick();
        if(level.isClientSide) {
            tickAudio();
        }
    }

    public float getVolume() {
        var I = Math.abs(wire.current());
        if(I < 0.25f)
            return 0;
        return I * 2.0f;
    }

    public float getPitch() {
        var I = Math.abs(wire.current());
        if(I < 0.5f)
            return 0.75f;
        return Math.min(0.5f + I * 0.5f, 1.25f);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(2);
        wire = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
    }
}
