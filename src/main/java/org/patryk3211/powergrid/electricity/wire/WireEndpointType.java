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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Contract;
import org.patryk3211.powergrid.electricity.wire.powercord.*;

import java.util.function.Supplier;

public enum WireEndpointType {
    BLOCK(BlockWireEndpoint::new, true),
    JUNCTION(JunctionWireEndpoint::new, true),
    BLOCK_WIRE(BlockWireEntityEndpoint::new, false),
    IMAGINARY(ImaginaryWireEndpoint::new, false),
    DEFERRED_JUNCTION(DeferredJunctionWireEndpoint::new, true),

    // Special endpoint type used by the multimeter
    CIRCUIT_BOARD(CircuitBoardEndpoint::new),

    // Cord endpoint types
    SOCKET(SocketEndpoint::new),
    SPLIT_CORD(SplitCordEndpoint::new),
    AUTO_CORD(AutoCordEndpoint::new)
    ;

    private final Supplier<IWireEndpoint> factory;
    // This is only used by the block wire placement code.
    private final boolean connectable;

    WireEndpointType(Supplier<IWireEndpoint> factory, boolean connectable) {
        this.factory = factory;
        this.connectable = connectable;
    }

    WireEndpointType(Supplier<IWireEndpoint> factory) {
        this(factory, false);
    }

    public boolean isConnectable() {
        return connectable;
    }

    public CompoundTag serialize(IWireEndpoint endpoint) {
        var tag = new CompoundTag();
        tag.putInt("Type", ordinal());
        endpoint.write(tag);
        return tag;
    }

    @Contract("null -> null")
    public static IWireEndpoint deserialize(CompoundTag tag) {
        if(tag == null)
            return null;
        if(!tag.contains("Type"))
            return null;
        var type = values()[tag.getInt("Type")];
        var endpoint = type.factory.get();
        endpoint.read(tag);
        return endpoint;
    }
}
