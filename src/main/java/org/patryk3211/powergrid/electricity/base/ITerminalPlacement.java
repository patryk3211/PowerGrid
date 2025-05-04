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

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface ITerminalPlacement {
    boolean check(Vec3d position);

    default boolean check(BlockPos blockPos, Vec3d position) {
        return check(position.subtract(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }

    Vec3d getOrigin();
}
