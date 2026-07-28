/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.phys.Vec3;

/**
 * Builds the terminal-geometry payload already understood by official
 * Power Grid 0.5.5.1 clients.
 */
public final class WireRenderSync {
    private WireRenderSync() {
    }

    public static boolean canApplyTerminalGeometry(CompoundTag data, boolean materialReady) {
        return !data.contains("V") || materialReady;
    }

    public static CompoundTag terminalGeometry(Vec3 terminal1, Vec3 terminal2, boolean dynamic) {
        var tag = new CompoundTag();
        var positions = new ListTag();
        positions.add(FloatTag.valueOf((float) terminal1.x));
        positions.add(FloatTag.valueOf((float) terminal1.y));
        positions.add(FloatTag.valueOf((float) terminal1.z));
        positions.add(FloatTag.valueOf((float) terminal2.x));
        positions.add(FloatTag.valueOf((float) terminal2.y));
        positions.add(FloatTag.valueOf((float) terminal2.z));
        tag.putBoolean("D", dynamic);
        tag.put("V", positions);
        return tag;
    }
}
