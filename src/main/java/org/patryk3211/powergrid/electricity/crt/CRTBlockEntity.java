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
package org.patryk3211.powergrid.electricity.crt;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class CRTBlockEntity extends ElectricBlockEntity {
    public static final int SAMPLE_COUNT = 80;

    private ElectricWire gun, xDeflect, yDeflect;

    // These are only needed on the client. CRT state is not persistent nor synchronized.
    protected float[] xPoints = new float[SAMPLE_COUNT];
    protected float[] yPoints = new float[SAMPLE_COUNT];
    protected float[] brightness = new float[SAMPLE_COUNT];
    protected int head;

    public CRTBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    int tick = 0;
    @Override
    public void tick() {
        super.tick();
        if(level.isClientSide) {
            brightness[head] = Mth.clamp(gun.current() / 0.1f, 0, 1);
            xPoints[head] = xDeflect.current() / 0.5f;
            yPoints[head] = yDeflect.current() / 0.5f;
            head = (head + 1) % SAMPLE_COUNT;
        }
    }

    @Override
    public void buildCircuit(CircuitBuilder builder) {
        builder.setTerminalCount(6);
        gun = builder.connect(resistance("gun"), builder.terminalNode(0), builder.terminalNode(1));
        xDeflect = builder.connect(resistance("coils"), builder.terminalNode(2), builder.terminalNode(3));
        yDeflect = builder.connect(resistance("coils"), builder.terminalNode(4), builder.terminalNode(5));
    }
}
