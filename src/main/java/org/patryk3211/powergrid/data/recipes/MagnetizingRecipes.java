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

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.StandardProcessingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class MagnetizingRecipes extends StandardProcessingRecipeGen<MagnetizingRecipe> {
    GeneratedRecipe

    MAGNET = create(() -> Items.IRON_INGOT, b -> b
            .output(ModdedItems.MAGNET)
    ),

    IRON_SEPARATION = create(() -> AllItems.CRUSHED_IRON, b -> b
            .output(Items.IRON_NUGGET, 8)
            .output(0.4f, Items.IRON_NUGGET, 3)
    )
            ;


    protected GeneratedRecipe create(Supplier<ItemLike> singleIngredient, UnaryOperator<StandardProcessingRecipe.Builder<MagnetizingRecipe>> transform) {
        return super.create(PowerGrid.MOD_ID, singleIngredient, transform);
    }

    public MagnetizingRecipes(PackOutput generator, CompletableFuture<HolderLookup.Provider> registries) {
        super(generator, registries, PowerGrid.MOD_ID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return MagnetizingRecipe.TYPE_INFO;
    }
}
