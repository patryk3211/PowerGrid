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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class TerminalBoundingBox implements ITerminalPlacement, INamedTerminal {
    private final Vec3d min;
    private final Vec3d max;
    private final Vec3d origin;
    private final double expand;
    private final Text name;

    public TerminalBoundingBox(Text name, double x1, double y1, double z1, double x2, double y2, double z2) {
        min = new Vec3d(x1 / 16.0, y1 / 16.0, z1 / 16.0);
        max = new Vec3d(x2 / 16.0, y2 / 16.0, z2 / 16.0);
        origin = min.add(max).multiply(0.5);
        expand = 0;
        this.name = name;
    }

    public TerminalBoundingBox(Text name, double x1, double y1, double z1, double x2, double y2, double z2, double expand) {
        min = new Vec3d((x1 - expand) / 16.0, (y1 - expand) / 16.0, (z1 - expand) / 16.0);
        max = new Vec3d((x2 + expand) / 16.0, (y2 + expand) / 16.0, (z2 + expand) / 16.0);
        origin = min.add(max).multiply(0.5);
        this.expand = expand / 16.0;
        this.name = name;
    }

    public VoxelShape getShape() {
        return VoxelShapes.cuboid(min.x + expand, min.y + expand, min.z + expand,
                max.x - expand, max.y - expand, max.z - expand);
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
