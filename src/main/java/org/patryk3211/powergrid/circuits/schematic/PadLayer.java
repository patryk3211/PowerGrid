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
package org.patryk3211.powergrid.circuits.schematic;

import net.minecraft.nbt.LongArrayTag;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PadLayer {
    public static final int GRID_SIZE = 16;
    public static final int TOTAL_SIZE = GRID_SIZE * GRID_SIZE;

    private final BitSet map;

    public PadLayer() {
        map = new BitSet(TOTAL_SIZE);
    }

    public void set(int x, int y) {
        map.set(x + y * GRID_SIZE);
    }

    public boolean get(int x, int y) {
        return map.get(x + y * GRID_SIZE);
    }

    public List<Point> calculatePoints() {
        var points = new ArrayList<Point>();
        for (int x = 0; x < GRID_SIZE; ++x) {
            for (int y = 0; y < GRID_SIZE; ++y) {
                if (map.get(x + y * GRID_SIZE)) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

    public void clear() {
        map.clear();
    }
}
