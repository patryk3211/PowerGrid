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

public class AxisRotatedTerminalCollection {
    private final TerminalBoundingBox[][] terminals;

    private AxisRotatedTerminalCollection(TerminalBoundingBox[][] terminals) {
        this.terminals = terminals;
    }

    public TerminalBoundingBox get(BlockRotation rotation, int index) {
        return terminals[rotation.ordinal()][index];
    }

    public static class Builder extends TerminalCollectionBuilder<Builder, AxisRotatedTerminalCollection> {
        private final List<BlockRotation> rotations = new ArrayList<>();
        private final Direction.Axis axis;

        private Builder(Direction.Axis axis) {
            this.axis = axis;
        }

        public Builder with(BlockRotation rotation) {
            rotations.add(rotation);
            return this;
        }

        public Builder with(BlockRotation... rotations) {
            Collections.addAll(this.rotations, rotations);
            return this;
        }

        public AxisRotatedTerminalCollection build() {
            var terminals = new TerminalBoundingBox[4][this.terminals.size()];
            for(var rotation : rotations) {
                for (int j = 0; j < this.terminals.size(); ++j) {
                    terminals[rotation.ordinal()][j] = this.terminals.get(j).rotate(axis, rotation);
                }
            }
            return new AxisRotatedTerminalCollection(terminals);
        }
    }

    public static Builder builder(Direction.Axis axis) {
        return new Builder(axis);
    }
}
