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

import net.minecraft.util.math.Vec3d;

public class TerminalPlacement implements ITerminalPlacement {
    private final Vec3d position;
    private final double size;

    public TerminalPlacement(Vec3d position, double size) {
        this.position = position.multiply(1.0 / 16.0);
        this.size = size / 16.0;
    }

    public TerminalPlacement(double x, double y, double z, double size) {
        this.position = new Vec3d(x / 16.0, y / 16.0, z / 16.0);
        this.size = size / 16.0;
    }

    public boolean check(Vec3d position) {
        return Math.abs(position.x - this.position.x) < size &&
                Math.abs(position.y - this.position.y) < size &&
                Math.abs(position.z - this.position.z) < size;
    }

    @Override
    public Vec3d getOrigin() {
        return position;
    }

    public Vec3d getPosition() {
        return position;
    }

    public double getSize() {
        return size;
    }
}
