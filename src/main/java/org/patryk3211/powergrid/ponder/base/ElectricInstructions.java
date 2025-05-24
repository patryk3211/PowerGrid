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
package org.patryk3211.powergrid.ponder.base;

import com.simibubi.create.foundation.ponder.ElementLink;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.instruction.FadeOutOfSceneInstruction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class ElectricInstructions {
    public static final float DEFAULT_RESISTANCE = 0.005f;

    private final SceneBuilder builder;

    public ElectricInstructions(SceneBuilder builder) {
        this.builder = builder;
    }

    public static ElectricInstructions of(SceneBuilder builder) {
        return new ElectricInstructions(builder);
    }

    public void tickFor(int ticks) {
        builder.addInstruction(new ElectricityTickInstruction(ticks));
    }

    public ElementLink<WireElement> connect(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, float resistance) {
        var link = new ElementLink<>(WireElement.class);
        var element = new WireElement(pos1, terminal1, pos2, terminal2, resistance);
        builder.addInstruction(new CreateWireInstruction(15, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connect(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2) {
        return connect(pos1, terminal1, pos2, terminal2, DEFAULT_RESISTANCE);
    }

    public ElementLink<WireElement> connectInvisible(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2, float resistance) {
        var link = new ElementLink<>(WireElement.class);
        var element = new InvisibleWireElement(pos1, terminal1, pos2, terminal2, resistance);
        builder.addInstruction(new CreateWireInstruction(0, Direction.DOWN, element));
        builder.addInstruction(ponder -> ponder.linkElement(element, link));
        return link;
    }

    public ElementLink<WireElement> connectInvisible(BlockPos pos1, int terminal1, BlockPos pos2, int terminal2) {
        return connectInvisible(pos1, terminal1, pos2, terminal2, DEFAULT_RESISTANCE);
    }

    public void removeWire(ElementLink<WireElement> wire) {
        builder.addInstruction(new FadeOutOfSceneInstruction<>(15, Direction.UP, wire));
    }

    public void setSource(BlockPos sourcePos, float value) {
        builder.addInstruction(new SetSourceInstruction(sourcePos, value));
    }

    public void unload() {
        builder.addInstruction(new UnloadElectricityWorldInstruction());
    }
}
