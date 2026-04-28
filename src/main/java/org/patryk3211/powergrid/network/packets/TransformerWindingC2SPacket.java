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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.patryk3211.powergrid.collections.ModdedDataComponents;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireConnection;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.network.C2SPacket;

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
        var connection = stack.get(ModdedDataComponents.CONNECTION_DATA.get());
        if (!IWire.isWire(player.level(), stack.getItem()) || connection == null)
            return;
        if (connection.isTransformer()) {
            // Alter existing tag
            stack.set(ModdedDataComponents.CONNECTION_DATA.get(), connection.withTurns(nTurns));
        } else {
            // Create a new tag
            var endpoint = connection.endpoint();
            if (endpoint == null || endpoint.type() != WireEndpointType.BLOCK)
                return;
            var blockEndpoint = (BlockWireEndpoint) endpoint;
            var pos = blockEndpoint.getPos();
            stack.set(ModdedDataComponents.CONNECTION_DATA.get(), WireConnection.of(pos, nTurns, blockEndpoint));
        }
    }
}
