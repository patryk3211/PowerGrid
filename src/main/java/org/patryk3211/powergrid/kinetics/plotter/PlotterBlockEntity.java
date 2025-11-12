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
package org.patryk3211.powergrid.kinetics.plotter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlockEntity;

public class PlotterBlockEntity extends ElectricKineticBlockEntity {
    private ElectricWire wire;
    protected final float[] sampleBuffer = new float[40];
    protected int head;

    public PlotterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        // 20 kilo-ohm "impedance".
        builder.setTerminalCount(2);
        wire = builder.connect(20e3f, builder.terminalNode(0), builder.terminalNode(1));
    }

    int tick = 0;
    @Override
    public void tick() {
        super.tick();
        sampleBuffer[head] = (float) Math.sin(tick++ / 20f * Math.PI); //wire.potentialDifference();
        head = (head + 1) % sampleBuffer.length;
    }
}
