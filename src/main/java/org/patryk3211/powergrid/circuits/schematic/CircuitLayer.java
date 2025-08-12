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

public class CircuitLayer {
    public static final int GRID_SIZE = 16;
    public static final int GRID_TO_GRID_SCALE = GRID_SIZE / 16;
    public static final int TOTAL_SIZE = GRID_SIZE * GRID_SIZE;

    private BitSet map;

    public CircuitLayer() {
        map = new BitSet(TOTAL_SIZE);
    }

    public void from(CircuitLayer other) {
        map = (BitSet) other.map.clone();
    }

    public LongArrayTag serializeNbt() {
        return new LongArrayTag(map.toLongArray());
    }

    public void deserialize(long[] tag) {
        map = BitSet.valueOf(tag);
    }

    public void set(int x, int y) {
        map.set(x + y * GRID_SIZE);
    }

    public boolean get(int x, int y) {
        return map.get(x + y * GRID_SIZE);
    }

    public List<Line> calculateLines() {
        var lines = new ArrayList<Line>();
        var visited = new BitSet(TOTAL_SIZE);
        for(int x = 0; x < GRID_SIZE; ++x) {
            for(int y = 0; y < GRID_SIZE; ++y) {
                var bit = map.get(x + y * GRID_SIZE);
                if(!bit || visited.get(x + y * GRID_SIZE))
                    continue;
                int len = 1;
                // Search for a vertical line.
                for(int i = y + 1; i < GRID_SIZE; ++i) {
                    if(!map.get(x + i * GRID_SIZE) || visited.get(x + i * GRID_SIZE))
                        break;
                    visited.set(x + i * GRID_SIZE);
                    ++len;
                }
                if(len > 1) {
                    // Make a vertical line.
                    lines.add(new Line(true, x, y, y + len));
                    continue;
                }
                // Search for a horizontal line.
                for(int i = x + 1; i < GRID_SIZE; ++i) {
                    if(!map.get(i + y * GRID_SIZE) || visited.get(i + y * GRID_SIZE))
                        break;
                    visited.set(i + y * GRID_SIZE);
                    ++len;
                }
                // Make a horizontal line.
                lines.add(new Line(false, y, x, x + len));
            }
        }

        return lines;
    }

    public List<Point> calculatePoints() {
        var points = new ArrayList<Point>();
        for(int x = 0; x < GRID_SIZE; ++x) {
            for(int y = 0; y < GRID_SIZE; ++y) {
                if(map.get(x + y * GRID_SIZE)) {
                    points.add(new Point(x, y));
                }
            }
        }
        return points;
    }

    public void fill(int x1, int y1, int x2, int y2) {
        for(int x = x1; x <= x2; ++x) {
            for(int y = y1; y <= y2; ++y) {
                map.set(x + y * GRID_SIZE);
            }
        }
    }

    public void clear(int x1, int y1, int x2, int y2) {
        for(int x = x1; x <= x2; ++x) {
            for(int y = y1; y <= y2; ++y) {
                map.clear(x + y * GRID_SIZE);
            }
        }
    }

    public void clear() {
        map.clear();
    }
}
