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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandlerType;
import org.patryk3211.powergrid.collections.ModdedMenus;

public class CircuitDesignTableEditMenu extends AbstractCircuitDesignTableMenu {
    public CircuitDesignTableEditMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, CircuitDesignTableBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    public CircuitDesignTableEditMenu(ScreenHandlerType<?> type, int id, PlayerInventory inv, PacketByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public static CircuitDesignTableEditMenu create(int id, PlayerInventory inv, CircuitDesignTableBlockEntity be) {
        return new CircuitDesignTableEditMenu(ModdedMenus.CIRCUIT_DESIGN_BENCH_EDIT.get(), id, inv, be);
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(3, 182);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
