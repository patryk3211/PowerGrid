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
package org.patryk3211.powergrid.compat.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.ArrayList;
import java.util.List;

public class CircuitDesignDisplay implements Display {
    private final List<EntryIngredient> ingredients = new ArrayList<>();
    private final List<EntryIngredient> output = new ArrayList<>();

    public CircuitDesignDisplay() {
        for(var component : ComponentRegistry.entries()) {
            var stack = EntryStack.of(VanillaEntryTypes.ITEM, component.getRequiredItem().getDefaultStack());
            ingredients.add(EntryIngredient.of(stack));
        }
        var stack = EntryStack.of(VanillaEntryTypes.ITEM, ModdedItems.CIRCUIT_SCHEMATIC.asStack());
        output.add(EntryIngredient.of(stack));
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return ingredients;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return output;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CircuitDesignCategory.IDENTIFIER;
    }
}
