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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.patryk3211.powergrid.electricity.wire.registry.WireRegistry;

public interface IWire {
    static boolean holdsWire(Player player) {
        var stack1 = player.getMainHandItem();
        if(stack1 != null && !stack1.isEmpty() && isWire(player.level(), stack1.getItem()))
            return true;
        var stack2 = player.getOffhandItem();
        if(stack2 != null && !stack2.isEmpty() && isWire(player.level(), stack2.getItem()))
            return true;
        return false;
    }

    static boolean isWire(Level level, Item item) {
        return item instanceof IWire || WireRegistry.forItem(level, item) != null;
    }

    static boolean isCord(Level level, Item item) {
        return isWire(level, item) && WireRegistry.forItem(level, item).cord();
    }
}
