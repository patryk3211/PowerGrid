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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.collections.ModdedMenus;

public class CircuitDesignTableEditMenu extends AbstractCircuitDesignTableMenu {
    public CircuitDesignTableEditMenu(MenuType<?> type, int id, Inventory inv, CircuitDesignTableBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public CircuitDesignTableEditMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public static CircuitDesignTableEditMenu create(int id, Inventory inv, CircuitDesignTableBlockEntity be) {
        return new CircuitDesignTableEditMenu(ModdedMenus.CIRCUIT_DESIGN_BENCH_EDIT.get(), id, inv, be);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(3, 182);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
