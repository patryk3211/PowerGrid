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
import net.minecraft.network.FriendlyByteBuf;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class SaveCardC2SPacket implements SimplePacket {
    private final byte[] data;

    public SaveCardC2SPacket(byte[] data) {
        this.data = data;
    }

    public SaveCardC2SPacket(FriendlyByteBuf buf) {
        data = new byte[16];
        buf.readBytes(data);
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeBytes(data);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        var ctx = context.get();
        ctx.queue(() -> {
            var player = ctx.getPlayer();
            var stack = player.getMainHandItem();
            if(!(stack.getItem() instanceof PunchCardItem))
                return;
            stack.getOrCreateTag().putByteArray("Data", data);
        });
    }
}
