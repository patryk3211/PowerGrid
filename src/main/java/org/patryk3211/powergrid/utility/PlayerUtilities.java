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
package org.patryk3211.powergrid.utility;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PlayerUtilities {
    public static boolean hasEnoughItems(PlayerEntity player, Item item, int requiredCount) {
        if(player.isCreative())
            return true;
        var inv = player.getInventory();
        return inv.count(item) >= requiredCount;
    }

    public static boolean hasEnoughItems(@Nullable PlayerEntity player, ItemStack usedStack, int requiredCount) {
        if(player != null)
            return hasEnoughItems(player, usedStack.getItem(), requiredCount);
        return usedStack.getCount() >= requiredCount;
    }

    public static void removeItems(PlayerEntity player, Item item, int count) {
        if(player.isCreative())
            return;
        var inv = player.getInventory();
        Inventories.remove(inv, stack -> stack.isOf(item), count, false);
    }

    public static void removeItems(@Nullable PlayerEntity player, ItemStack usedStack, int count) {
        if(player != null) {
            removeItems(player, usedStack.getItem(), count);
            return;
        }
        usedStack.decrement(Math.min(count, usedStack.getCount()));
    }
}
