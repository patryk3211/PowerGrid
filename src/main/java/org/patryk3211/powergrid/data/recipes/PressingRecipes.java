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

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class PressingRecipes<P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>, B extends ProcessingRecipeBuilder<P, R, B>> extends ProcessingRecipeGen<P,R,B> {
    GeneratedRecipe

    ZINC_SHEET = create("zinc_sheet", b -> b
            .require(RecipeTags.zincIngot())
            .output(ModdedItems.ZINC_SHEET.get())
    );

    public PressingRecipes(PackOutput generator, CompletableFuture<HolderLookup.Provider> registries) {
        super(generator, registries, PowerGrid.MOD_ID);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.PRESSING;
    }

    @Override
    protected B getBuilder(ResourceLocation id) {
        return null;
    }
}
