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

import dev.architectury.networking.NetworkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class TransformerWindingC2SPacket implements SimplePacket {
    private final int nTurns;
    private final Hand hand;

    public TransformerWindingC2SPacket(int nTurns, Hand hand) {
        this.nTurns = nTurns;
        this.hand = hand;
    }

    public TransformerWindingC2SPacket(PacketByteBuf buf) {
        nTurns = buf.readInt();
        hand = buf.readEnumConstant(Hand.class);
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeInt(nTurns);
        buf.writeEnumConstant(hand);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var stack = ctx.getPlayer().getStackInHand(hand);
            if(!(stack.getItem() instanceof IWire) || !stack.hasNbt())
                return;
            if(stack.getNbt().contains("Turns")) {
                // Alter existing tag
                stack.getNbt().putInt("Turns", nTurns);
            } else {
                // Create a new tag
                var endpoint = WireEndpointType.deserialize(stack.getNbt());
                if (endpoint == null || endpoint.type() != WireEndpointType.BLOCK)
                    return;
                var blockEndpoint = (BlockWireEndpoint) endpoint;
                var nbt = new NbtCompound();
                nbt.putInt("Turns", nTurns);
                var pos = blockEndpoint.getPos();
                nbt.putIntArray("Initiator", new int[]{pos.getX(), pos.getY(), pos.getZ()});
                nbt.putInt("Terminal", blockEndpoint.getTerminal());
                stack.setNbt(nbt);
            }
        });
    }
}
