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
package org.patryk3211.powergrid.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import org.patryk3211.powergrid.collections.ModdedPackets;

import java.util.function.Supplier;

public interface SimplePacket {
    void encode(FriendlyByteBuf buf);

    void handle(Supplier<NetworkManager.PacketContext> context);

    default Packet<ClientGamePacketListener> clientBoundPacket() {
        return (Packet<ClientGamePacketListener>) ModdedPackets.getChannel().toPacket(NetworkManager.Side.S2C, this);
    }

    default Packet<ServerGamePacketListener> serverBoundPacket() {
        return (Packet<ServerGamePacketListener>) ModdedPackets.getChannel().toPacket(NetworkManager.Side.C2S, this);
    }
}
