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
package org.patryk3211.powergrid.electricity.sim.special;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;

class TransmissionConnectionEndpointsTests {
    @Test
    void physicalProxyEndpointRemainsIndependentFromItsCanonicalNodeEndpoint() {
        var physicalAlias = new BlockWireEndpoint(new BlockPos(438, 65, 54), 1);
        var canonicalEndpoint = new BlockWireEndpoint(new BlockPos(438, 64, 54), 1);
        var otherEndpoint = new BlockWireEndpoint(new BlockPos(438, 63, 62), 0);
        var canonicalNode = new OwnedFloatingNode(canonicalEndpoint);
        var endpoints = new TransmissionConnectionEndpoints(physicalAlias, otherEndpoint);

        Assertions.assertNotEquals(canonicalNode.endpoint, endpoints.endpoint1());
        Assertions.assertEquals(physicalAlias, endpoints.endpoint1());
        Assertions.assertTrue(endpoints.matches(physicalAlias, otherEndpoint));
        Assertions.assertFalse(endpoints.matches(canonicalEndpoint, otherEndpoint));
    }

    @Test
    void physicalEndpointsRoundTripThroughTheExistingSavedDataKeys() {
        var first = new BlockWireEndpoint(new BlockPos(-757, 65, 93), 2);
        var second = new BlockWireEndpoint(new BlockPos(-757, 64, 90), 1);
        var endpoints = new TransmissionConnectionEndpoints(first, second);
        var tag = new CompoundTag();

        endpoints.writeToNbt(tag);
        var restored = TransmissionConnectionEndpoints.fromNbt(tag);

        Assertions.assertEquals(first, restored.endpoint1());
        Assertions.assertEquals(second, restored.endpoint2());
        Assertions.assertTrue(tag.contains("Node1"));
        Assertions.assertTrue(tag.contains("Node2"));
        Assertions.assertEquals(2, tag.size());
    }
}
