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
package org.patryk3211.powergrid.network.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.wire.WireRenderSync;

public class EntityDataS2CPacketTests {
    @Test
    void completePreSpawnDataReplacesStaleRenderOnlyData() {
        int entityId = 42;
        var renderOnly = WireRenderSync.terminalGeometry(Vec3.ZERO, new Vec3(1, 2, 3), false);

        var complete = new CompoundTag();
        complete.put("Item", new CompoundTag());

        EntityDataS2CPacket.deferData(entityId, renderOnly);
        EntityDataS2CPacket.deferData(entityId, complete);

        var deferred = EntityDataS2CPacket.takeDeferredData(entityId);
        Assertions.assertSame(complete, deferred);
        Assertions.assertTrue(deferred.contains("Item"));
        Assertions.assertFalse(deferred.contains("V"));
        Assertions.assertNull(EntityDataS2CPacket.takeDeferredData(entityId));
    }
}
