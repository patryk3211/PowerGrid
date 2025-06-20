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
package org.patryk3211.powergrid.recipes;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.ProcessingRecipeGen;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.function.UnaryOperator;

public class MixingRecipes extends ProcessingRecipeGen {
    GeneratedRecipe

    GOLD_MESH = create("gold_mesh", b -> b
            .require(ModdedItems.GOLDEN_WIRE)
            .require(ModdedItems.GOLDEN_WIRE)
            .require(ModdedItems.GOLDEN_WIRE)
            .require(ModdedItems.GOLDEN_WIRE)
            .require(ModdedItems.GOLDEN_WIRE)
            .require(ModdedItems.GOLDEN_WIRE)
            .output(ModdedItems.GOLDEN_MESH)
    );

    public MixingRecipes(FabricDataOutput output) {
        super(output);
    }

    <T extends ProcessingRecipe<?>> GeneratedRecipe create(String name, UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
        return create(PowerGrid.asResource(name), transform);
    }

    @Override
    protected AllRecipeTypes getRecipeType() {
        return AllRecipeTypes.MIXING;
    }
}
