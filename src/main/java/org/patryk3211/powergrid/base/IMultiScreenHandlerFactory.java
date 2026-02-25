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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IMultiScreenHandlerFactory extends MenuProvider {
    @Override
    @Nullable
    default AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return createMenu(syncId, playerInventory, player, 0);
    }

    @Nullable
    AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player, int menuIndex);

    static void openScreen(ServerPlayer player, IMultiScreenHandlerFactory factory, Consumer<RegistryFriendlyByteBuf> extraDataWriter, int menuIndex) {
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(FriendlyByteBuf buf) {
                var regBuf = new RegistryFriendlyByteBuf(buf, player.registryAccess());
                extraDataWriter.accept(regBuf);
            }

            @Override
            public Component getDisplayName() {
                return factory.getDisplayName();
            }

            @Override
            public @Nullable AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                return factory.createMenu(syncId, playerInventory, player, menuIndex);
            }
        });
    }
}
