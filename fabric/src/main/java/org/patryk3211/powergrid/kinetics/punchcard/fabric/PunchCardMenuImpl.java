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
package org.patryk3211.powergrid.kinetics.punchcard.fabric;

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardMenu;

public class PunchCardMenuImpl extends PunchCardMenu {
    public PunchCardMenuImpl(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public PunchCardMenuImpl(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        return new ItemStackHandler(0);
    }

    public static PunchCardMenuConstructors constructors() {
        return new PunchCardMenuConstructors() {
            @Override
            public PunchCardMenu create(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf buf) {
                return new PunchCardMenuImpl(type, id, inv, buf);
            }

            @Override
            public PunchCardMenu create(MenuType<?> type, int id, Inventory inv, ItemStack holder) {
                return new PunchCardMenuImpl(type, id, inv, holder);
            }
        };
    }
}
