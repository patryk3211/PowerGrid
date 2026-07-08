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
package org.patryk3211.powergrid.compat;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.Set;
import java.util.function.Predicate;

public class HiddenItems {
    private static final Set<Item> HIDDEN = new Builder()
            .add(ModdedItems.INCOMPLETE_ELECTRICAL_GIZMO)
            .add(ModdedItems.INCOMPLETE_UNETCHED_CIRCUIT)
            .add(ModdedItems.INCOMPLETE_TRANSFORMER_CORE)
            .add(ModdedItems.INCOMPLETE_PUNCH_CARD)
            .add(ModdedItems.INCOMPLETE_BATTERY)
            .add(ModdedItems.INCOMPLETE_BJT_NPN)
            .add(ModdedItems.INCOMPLETE_BJT_PNP)
            .add(ModdedItems.INCOMPLETE_SOLAR_PANEL)
            .add(ModdedItems.PORTABLE_BATTERY_PLACEABLE)
            .build();

    public static Predicate<Item> getHiddenPredicate() {
        return HIDDEN::contains;
    }

    private static class Builder {
        private final Set<Item> items = new ReferenceOpenHashSet<>();

        public Builder() {

        }

        public Builder add(ItemLike itemConvertible) {
            items.add(itemConvertible.asItem());
            return this;
        }

        public Set<Item> build() {
            return items;
        }
    }
}
