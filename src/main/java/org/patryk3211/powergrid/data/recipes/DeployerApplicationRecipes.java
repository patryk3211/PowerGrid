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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.DeployingRecipeGen;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.equipment.BoostRecipe;

@SuppressWarnings("unused")
public class DeployerApplicationRecipes extends DeployingRecipeGen {
    GeneratedRecipe

    SCHEMATIC_COPY = create("schematic_copy", b -> b
            .require(AllItems.EMPTY_SCHEMATIC)
            .require(ModdedItems.CIRCUIT_SCHEMATIC)
            .toolNotConsumed()
            .output(ModdedItems.CIRCUIT_SCHEMATIC)
    ),

    STRING_LIGHT_CORD = create("string_light_cord", b -> b
            .require(ModdedItems.CORD)
            .require(ModdedItems.LIGHT_BULB)
            .output(ModdedItems.STRING_LIGHT_CORD)
    ),

    COPPER_PLATING = create("copper_plating", b -> b
            .require(Items.COPPER_BLOCK)
            .require(RecipeTags.copperSheet())
            .output(ModdedBlocks.COPPER_PLATING)
    ),

    CEILING_TILE = create("ceiling_tile", b -> b
            .require(AllBlocks.COPYCAT_PANEL)
            .require(AllItems.CARDBOARD)
            .output(ModdedBlocks.CEILING_TILE)
    ),

    BOOST_DRILL = boost(ModdedItems.PORTABLE_DRILL),
    BOOST_SAW = boost(ModdedItems.PORTABLE_SAW),
    BOOST_ZAPPER = boost(ModdedItems.ELECTROZAPPER),
    BOOST_BATON = boost(ModdedItems.ELECTROBATON);

    public DeployerApplicationRecipes(PackOutput generator) {
        super(generator, PowerGrid.MOD_ID);
    }

    private GeneratedRecipe boost(ItemEntry<?> boostItem) {
        GeneratedRecipe generatedRecipe =
                c -> new ProcessingRecipeBuilder<>(BoostRecipe::new, boostItem.getId().withSuffix("_boosting"))
                        .require(boostItem)
                        .require(ModdedItems.INTEGRATED_CIRCUIT)
                        .output(boostItem)
                        .build(c);
        all.add(generatedRecipe);
        return generatedRecipe;
    }
}
