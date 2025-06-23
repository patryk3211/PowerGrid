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
package org.patryk3211.powergrid.circuits.components;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import net.minecraft.item.Item;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.*;
import java.util.function.Supplier;

public class Component {
    private static final Map<Item, Component> COMPONENT_MAP = new HashMap<>();

    private Supplier<? extends Item> item;
    private final ComponentFootprint footprint;
    private final ImmutableList<ComponentProperty<?>> properties;

    public Component(ComponentFootprint footprint) {
        this.footprint = footprint;

        var properties = new ImmutableList.Builder<ComponentProperty<?>>();
        addProperties(properties);
        this.properties = properties.build();
    }

    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {

    }

    void setItem(Supplier<? extends Item> item) {
        this.item = item;
    }

    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        return footprint;
    }

    public Item getRequiredItem() {
        return item.get();
    }

    public ImmutableList<ComponentProperty<?>> getProperties() {
        return properties;
    }

    public static Component forItem(Item item) {
        if(COMPONENT_MAP.containsKey(item))
            return COMPONENT_MAP.get(item);
        for(var entry : ComponentRegistry.REGISTRY) {
            if(entry.item.get() == item) {
                COMPONENT_MAP.put(item, entry);
                return entry;
            }
        }
        COMPONENT_MAP.put(item, null);
        return null;
    }
}
