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

import com.simibubi.create.AllBlocks;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.compat.HiddenItems;

public class PowerGridREI implements REIClientPlugin {
    private static final Identifier ID = PowerGrid.asResource("rei_plugin");

    @Override
    public String getPluginProviderName() {
        return ID.toString();
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        var circuitDesignCategory = new CircuitDesignCategory();
        var circuitAssemblyCategory = new CircuitAssemblyCategory();

        registry.add(circuitDesignCategory);
        registry.addWorkstations(
                CircuitDesignCategory.IDENTIFIER,
                EntryStack.of(VanillaEntryTypes.ITEM, ModdedBlocks.CIRCUIT_DESIGN_TABLE.asStack())
        );

        registry.add(circuitAssemblyCategory);
        registry.addWorkstations(
                CircuitAssemblyCategory.IDENTIFIER,
                EntryStack.of(VanillaEntryTypes.ITEM, AllBlocks.MECHANICAL_ARM.asStack())
        );
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.add(new CircuitDesignDisplay());
        registry.add(new CircuitAssemblyDisplay());
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entryStack -> {
            if(entryStack.getType() == VanillaEntryTypes.ITEM) {
                ItemStack itemStack = entryStack.castValue();
                return HiddenItems.getHiddenPredicate().test(itemStack.getItem());
            }
            return false;
        });
    }
}
