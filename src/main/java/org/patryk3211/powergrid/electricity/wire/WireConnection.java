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

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class WireConnection {
    public static final Codec<WireConnection> CODEC = CompoundTag.CODEC.xmap(WireConnection::new, WireConnection::tag);
    public static final WireConnection EMPTY = new WireConnection(new CompoundTag());

    private final CompoundTag tag;

    public WireConnection(CompoundTag tag) {
        this.tag = tag;
    }

    public static WireConnection of(IWireEndpoint endpoint) {
        var tag = new CompoundTag();
        tag.put("Connection", endpoint.serialize());
        return new WireConnection(tag);
    }

    public static WireConnection of(BlockPos pos, int nTurns, BlockWireEndpoint blockEndpoint) {
        var tag = new CompoundTag();
        var trTag = new CompoundTag();
        trTag.putInt("Turns", nTurns);
        trTag.put("Initiator", NbtUtils.writeBlockPos(pos));
        trTag.put("Terminal", blockEndpoint.serialize());
        tag.put("Transformer", trTag);
        return new WireConnection(tag);
    }

    protected CompoundTag tag() {
        return tag;
    }

    @Nullable
    public IWireEndpoint endpoint() {
        return WireEndpointType.deserialize(tag.getCompound("Connection"));
    }

    public WireConnection withEndpoint(IWireEndpoint endpoint) {
        var result = new WireConnection(tag.copy());
        result.tag.put("Connection", endpoint.serialize());
        return result;
    }

    public boolean hasHalf() {
        return tag.contains("Half");
    }

    @Nullable
    public IWireEndpoint half() {
        return WireEndpointType.deserialize(tag.getCompound("Half"));
    }

    public WireConnection withHalf(BlockWireEndpoint endpoint) {
        var result = new WireConnection(tag.copy());
        result.tag.put("Half", endpoint.serialize());
        return result;
    }

    public boolean isTransformer() {
        return tag.contains("Transformer");
    }

    @Nullable
    public BlockPos getTransformerInitiator() {
        var trTag = tag.getCompound("Transformer");
        return NbtUtils.readBlockPos(trTag, "Initiator").orElse(null);
    }

    public int getTransformerTurns() {
        var trTag = tag.getCompound("Transformer");
        return trTag.getInt("Turns");
    }

    public int getTransformerTerminal() {
        var trTag = tag.getCompound("Transformer");
        return trTag.getInt("Terminal");
    }

    public WireConnection withTurns(int nTurns) {
        var result = new WireConnection(tag.copy());
        result.tag.getCompound("Transformer").putInt("Turns", nTurns);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof WireConnection other)
            return tag.equals(other.tag);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag);
    }
}
