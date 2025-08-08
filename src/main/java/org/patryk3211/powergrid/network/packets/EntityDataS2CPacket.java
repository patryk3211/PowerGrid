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
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import org.patryk3211.powergrid.network.ClientBoundPackets;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class EntityDataS2CPacket implements SimplePacket {
    public final int entityId;
    public final NbtCompound data;

    public EntityDataS2CPacket(Entity entity, NbtCompound data) {
        entityId = entity.getId();
        this.data = data;
    }

    public EntityDataS2CPacket(PacketByteBuf buffer) {
        entityId = buffer.readInt();
        data = buffer.readNbt();
    }

    @Override
    public void encode(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeNbt(data);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var world = ClientBoundPackets.world();
            var entity = world.getEntityById(entityId);
            if(entity instanceof IConsumer consumer)
                consumer.onEntityDataPacket(data);
        });
    }

    //    public Packet<ClientPlayPacketListener> packet() {
//        return ServerPlayNetworking.createS2CPacket(ModdedPackets.ENTITY_DATA_PACKET, this.buffer);
//    }

//    public void send() {
//        if(entity == null)
//            throw new IllegalStateException();
//        for(var player : PlayerLookup.tracking(entity)) {
//            ServerPlayNetworking.send(player, ModdedPackets.ENTITY_DATA_PACKET, buffer);
//        }
//    }

    public interface IConsumer {
        void onEntityDataPacket(NbtCompound packet);
    }
}
