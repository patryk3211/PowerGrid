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
import java.util.Collections;
import java.util.List;

public class CircuitLayer {
    public static final int GRID_SIZE = 16;
    public static final int GRID_TO_GRID_SCALE = GRID_SIZE / 16;

    private TraceMatrix traces;

    // Mirror traces, recalculated when necessary
    // TODO: it would be more efficient to access lines from a List<List<Line>> where e.g. verticalLines[x]
    //       is a list containing all the vertical lines in column x
    private final List<Line> verticalLines;
    private final List<Line> horizontalLines;

    public CircuitLayer() {
        traces = new TraceMatrix();
        verticalLines = new ArrayList<>();
        horizontalLines = new ArrayList<>();
    }

    public void from(CircuitLayer other) {
        traces = new TraceMatrix(other.traces);
        recalculateLines();
    }

    public LongArrayTag serializeNbt() {
        return traces.serializeNbt();
    }

    public void deserialize(long[] tag) {
        traces.deserialize(tag);
        recalculateLines();
    }

    public boolean hasTrace(int x, int y) {
        return traces.hasTrace(x, y);
    }

    public boolean get(int x, int y, TraceMatrix.TraceDirection dir) {
        return traces.get(x, y, dir);
    }

    private void recalculateLines() {
        verticalLines.clear();
        horizontalLines.clear();

        // Vertical lines
        Integer start = null;
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.UP)) {
                    // throw new IllegalStateException("")
                    // TODO: ILLEGAL STATE?
                    System.out.println("Illegal state: broken vertical line");
                }
                if (start == null && traces.get(x, y, TraceMatrix.TraceDirection.DOWN)) {
                    start = y;
                }
                else if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.DOWN)) {
                    verticalLines.add(new Line(true, x, start, y));
                    start = null;
                }
            }
        }

        // Horizontal lines
        start = null;
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.LEFT)) {
                    // throw new IllegalStateException("")
                    // TODO: ILLEGAL STATE?
                    System.out.println("Illegal state: broken horizontal line");
                }
                if (start == null && traces.get(x, y, TraceMatrix.TraceDirection.RIGHT)) {
                    start = x;
                }
                else if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.RIGHT)) {
                    horizontalLines.add(new Line(false, y, start, x));
                    start = null;
                }
            }
        }
    }

    public List<Line> readVerticalLines() {
        return Collections.unmodifiableList(verticalLines);
    }

    public List<Line> readHorizontalLines() {
        return Collections.unmodifiableList(horizontalLines);
    }

    /**
     * Determine whether two positive inclusive ranges overlap
     * @return true if [start1, end1] and [start2, end2] overlap
     */
    private static boolean rangesOverlap(int start1, int end1, int start2, int end2) {
        if (start1 > end1 || start2 > end2) {
            throw new IllegalArgumentException("One of the ranges was negative: " + start1 + ".." + end1 + ", " + start2 + ".." + end2);
        }
        return start1 <= end2 && start2 <= end1;
    }

    private void addLine(List<Line> lines, boolean vertical, int position, int start, int end) {
        List<Line> overlaps = new ArrayList<>();
        int newStart = start;
        int newEnd = end;
        for (var line : lines) {
            if (line.vertical() == vertical && line.position() == position &&
                    rangesOverlap(start, end, line.start(), line.end())) {
                overlaps.add(line);
                newStart = Math.min(newStart, line.start());
                newEnd = Math.max(newEnd, line.end());
            }
        }
        lines.removeAll(overlaps);
        lines.add(new Line(vertical, position, newStart, newEnd));

        traces.debugTraces();
    }

    public void addVerticalLine(int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            if (y1 < y) {
                traces.set(x, y, TraceMatrix.TraceDirection.UP, true);
            }
            if (y < y2) {
                traces.set(x, y, TraceMatrix.TraceDirection.DOWN, true);
            }
        }

        addLine(verticalLines, true, x, y1, y2);
    }

    public void addHorizontalLine(int y, int x1, int x2) {
        for (int x = x1; x <= x2; x++) {
            if (x1 < x) {
                traces.set(x, y, TraceMatrix.TraceDirection.LEFT, true);
            }
            if (x < x2) {
                traces.set(x, y, TraceMatrix.TraceDirection.RIGHT, true);
            }
        }

        addLine(horizontalLines, false, y, x1, x2);
    }

    public void clear(int x1, int y1, int x2, int y2) {
        System.err.println("CLEAR: (" + x1 + "," + y1 + ")..(" + x2 + "," + y2 + ")");
        traces.clear(x1, y1, x2, y2);
        recalculateLines();
        traces.debugTraces();
    }

    public void clear() {
        clear(0, 0, GRID_SIZE - 1, GRID_SIZE - 1);
    }
}
