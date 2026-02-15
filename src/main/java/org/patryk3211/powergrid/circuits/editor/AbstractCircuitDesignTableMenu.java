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
package org.patryk3211.powergrid.circuits.editor;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public abstract class AbstractCircuitDesignTableMenu<T extends SmartBlockEntity> extends MenuBase<T> {
    protected AbstractCircuitDesignTableMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    protected AbstractCircuitDesignTableMenu(MenuType<?> type, int id, Inventory inv, T contentHolder) {
        super(type, id, inv, contentHolder);
    }

    protected abstract Class<T> clazz();

    @Override
    protected T createOnClient(RegistryFriendlyByteBuf extraData) {
        var world = Minecraft.getInstance().level;
        var be = world.getBlockEntity(extraData.readBlockPos());
        if(clazz().isInstance(be)) {
            ((T) be).readClient(extraData.readNbt(), extraData.registryAccess());
            return (T) be;
        }
        return null;
    }

    protected void addPlayerSlots(int xOffset, int yOffset) {
        // Player Inventory
        for(int row = 0; row < 3; ++row) {
            for(int col = 0; col < 9; ++col) {
                addSlot(new Slot(player.getInventory(), col + row * 9 + 9, xOffset + col * 18, yOffset + row * 18));
            }
        }

        for(int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            addSlot(new Slot(player.getInventory(), hotbarSlot, xOffset + hotbarSlot * 18, yOffset + 18 * 3 + 4));
        }

    }

    @Override
    protected void initAndReadInventory(T contentHolder) {

    }

    @Override
    protected void saveData(T contentHolder) {

    }
}
