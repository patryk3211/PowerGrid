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
package org.patryk3211.powergrid.base;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.ServerPlayerAccessor;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static io.github.fabricators_of_create.porting_lib.util.NetworkHooks.OPEN_ID;

public interface IMultiScreenHandlerFactory extends NamedScreenHandlerFactory {
    @Override
    @Nullable
    default ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return createMenu(syncId, playerInventory, player, 0);
    }

    @Nullable
    ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player, int menuIndex);

    /**
     * @see io.github.fabricators_of_create.porting_lib.util.NetworkHooks#openScreen(ServerPlayerEntity, NamedScreenHandlerFactory, Consumer)
     */
    static void openScreen(ServerPlayerEntity player, IMultiScreenHandlerFactory factory, Consumer<PacketByteBuf> extraDataWriter, int menuIndex) {
        player.onHandledScreenClosed();
        ((ServerPlayerAccessor)player).callNextContainerCounter();
        int openContainerId = ((ServerPlayerAccessor)player).getContainerCounter();
        PacketByteBuf extraData = new PacketByteBuf(Unpooled.buffer());
        extraDataWriter.accept(extraData);
        extraData.readerIndex(0);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ScreenHandler menu = factory.createMenu(openContainerId, player.getInventory(), player, menuIndex);
        buf.writeVarInt(Registries.SCREEN_HANDLER.getRawId(menu.getType()));
        buf.writeVarInt(openContainerId);
        buf.writeText(factory.getDisplayName());
        buf.writeVarInt(extraData.readableBytes());
        buf.writeBytes(extraData);
        ServerPlayNetworking.send(player, OPEN_ID, buf);
        player.currentScreenHandler = menu;
        ((ServerPlayerAccessor)player).callInitMenu(menu);
    }
}
