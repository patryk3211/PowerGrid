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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.patryk3211.powergrid.network.SimplePacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class EntityDataS2CPacket implements SimplePacket {
    private static final Map<Integer, CompoundTag> DEFERRED_DATA = new HashMap<>();
    public final int entityId;
    public final CompoundTag data;

    public EntityDataS2CPacket(Entity entity, CompoundTag data) {
        entityId = entity.getId();
        this.data = data;
    }

    public EntityDataS2CPacket(FriendlyByteBuf buffer) {
        entityId = buffer.readInt();
        data = buffer.readNbt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeNbt(data);
    }

    @Override
    public void handle(Supplier<NetworkManager.PacketContext> context) {
        context.get().queue(() -> {
            var world = ClientSideAccess.world();
            var entity = world.getEntity(entityId);
            if(entity == null) {
                DEFERRED_DATA.put(entityId, data);
                return;
            }
            if(entity instanceof IConsumer consumer)
                consumer.onEntityDataPacket(data);
        });
    }

    public static void clientEntityAdded(Entity entity) {
        var tag = DEFERRED_DATA.remove(entity.getId());
        if(tag == null)
            return;
        if(entity instanceof IConsumer consumer)
            consumer.onEntityDataPacket(tag);
    }

    public interface IConsumer {
        void onEntityDataPacket(CompoundTag packet);
    }
}
