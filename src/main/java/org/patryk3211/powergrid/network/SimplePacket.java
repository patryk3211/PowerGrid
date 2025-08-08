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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import org.patryk3211.powergrid.collections.ModdedPackets;

import java.util.function.Supplier;

public interface SimplePacket {
    void encode(PacketByteBuf buf);

    void handle(Supplier<NetworkManager.PacketContext> context);

    default Packet<ClientPlayPacketListener> clientBoundPacket() {
        return (Packet<ClientPlayPacketListener>) ModdedPackets.getChannel().toPacket(NetworkManager.Side.S2C, this);
    }

    default Packet<ServerPlayPacketListener> serverBoundPacket() {
        return (Packet<ServerPlayPacketListener>) ModdedPackets.getChannel().toPacket(NetworkManager.Side.C2S, this);
    }
}
