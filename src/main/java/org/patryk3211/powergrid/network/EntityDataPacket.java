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

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import org.patryk3211.powergrid.collections.ModdedPackets;

public class EntityDataPacket {
    public final int entityId;
    public final PacketByteBuf buffer;

    public EntityDataPacket(Entity entity) {
        buffer = PacketByteBufs.create();
        entityId = entity.getId();
        buffer.writeInt(entityId);
    }

    public EntityDataPacket(PacketByteBuf buffer) {
        entityId = buffer.readInt();
        this.buffer = buffer;
    }

    public Packet<ClientPlayPacketListener> packet() {
        return ServerPlayNetworking.createS2CPacket(ModdedPackets.ENTITY_DATA_PACKET, this.buffer);
    }

    public interface IConsumer {
        void onEntityDataPacket(EntityDataPacket packet);
    }
}
