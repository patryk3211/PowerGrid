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

import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

import static net.minecraft.util.math.Direction.*;

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

    public static BooleanProperty property(Direction dir) {
        return switch(dir) {
            case EAST -> Properties.EAST;
            case WEST -> Properties.WEST;
            case UP -> Properties.UP;
            case DOWN -> Properties.DOWN;
            case SOUTH -> Properties.SOUTH;
            case NORTH -> Properties.NORTH;
        };
    }
}
