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

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IMultiScreenHandlerFactory extends NamedScreenHandlerFactory {
    @Override
    @Nullable
    default ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return createMenu(syncId, playerInventory, player, 0);
    }

    @Nullable
    ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player, int menuIndex);

    static void openScreen(ServerPlayerEntity player, IMultiScreenHandlerFactory factory, Consumer<PacketByteBuf> extraDataWriter, int menuIndex) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(PacketByteBuf buf) {
                extraDataWriter.accept(buf);
            }

            @Override
            public Text getDisplayName() {
                return factory.getDisplayName();
            }

            @Override
            public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return factory.createMenu(syncId, playerInventory, player, menuIndex);
            }
        });
    }
}
