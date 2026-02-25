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
package org.patryk3211.powergrid.data.recipes;

import com.simibubi.create.api.data.recipe.CuttingRecipeGen;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CuttingRecipes extends CuttingRecipeGen {
    GeneratedRecipe

    COPPER_WIRE = create("copper_wire_cutting", b ->
            b.require(RecipeTags.copperSheet()).duration(50).output(ModdedItems.WIRE.get(), 4)),

    IRON_WIRE = create("iron_wire_cutting", b ->
            b.require(RecipeTags.ironSheet()).duration(50).output(ModdedItems.IRON_WIRE.get(), 4)),

    GOLD_WIRE = create("gold_wire_cutting", b ->
            b.require(RecipeTags.goldSheet()).duration(50).output(ModdedItems.GOLDEN_WIRE.get(), 4)),

    EMPTY_CIRCUIT = create("empty_circuit_slabs", b -> b
            .output(ModdedItems.EMPTY_CIRCUIT, 2)
            .require(ItemTags.WOODEN_SLABS)
            .duration(50))

            ;

    public CuttingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> future) {
        super(output, future, PowerGrid.MOD_ID);
    }
}
