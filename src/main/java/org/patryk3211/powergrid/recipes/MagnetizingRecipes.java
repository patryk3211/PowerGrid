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

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class MagnetizingRecipes extends ProcessingRecipeGen {
    GeneratedRecipe

    GOLD = create(() -> Items.IRON_INGOT, b -> b
            .output(Items.GOLD_INGOT)
    );

    protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(Supplier<ItemConvertible> singleIngredient, UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
        return super.create(PowerGrid.MOD_ID, singleIngredient, transform);
    }

    public MagnetizingRecipes(FabricDataOutput generator) {
        super(generator);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return MagnetizingRecipe.TYPE_INFO;
    }
}
