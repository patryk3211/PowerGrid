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
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class TerminalBoundingBox implements ITerminalPlacement, IDecoratedTerminal {
    private Vec3d min;
    private Vec3d max;
    private Vec3d origin;
    private double expand;
    private final Text name;
    private int color;

    private TerminalBoundingBox(Text name) {
        this.name = name;
        this.color = GRAY;
    }

    private TerminalBoundingBox(TerminalBoundingBox other) {
        this.name = other.name;
        this.color = other.color;
        this.expand = other.expand;
        this.min = other.min;
        this.max = other.max;
        this.origin = other.origin;
    }

    public TerminalBoundingBox(Text name, double x1, double y1, double z1, double x2, double y2, double z2) {
        this(name, x1, y1, z1, x2, y2, z2, 0.1);
    }

    public TerminalBoundingBox(Text name, double x1, double y1, double z1, double x2, double y2, double z2, double expand) {
        this(name);
        min = new Vec3d((x1 - expand) / 16.0, (y1 - expand) / 16.0, (z1 - expand) / 16.0);
        max = new Vec3d((x2 + expand) / 16.0, (y2 + expand) / 16.0, (z2 + expand) / 16.0);
        origin = new Vec3d((x1 + x2) * 0.03125, (y1 + y2) * 0.03125, (z1 + z2) * 0.03125);
        this.expand = expand / 16.0;
    }

    public VoxelShape getShape() {
        return VoxelShapes.cuboid(min.x + expand, min.y + expand, min.z + expand,
                max.x - expand, max.y - expand, max.z - expand);
    }

    public TerminalBoundingBox rotateAroundX(BlockRotation rotation) {
        TerminalBoundingBox terminal = new TerminalBoundingBox(this);
        switch(rotation) {
            case NONE -> {
                terminal.min = min;
                terminal.max = max;
                terminal.origin = origin;
            }
            case CLOCKWISE_90 -> {
                terminal.min = new Vec3d(min.x, min.z, 1 - max.y);
                terminal.max = new Vec3d(max.x, max.z, 1 - min.y);
                terminal.origin = new Vec3d(origin.x, origin.z, 1 - origin.y);
            }
            case CLOCKWISE_180 -> {
                terminal.min = new Vec3d(min.x, 1 - max.y, 1 - max.z);
                terminal.max = new Vec3d(max.x, 1 - min.y, 1 - min.z);
                terminal.origin = new Vec3d(origin.x, 1 - origin.y, 1 - origin.z);
            }
            case COUNTERCLOCKWISE_90 -> {
                terminal.min = new Vec3d(min.x, 1 - max.z, min.y);
                terminal.max = new Vec3d(max.x, 1 - min.z, max.y);
                terminal.origin = new Vec3d(origin.x, 1 - origin.z, origin.y);
            }
        }
        return terminal;
    }

    public TerminalBoundingBox rotateAroundY(BlockRotation rotation) {
        TerminalBoundingBox terminal = new TerminalBoundingBox(this);
        switch(rotation) {
            case NONE -> {
                terminal.min = min;
                terminal.max = max;
                terminal.origin = origin;
            }
            case CLOCKWISE_90 -> {
                terminal.min = new Vec3d(1 - max.z, min.y, min.x);
                terminal.max = new Vec3d(1 - min.z, max.y, max.x);
                terminal.origin = new Vec3d(1 - origin.z, origin.y, origin.x);
            }
            case CLOCKWISE_180 -> {
                terminal.min = new Vec3d(1 - max.x, min.y, 1 - max.z);
                terminal.max = new Vec3d(1 - min.x, max.y, 1 - min.z);
                terminal.origin = new Vec3d(1 - origin.x, origin.y, 1 - origin.z);
            }
            case COUNTERCLOCKWISE_90 -> {
                terminal.min = new Vec3d(min.z, min.y, 1 - max.x);
                terminal.max = new Vec3d(max.z, max.y, 1 - min.x);
                terminal.origin = new Vec3d(origin.z, origin.y, 1 - origin.x);
            }
        }
        return terminal;
    }

    public TerminalBoundingBox rotateAroundZ(BlockRotation rotation) {
        TerminalBoundingBox terminal = new TerminalBoundingBox(this);
        switch(rotation) {
            case NONE -> {
                terminal.min = min;
                terminal.max = max;
                terminal.origin = origin;
            }
            case CLOCKWISE_90 -> {
                terminal.min = new Vec3d(min.y, 1 - max.x, min.z);
                terminal.max = new Vec3d(max.y, 1 - min.x, max.z);
                terminal.origin = new Vec3d(origin.y, 1 - origin.x, origin.z);
            }
            case CLOCKWISE_180 -> {
                terminal.min = new Vec3d(1 - max.x, 1 - max.y, min.z);
                terminal.max = new Vec3d(1 - min.x, 1 - min.y, max.z);
                terminal.origin = new Vec3d(1 - origin.x, 1 - origin.y, origin.z);
            }
            case COUNTERCLOCKWISE_90 -> {
                terminal.min = new Vec3d(1 - max.y, min.x, min.z);
                terminal.max = new Vec3d(1 - min.y, max.x, max.z);
                terminal.origin = new Vec3d(1 - origin.y, origin.x, origin.z);
            }
        }
        return terminal;
    }

    public TerminalBoundingBox rotate(Direction.Axis axis, BlockRotation rotation) {
        return switch(axis) {
            case X -> rotateAroundX(rotation);
            case Y -> rotateAroundY(rotation);
            case Z -> rotateAroundZ(rotation);
        };
    }

    public TerminalBoundingBox rotate(Direction.Axis axis, int angle) {
        return rotate(axis, angleToRotation(angle));
    }

    public TerminalBoundingBox rotateAroundX(int angle) {
        return rotateAroundX(angleToRotation(angle));
    }

    public TerminalBoundingBox rotateAroundY(int angle) {
        return rotateAroundY(angleToRotation(angle));
    }

    public TerminalBoundingBox rotateAroundZ(int angle) {
        return rotateAroundZ(angleToRotation(angle));
    }

    private static BlockRotation angleToRotation(int angle) {
        angle = angle % 360;
        if(angle < 0)
            angle += 360;
        return switch(angle) {
            case 0 -> BlockRotation.NONE;
            case 90 -> BlockRotation.CLOCKWISE_90;
            case 180 -> BlockRotation.CLOCKWISE_180;
            case 270 -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> throw new IllegalArgumentException("Angle must be a multiple of 90 degrees");
        };
    }

    public TerminalBoundingBox withOrigin(Vec3d origin) {
        this.origin = origin.multiply(1.0 / 16.0);
        return this;
    }

    public TerminalBoundingBox withOrigin(double x, double y, double z) {
        return withOrigin(new Vec3d(x, y, z));
    }

    public TerminalBoundingBox withColor(int rgb) {
        this.color = rgb;
        return this;
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

    @Override
    public Box getOutline() {
        return new Box(min, max);
    }

    @Override
    public int getColor() {
        return color;
    }
}
