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

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.patryk3211.powergrid.network.S2CPacket;
import org.patryk3211.powergrid.utility.ClientSideAccess;

import java.util.HashMap;
import java.util.Map;

public class EntityDataS2CPacket implements S2CPacket {
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

    static CompoundTag takeDeferredData(int entityId) {
        return DEFERRED_DATA.remove(entityId);
    }

    static CompoundTag selectDeferredData(CompoundTag existing, CompoundTag incoming) {
        if(existing != null && existing.contains("Item") && !incoming.contains("Item"))
            return existing;
        return incoming;
    }

    static void deferData(int entityId, CompoundTag data) {
        DEFERRED_DATA.compute(entityId, ($, existing) -> selectDeferredData(existing, data));
    }

    public static void clearDeferredData() {
        DEFERRED_DATA.clear();
    }

    public static void clientEntityAdded(Entity entity) {
        var tag = takeDeferredData(entity.getId());
        if(tag == null)
            return;
        if(entity instanceof IConsumer consumer)
            consumer.onEntityDataPacket(tag);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeNbt(data);
    }

    @Override
    public void handle(Minecraft mc) {
        var world = ClientSideAccess.world();
        var entity = world.getEntity(entityId);
        if(entity == null) {
            deferData(entityId, data);
            return;
        }
        if(entity instanceof IConsumer consumer)
            consumer.onEntityDataPacket(data);
    }

    public interface IConsumer {
        void onEntityDataPacket(CompoundTag packet);
    }
}
