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
package org.patryk3211.powergrid.electricity.base;

import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class TerminalBoundingBox implements ITerminalPlacement, INamedTerminal {
    private Vec3d min;
    private Vec3d max;
    private Vec3d origin;
    private double expand;
    private final Text name;

    private TerminalBoundingBox(Text name) {
        this.name = name;
    }

    public TerminalBoundingBox(Text name, double x1, double y1, double z1, double x2, double y2, double z2) {
        min = new Vec3d(x1 / 16.0, y1 / 16.0, z1 / 16.0);
        max = new Vec3d(x2 / 16.0, y2 / 16.0, z2 / 16.0);
        origin = new Vec3d((x1 + x2) * 0.03125, (y1 + y2) * 0.03125, (z1 + z2) * 0.03125);
        expand = 0;
        this.name = name;
    }

    public TerminalBoundingBox(Text name, double x1, double y1, double z1, double x2, double y2, double z2, double expand) {
        min = new Vec3d((x1 - expand) / 16.0, (y1 - expand) / 16.0, (z1 - expand) / 16.0);
        max = new Vec3d((x2 + expand) / 16.0, (y2 + expand) / 16.0, (z2 + expand) / 16.0);
        origin = new Vec3d((x1 + x2) * 0.03125, (y1 + y2) * 0.03125, (z1 + z2) * 0.03125);
        this.expand = expand / 16.0;
        this.name = name;
    }

    public VoxelShape getShape() {
        return VoxelShapes.cuboid(min.x + expand, min.y + expand, min.z + expand,
                max.x - expand, max.y - expand, max.z - expand);
    }

    /**
     * Rotates the terminal bounding box around the block's center,
     * the angle is determined by the provided direction, while
     * treating the current terminal orientation as north.
     */
    public TerminalBoundingBox rotated(Direction direction) {
        var xSize = max.x - min.x;
        var ySize = max.y - min.y;
        var zSize = max.z - min.z;
        TerminalBoundingBox terminal = new TerminalBoundingBox(name);
        terminal.expand = expand;
        switch(direction) {
            case NORTH -> {
                terminal.min = min;
                terminal.max = max;
                terminal.origin = origin;
            }
            case EAST -> {
                terminal.min = new Vec3d(1 - min.z - zSize, min.y, min.x);
                terminal.max = new Vec3d(1 - min.z, max.y, max.x);
                terminal.origin = new Vec3d(1 - origin.z, origin.y, origin.x);
            }
            case SOUTH -> {
                terminal.min = new Vec3d(1 - min.x - xSize, min.y, 1 - min.z - zSize);
                terminal.max = new Vec3d(1 - min.x, max.y, 1 - min.z);
                terminal.origin = new Vec3d(1 - origin.x, origin.y, 1 - origin.z);
            }
            case WEST -> {
                terminal.min = new Vec3d(min.z, min.y, 1 - min.x - xSize);
                terminal.max = new Vec3d(max.z, max.y, 1 - min.x);
                terminal.origin = new Vec3d(origin.z, origin.y, 1 - origin.x);
            }
            case UP, DOWN -> throw new IllegalArgumentException("Current unsupported");
        };
        return terminal;
    }

    public TerminalBoundingBox withOrigin(Vec3d origin) {
        this.origin = origin.multiply(1.0 / 16.0);
        return this;
    }

    public TerminalBoundingBox withOrigin(double x, double y, double z) {
        return withOrigin(new Vec3d(x, y, z));
    }

    @Override
    public boolean check(Vec3d position) {
        return position.x >= min.x && position.y >= min.y && position.z >= min.z &&
                position.x < max.x && position.y < max.y && position.z < max.z;
    }

    @Override
    public Vec3d getOrigin() {
        return origin;
    }

    @Override
    public Text getName() {
        return name;
    }
}
