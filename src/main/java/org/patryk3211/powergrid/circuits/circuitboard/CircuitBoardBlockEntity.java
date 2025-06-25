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
package org.patryk3211.powergrid.circuits.circuitboard;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectric;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;

public class CircuitBoardBlockEntity extends ElectricBlockEntity implements IElectric {
    private CircuitSchematic schematic = new CircuitSchematic();
    private BakedCircuit baked;

    public CircuitBoardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void withSchematic(CircuitSchematic schematic) {
        this.schematic = new CircuitSchematic(schematic);
        bakeCircuit();
        notifyUpdate();
    }

    private void bakeCircuit() {
        baked = BakedCircuit.from(schematic);
        electricBehaviour.rebuildCircuit();
    }

    @Override
    protected void write(NbtCompound tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Schematic", schematic.serializeNbt());
    }

    @Override
    protected void read(NbtCompound tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        schematic.deserializeNbt(tag.getCompound("Schematic"));
        if(baked == null || tag.getBoolean("Rebuild")) {
            bakeCircuit();
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        if(baked != null)
            builder.setTo(baked);
    }

    @Override
    public int terminalCount() {
        return baked == null ? 0 : baked.externalNodes.size();
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        // TODO: Apply block state
        return baked == null ? null : baked.terminals.get(index);
    }

    public CircuitSchematic getSchematic() {
        return schematic;
    }
}
