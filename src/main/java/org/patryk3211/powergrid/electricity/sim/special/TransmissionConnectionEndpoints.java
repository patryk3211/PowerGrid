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

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.electricity.wire.IWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

import java.util.Objects;

/**
 * The physical attachment points owned by a persisted transmission segment.
 *
 * <p>A multiblock proxy endpoint can resolve to a node whose canonical endpoint
 * belongs to the main block. These connection endpoints must therefore remain
 * independent from the nodes used by the electrical topology.</p>
 */
final class TransmissionConnectionEndpoints {
    private static final String ENDPOINT_1_TAG = "Node1";
    private static final String ENDPOINT_2_TAG = "Node2";

    @NotNull
    private final IWireEndpoint endpoint1;
    @NotNull
    private final IWireEndpoint endpoint2;

    TransmissionConnectionEndpoints(
            @NotNull IWireEndpoint endpoint1,
            @NotNull IWireEndpoint endpoint2
    ) {
        this.endpoint1 = Objects.requireNonNull(endpoint1);
        this.endpoint2 = Objects.requireNonNull(endpoint2);
    }

    static TransmissionConnectionEndpoints fromNbt(CompoundTag tag) {
        return new TransmissionConnectionEndpoints(
                WireEndpointType.deserialize(tag.getCompound(ENDPOINT_1_TAG)),
                WireEndpointType.deserialize(tag.getCompound(ENDPOINT_2_TAG))
        );
    }

    @NotNull
    IWireEndpoint endpoint1() {
        return endpoint1;
    }

    @NotNull
    IWireEndpoint endpoint2() {
        return endpoint2;
    }

    boolean matches(IWireEndpoint other1, IWireEndpoint other2) {
        return sameEndpoints(endpoint1, endpoint2, other1, other2);
    }

    void writeToNbt(CompoundTag tag) {
        tag.put(ENDPOINT_1_TAG, endpoint1.serialize());
        tag.put(ENDPOINT_2_TAG, endpoint2.serialize());
    }

    static boolean sameEndpoints(
            IWireEndpoint stored1,
            IWireEndpoint stored2,
            IWireEndpoint current1,
            IWireEndpoint current2
    ) {
        return stored1.equals(current1) && stored2.equals(current2)
                || stored1.equals(current2) && stored2.equals(current1);
    }
}
