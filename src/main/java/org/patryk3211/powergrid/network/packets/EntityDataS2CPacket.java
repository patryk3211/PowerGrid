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

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.patryk3211.powergrid.collections.ModdedPackets;

import java.util.List;

public class EntityDataS2CPacket {
    private Entity entity;
    public final int entityId;
    public final PacketByteBuf buffer;
    public final byte type;

    public EntityDataS2CPacket(Entity entity, int type) {
        this.entity = entity;
        this.type = (byte) type;
        buffer = PacketByteBufs.create();
        entityId = entity.getId();
        buffer.writeInt(entityId);
        buffer.writeByte(type);
    }

    public EntityDataS2CPacket(PacketByteBuf buffer) {
        entityId = buffer.readInt();
        type = buffer.readByte();
        this.buffer = buffer;
    }

    public Packet<ClientPlayPacketListener> packet() {
        return ServerPlayNetworking.createS2CPacket(ModdedPackets.ENTITY_DATA_PACKET, this.buffer);
    }

    public void send() {
        if(entity == null)
            throw new IllegalStateException();
        for(var player : PlayerLookup.tracking(entity)) {
            ServerPlayNetworking.send(player, ModdedPackets.ENTITY_DATA_PACKET, buffer);
        }
    }

    public interface IConsumer {
        void onEntityDataPacket(EntityDataS2CPacket packet);
    }
}
