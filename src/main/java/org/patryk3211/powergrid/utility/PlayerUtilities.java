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
import dev.architectury.event.events.common.TickEvent;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;
import org.patryk3211.powergrid.electricity.sim.special.TransmissionLine;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.WireEndpointType;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @NotNull
    public static Collection<ServerPlayerEntity> partialTracking(@NotNull ServerWorld world, @NotNull TransmissionLine line) {
        if(line.segments.isEmpty())
            return List.of();
        var firstSegment = line.segments.get(0);
        var lastSegment = line.segments.get(line.segments.size() - 1);

        Set<ServerPlayerEntity> players1 = null, players2 = null;
        players1 = Set.copyOf(PlayerLookup.tracking(firstSegment.owner));
        players2 = Set.copyOf(PlayerLookup.tracking(lastSegment.owner));
//        if(line.getNode1() instanceof OwnedFloatingNode owned) {
//            if(owned.endpoint.type() == WireEndpointType.BLOCK) {
//                var pos = ((BlockWireEndpoint) owned.endpoint).getPos();
//                players1 = Set.copyOf(PlayerLookup.tracking(world, pos));
//            }
//        }
//        if(line.getNode2() instanceof OwnedFloatingNode owned) {
//            if(owned.endpoint.type() == WireEndpointType.BLOCK) {
//                var pos = ((BlockWireEndpoint) owned.endpoint).getPos();
//                players2 = Set.copyOf(PlayerLookup.tracking(world, pos));
//            }
//        }
//        if(players1 == null || players2 == null)
//            return List.of();
        return Sets.symmetricDifference(players1, players2);
    }
}
