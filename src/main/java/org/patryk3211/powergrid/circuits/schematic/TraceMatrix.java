package org.patryk3211.powergrid.circuits.schematic;

import net.minecraft.nbt.LongArrayTag;

import java.util.BitSet;

public class TraceMatrix {
    public static final int GRID_SIZE = 16;
    public static final int WIDTH = GRID_SIZE;
    public static final int HEIGHT = GRID_SIZE;
    private static final int CELL_SIZE = 4;
    private static final int TOTAL_SIZE = WIDTH * HEIGHT * CELL_SIZE;

    private BitSet map;

    public TraceMatrix() {
        map = new BitSet(TOTAL_SIZE);
    }

    public TraceMatrix(TraceMatrix other) {
        map = (BitSet) other.map.clone();
    }

    public LongArrayTag serializeNbt() {
        return new LongArrayTag(map.toLongArray());
    }

    public void deserialize(long[] tag, boolean fullPixelTraces) {
        if (!fullPixelTraces || tag.length > 16) {
            // Use new directional traces if requested or if there's too much data for this to be legacy
            // 16 longs = 32 bytes = 256 bits
            map = BitSet.valueOf(tag);
        }
        else {
            // Legacy circuit with full pixel traces
            map = new BitSet(TOTAL_SIZE);
            BitSet old = BitSet.valueOf(tag);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    if (old.get(y * WIDTH + x)) {
                        set(x, y, TraceDirection.UP, y > 0 && old.get((y - 1) * WIDTH + x));
                        set(x, y, TraceDirection.DOWN, y < HEIGHT - 1 && old.get((y + 1) * WIDTH + x));
                        set(x, y, TraceDirection.LEFT, x > 0 && old.get(y * WIDTH + x - 1));
                        set(x, y, TraceDirection.RIGHT, x < WIDTH - 1 && old.get(y * WIDTH + x + 1));
                    }
                }
            }
        }
    }

    private int cellIndex(int x, int y) {
        return (y * WIDTH + x) * CELL_SIZE;
    }

    public void set(int x, int y, TraceDirection dir, boolean value) {
        map.set(cellIndex(x, y) + dir.getBitIndex(), value);
    }

    public void clear(int x, int y) {
        int startIdx = cellIndex(x, y);
        for (int i = startIdx; i < startIdx + CELL_SIZE; i++) {
            map.set(i, false);
        }
    }

    public boolean get(int x, int y, TraceDirection dir) {
        return map.get(cellIndex(x, y) + dir.getBitIndex());
    }

    public boolean hasTrace(int x, int y) {
        int startIdx = cellIndex(x, y);
        for (int i = startIdx; i < startIdx + CELL_SIZE; i++) {
            if (map.get(i)) {
                return true;
            }
        }
        return false;
    }

    public void clear(int x1, int y1, int x2, int y2) {
        // Clear requested area
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                clear(x, y);
            }
        }

        // Clear incoming connections from area borders
        // TODO: This prevents single cell traces from being created by clearing. They're still possible by directly
        //       drawing them. Should they be allowed?
        if (y1 > 0) {
            for (int x = x1; x <= x2; x++) {
                set(x, y1 - 1, TraceDirection.DOWN, false);
            }
        }
        if (y2 < HEIGHT - 1) {
            for (int x = x1; x <= x2; x++) {
                set(x, y2 + 1, TraceDirection.UP, false);
            }
        }
        if (x1 > 0) {
            for (int y = y1; y <= y2; y++) {
                set(x1 - 1, y, TraceDirection.RIGHT, false);
            }
        }
        if (x2 < WIDTH - 1) {
            for (int y = y1; y <= y2; y++) {
                set(x2 + 1, y, TraceDirection.LEFT, false);
            }
        }
    }

    private static final char[] CELL_TO_CHAR = new char[] {
            ' ',  // 0b0000  ' '
            '<',  // 0b0001  '←'
            '>',  // 0b0010  '→'
            '-',  // 0b0011  '─'

            '^',  // 0b0100  '↑'
            '/',  // 0b0101  '┌'
            '\\', // 0b0110  '┐'
            'T',  // 0b0111  '┬'

            'V',  // 0b1000  '↓'
            '\\', // 0b1001  '└'
            '/',  // 0b1010  '┘'
            'W',  // 0b1011  '┴'

            '|',  // 0b1100  '│'
            '}',  // 0b1101  '├'
            '{',  // 0b1110  '┤'
            '+',  // 0b1111  '┼'
    };

    public void debugTraces() {
        int startIdx, cell;
        System.out.print(' ');
        for (int i = 0; i < WIDTH; i++) {
            System.out.print('.');
        }
        System.out.println();
        for (int y = 0; y < HEIGHT; y++) {
            System.out.print('.');
            for (int x = 0; x < WIDTH; x++) {
                startIdx = cellIndex(x, y);
                cell = 0;
                for (int i = 0; i < CELL_SIZE; i++) {
                    cell |= (map.get(startIdx + i) ? 1 : 0) << (3 - i);
                }
                System.out.print(CELL_TO_CHAR[cell]);
            }
            System.out.println('.');
        }
        System.out.print(' ');
        for (int i = 0; i < WIDTH; i++) {
            System.out.print('.');
        }
        System.out.println();
    }

    public enum TraceDirection {
        // 0 is MSB, 3 is LSB (farthest right)
        UP(0), DOWN(1), LEFT(2), RIGHT(3);

        private final int bitIndex;

        TraceDirection(int bitIndex) {
            this.bitIndex = bitIndex;
        }

        public static TraceDirection fromXY(int x, int y) throws IllegalArgumentException {
            if (x != 0 && y != 0) {
                throw new IllegalArgumentException("Can't get trace direction from XY (" +
                        x + "," + ") because both are nonzero");
            }
            else if (x == 1) return RIGHT;
            else if (x == -1) return LEFT;
            else if (y == 1) return DOWN;
            else if (y == -1) return UP;
            else {
                throw new IllegalArgumentException("Invalid XY (" + x + "," + y + ") for trace direction");
            }
        }

        public int getBitIndex() {
            return this.bitIndex;
        }
    }
}
