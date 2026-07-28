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

import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class WireRenderSyncTests {
    @Test
    void terminalGeometryContainsServerCalculatedEndpoints() {
        var terminal1 = new Vec3(82.125, 68.75, 16.5);
        var terminal2 = new Vec3(110.875, 71.25, 43.5);

        var tag = WireRenderSync.terminalGeometry(terminal1, terminal2, true);
        var positions = tag.getList("V", Tag.TAG_FLOAT);

        Assertions.assertEquals(6, positions.size());
        Assertions.assertEquals((float) terminal1.x, positions.getFloat(0));
        Assertions.assertEquals((float) terminal1.y, positions.getFloat(1));
        Assertions.assertEquals((float) terminal1.z, positions.getFloat(2));
        Assertions.assertEquals((float) terminal2.x, positions.getFloat(3));
        Assertions.assertEquals((float) terminal2.y, positions.getFloat(4));
        Assertions.assertEquals((float) terminal2.z, positions.getFloat(5));
        Assertions.assertTrue(tag.getBoolean("D"));
        Assertions.assertFalse(tag.contains("Version"));
        Assertions.assertFalse(tag.contains("Item"));
    }

    @Test
    void terminalGeometryWaitsForWireMaterial() {
        var tag = WireRenderSync.terminalGeometry(Vec3.ZERO, new Vec3(1, 2, 3), false);

        Assertions.assertFalse(WireRenderSync.canApplyTerminalGeometry(tag, false));
        Assertions.assertTrue(WireRenderSync.canApplyTerminalGeometry(tag, true));
        Assertions.assertTrue(WireRenderSync.canApplyTerminalGeometry(new net.minecraft.nbt.CompoundTag(), false));
    }

    @Test
    void renderBroadcastsOnlyReachCompletedTrackingSessions() {
        var recipients = new WireTrackingRecipients<Object>();
        var player = new Object();
        var deliveries = new ArrayList<Object>();

        recipients.forEach(deliveries::add);
        Assertions.assertTrue(deliveries.isEmpty());

        recipients.start(player);
        recipients.forEach(deliveries::add);
        Assertions.assertEquals(java.util.List.of(player), deliveries);

        recipients.stop(player);
        recipients.forEach(deliveries::add);
        Assertions.assertEquals(java.util.List.of(player), deliveries);
    }
}
