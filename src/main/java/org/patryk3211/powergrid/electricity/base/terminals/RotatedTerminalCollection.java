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
package org.patryk3211.powergrid.electricity.base.terminals;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RotatedTerminalCollection {
    private final TerminalBoundingBox[][] terminals;

    private RotatedTerminalCollection(TerminalBoundingBox[][] terminals) {
        this.terminals = terminals;
    }

    public TerminalBoundingBox get(Direction dir, int index) {
        return terminals[dir.ordinal()][index];
    }

    public static class Builder extends TerminalCollectionBuilder<Builder, RotatedTerminalCollection> {
        private final ITerminalRotator rotator;
        private final List<Direction> directions = new ArrayList<>();

        private Builder(ITerminalRotator rotator) {
            this.rotator = rotator;
        }

        public Builder with(Direction direction) {
            directions.add(direction);
            return this;
        }

        public Builder with(Direction... directions) {
            Collections.addAll(this.directions, directions);
            return this;
        }

        @Override
        public RotatedTerminalCollection build() {
            var terminals = new TerminalBoundingBox[6][this.terminals.size()];
            for(var dir : directions) {
                for(int i = 0; i < this.terminals.size(); ++i) {
                    terminals[dir.ordinal()][i] = rotator.rotate(this.terminals.get(i), dir);
                }
            }
            return new RotatedTerminalCollection(terminals);
        }
    }

    public static Builder builder(ITerminalRotator rotator) {
        return new Builder(rotator);
    }

    public interface ITerminalRotator {
        TerminalBoundingBox rotate(TerminalBoundingBox terminal, Direction facing);
    }

    public static TerminalBoundingBox rotateNorthToFacing(TerminalBoundingBox terminal, Direction facing) {
        return switch(facing) {
            case NORTH -> terminal;
            case SOUTH -> terminal.rotateAroundY(BlockRotation.CLOCKWISE_180);
            case EAST -> terminal.rotateAroundY(BlockRotation.CLOCKWISE_90);
            case WEST -> terminal.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
            case UP -> terminal.rotateAroundX(BlockRotation.COUNTERCLOCKWISE_90);
            case DOWN -> terminal.rotateAroundX(BlockRotation.CLOCKWISE_90);
        };
    }
}
