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

import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public abstract class AbstractCircuitDesignTableMenu extends MenuBase<CircuitDesignTableBlockEntity> {
    protected AbstractCircuitDesignTableMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    protected AbstractCircuitDesignTableMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, CircuitDesignTableBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected CircuitDesignTableBlockEntity createOnClient(PacketByteBuf extraData) {
        var world = MinecraftClient.getInstance().world;
        var be = world.getBlockEntity(extraData.readBlockPos());
        if(be instanceof CircuitDesignTableBlockEntity bench) {
            bench.readClient(extraData.readNbt());
            return bench;
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
    protected void initAndReadInventory(CircuitDesignTableBlockEntity contentHolder) {

    }

    @Override
    protected void saveData(CircuitDesignTableBlockEntity contentHolder) {

    }
}
