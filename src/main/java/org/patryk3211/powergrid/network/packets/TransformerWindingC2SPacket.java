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
package org.patryk3211.powergrid.network.packets;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.patryk3211.powergrid.collections.ModdedComponentTypes;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.network.C2SPacket;

import java.util.Objects;

public class TransformerWindingC2SPacket implements C2SPacket {
    private final int nTurns;
    private final InteractionHand hand;

    public TransformerWindingC2SPacket(int nTurns, InteractionHand hand) {
        this.nTurns = nTurns;
        this.hand = hand;
    }

    public TransformerWindingC2SPacket(FriendlyByteBuf buf) {
        nTurns = buf.readInt();
        hand = buf.readEnum(InteractionHand.class);
    }


    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(nTurns);
        buf.writeEnum(hand);
    }

    @Override
    public void handle(ServerPlayer player) {
        var stack = player.getItemInHand(hand);
        if(!(stack.getItem() instanceof IWire) || stack.has(ModdedComponentTypes.CONNECTION))
            return;
        if(stack.has(ModdedComponentTypes.TURNS)) {
            // Alter existing tag
            stack.set(ModdedComponentTypes.TURNS, nTurns);
        } else {
            // Create a new tag
            var endpoint = WireEndpointType.deserialize(Objects.requireNonNull(stack.get(ModdedComponentTypes.CONNECTION)).getCompound("Connection"));
            if (endpoint == null || endpoint.type() != WireEndpointType.BLOCK)
                return;
            var blockEndpoint = (BlockWireEndpoint) endpoint;
            var nbt = new CompoundTag();
            nbt.putInt("Turns", nTurns);
            var pos = blockEndpoint.getPos();
            nbt.putIntArray("Initiator", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            nbt.putInt("Terminal", blockEndpoint.getTerminal());
            stack.set(ModdedComponentTypes.CONNECTION, nbt);
        }
    }
}
