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
package org.patryk3211.powergrid.circuits.components.properties;

import org.patryk3211.powergrid.PowerGrid;

public enum Orientation {
    RIGHT(2, 3, 1),
    UP(3, 0, 2),
    LEFT(0, 1, 3),
    DOWN(1, 2, 0);

    public static final EnumProperty<Orientation> PROPERTY =
            (EnumProperty<Orientation>) new EnumProperty<>(PowerGrid.MOD_ID, "orientation", Orientation.class).hidden();

    private final int idOpposite, idClockwise, idCounterClockwise;

    Orientation(int idOpposite, int idClockwise, int idCounterClockwise) {
        this.idOpposite = idOpposite;
        this.idClockwise = idClockwise;
        this.idCounterClockwise = idCounterClockwise;
    }

    public Orientation getOpposite() {
        return values()[idOpposite];
    }

    public Orientation getClockwise() {
        return values()[idClockwise];
    }

    public Orientation getCounterClockwise() {
        return values()[idCounterClockwise];
    }

    public Orientation rotate(Orientation rotateBy) {
        switch(rotateBy) {
            case RIGHT -> {
                // 0 degrees
                return this;
            }
            case DOWN -> {
                // 90 degrees
                return getClockwise();
            }
            case LEFT -> {
                // 180 degrees
                return getOpposite();
            }
            case UP -> {
                // 270 degrees
                return getCounterClockwise();
            }
            default -> throw new IllegalStateException();
        }
    }

    public boolean positive() {
        return this == RIGHT || this == DOWN;
    }

    public boolean isX() {
        return this == RIGHT || this == LEFT;
    }

    public boolean isY() {
        return this == UP || this == DOWN;
    }
}
