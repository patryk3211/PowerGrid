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
package org.patryk3211.powergrid.chemistry.reagent.source;

import net.minecraft.item.Item;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.Reagent;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;

import java.util.HashMap;
import java.util.Map;

public record ReagentSource(Item item, Reagent reagent, int amount) {
    private static DynamicRegistryManager cachedManager = null;
    private static final Map<Item, ReagentSource> cache = new HashMap<>();

    public ReagentStack defaultStack() {
        return new ReagentStack(reagent, amount, 22);
    }

    public static ReagentSource get(World world, Item item) {
        if(world.isClient)
            PowerGrid.LOGGER.warn("Reagent source fetch should not occur on client world!");
        var manager = world.getRegistryManager();
        if(cachedManager != manager) {
            cache.clear();
            cachedManager = manager;
        }
        var source = cache.get(item);
        if(!cache.containsKey(item)) {
            var registry = manager.get(ReagentSourceRegistry.REGISTRY_KEY);
            for(var registeredSource : registry) {
                if(registeredSource.item == item) {
                    source = registeredSource;
                    break;
                }
            }
            cache.put(item, source);
        }
        return source;
    }
}
