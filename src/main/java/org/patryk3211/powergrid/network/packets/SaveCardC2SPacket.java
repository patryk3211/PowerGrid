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

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.collections.ModdedComponentTypes;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardItem;
import org.patryk3211.powergrid.network.C2SPacket;

public class SaveCardC2SPacket implements C2SPacket {
    private final byte[] data;
    private final String name;
    private final boolean lock;

    public SaveCardC2SPacket(byte[] data, @NotNull String name, boolean lock) {
        this.data = data;
        this.name = name;
        this.lock = lock;
    }

    public SaveCardC2SPacket(FriendlyByteBuf buf) {
        data = new byte[16];
        buf.readBytes(data);
        name = buf.readUtf(35);
        lock = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBytes(data);
        buf.writeUtf(name);
        buf.writeBoolean(lock);
    }

    @Override
    public void handle(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if(!(stack.getItem() instanceof PunchCardItem))
            return;
        if(stack.get(ModdedComponentTypes.LOCKED))
            return;
        stack.set(ModdedComponentTypes.DATA, data);
        if(name.isEmpty()) {
            stack.remove(DataComponents.ITEM_NAME);
        } else {
            stack.set(DataComponents.ITEM_NAME, Component.literal(name));
        }
        if(lock) {
            stack.set(ModdedComponentTypes.LOCKED, true);
            stack.set(ModdedComponentTypes.AUTHOR, player.getDisplayName().getString());
        }
    }
}
