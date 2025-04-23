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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;

public enum ShaftDirection implements StringIdentifiable {
    POSITIVE("positive"),
    NEGATIVE("negative"),
    NONE("none");

    private final String name;

    ShaftDirection(String name) {
        this.name = name;
    }

    public static ShaftDirection from(Direction.AxisDirection direction) {
        return switch (direction) {
            case POSITIVE -> POSITIVE;
            case NEGATIVE -> NEGATIVE;
        };
    }

    @Override
    public String asString() {
        return name;
    }

    public Direction.AxisDirection axisDirection() {
        return switch (this) {
            case POSITIVE -> Direction.AxisDirection.POSITIVE;
            case NEGATIVE -> Direction.AxisDirection.NEGATIVE;
            case NONE -> null;
        };
    }

    public ShaftDirection opposite() {
        return switch (this) {
            case POSITIVE -> NEGATIVE;
            case NEGATIVE -> POSITIVE;
            case NONE -> NONE;
        };
    }

    public Direction with(Direction.Axis axis) {
        return switch (axis) {
            case X -> switch (this) {
                case POSITIVE -> Direction.EAST;
                case NEGATIVE -> Direction.WEST;
                default -> null;
            };
            case Y -> switch (this) {
                case POSITIVE -> Direction.UP;
                case NEGATIVE -> Direction.DOWN;
                default -> null;
            };
            case Z -> switch (this) {
                case POSITIVE -> Direction.SOUTH;
                case NEGATIVE -> Direction.NORTH;
                default -> null;
            };
        };
    }
}
