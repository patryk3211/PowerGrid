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

import com.google.common.collect.Sets;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;

import java.util.Collection;
import java.util.Set;

public class PlayerUtilities {
    public static boolean hasEnoughItems(Player player, Item item, int requiredCount) {
        if(player.isCreative())
            return true;
        var inv = player.getInventory();
        return inv.countItem(item) >= requiredCount;
    }

    public static boolean hasEnoughItems(@Nullable Player player, ItemStack usedStack, int requiredCount) {
        if(player != null)
            return hasEnoughItems(player, usedStack.getItem(), requiredCount);
        return usedStack.getCount() >= requiredCount;
    }

    public static void removeItems(Player player, Item item, int count) {
        if(player.isCreative())
            return;
        var inv = player.getInventory();
        ContainerHelper.clearOrCountMatchingItems(inv, stack -> stack.is(item), count, false);
    }

    public static void removeItems(@Nullable Player player, ItemStack usedStack, int count) {
        if(player != null) {
            removeItems(player, usedStack.getItem(), count);
            return;
        }
        usedStack.shrink(Math.min(count, usedStack.getCount()));
    }

    @NotNull
    public static Collection<ServerPlayer> partialTracking(@NotNull ServerLevel world, @NotNull TransmissionLine line) {
        Set<ServerPlayer> players1, players2;
        players1 = GlobalElectricNetworks.getWorldNetworks(world).getTrackers(line.getNode1().endpoint);
        players2 = GlobalElectricNetworks.getWorldNetworks(world).getTrackers(line.getNode2().endpoint);
        return Sets.symmetricDifference(players1, players2);
    }

    @ExpectPlatform
    public static boolean isFake(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean cancelBreak(Level world, BlockPos pos, Player player) {
        throw new AssertionError();
    }
}
