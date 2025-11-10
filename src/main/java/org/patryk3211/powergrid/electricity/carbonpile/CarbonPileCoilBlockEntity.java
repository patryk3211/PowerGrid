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
package org.patryk3211.powergrid.electricity.carbonpile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.config.ResistanceValues;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class CarbonPileCoilBlockEntity extends ElectricBlockEntity {
    private ElectricWire coil;
    private SwitchedWire pile;
    private float baseResistance;
    private float trim = 1;
    private float coilPull = 0;

    public CarbonPileCoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(4);
        coil = builder.connect(resistance(), builder.terminalNode(0), builder.terminalNode(1));
        // Resistance values won't be valid here
        pile = builder.connectSwitch(1, builder.terminalNode(2), builder.terminalNode(3), false);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        baseResistance = tag.getFloat("Base");
        trim = tag.getFloat("Trim");
        coilPull = tag.getFloat("Coil");
        refreshResistance();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("Base", baseResistance);
        tag.putFloat("Trim", trim);
        tag.putFloat("Coil", coilPull);
    }

    public void refreshResistance() {
        var R = baseResistance * trim * (1 + coilPull);
        pile.setState(R > 0);
        if(R > 0) {
            // Max coil current makes the resistance twice as high
            pile.setResistance(R);
        }
    }

    public void pileChanged() {
        assert level != null;
        baseResistance = 0;
        var pos = worldPosition.above();
        BlockState state;
        while(ModdedBlocks.CARBON_PILE.has(state = level.getBlockState(pos))) {
            baseResistance += ResistanceValues.get(state.getBlock());
            pos = pos.above();
        }
        if(baseResistance == 0) {
            pile.setState(false);
        }
        trim = 1.0f;
        refreshResistance();
        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        coilPull = Mth.clamp(Math.abs(coil.current()), 0, 1);
        refreshResistance();
        setChanged();
    }
}
