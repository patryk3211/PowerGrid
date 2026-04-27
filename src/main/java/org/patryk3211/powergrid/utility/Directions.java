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
package org.patryk3211.powergrid.utility;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import static net.minecraft.core.Direction.*;

public class Directions {
    public static final Direction[] ALL = {
            NORTH, SOUTH, EAST, WEST, UP, DOWN
    };
    public static final Direction[] HORIZONTAL = {
            NORTH, SOUTH, EAST, WEST
    };
    public static final Direction[] VERTICAL = {
            UP, DOWN
    };
    public static final Axis[] HORIZONTAL_AXIS = {
            Axis.X, Axis.Z
    };

    public static BooleanProperty property(Direction dir) {
        return switch(dir) {
            case EAST -> BlockStateProperties.EAST;
            case WEST -> BlockStateProperties.WEST;
            case UP -> BlockStateProperties.UP;
            case DOWN -> BlockStateProperties.DOWN;
            case SOUTH -> BlockStateProperties.SOUTH;
            case NORTH -> BlockStateProperties.NORTH;
        };
    }
}
