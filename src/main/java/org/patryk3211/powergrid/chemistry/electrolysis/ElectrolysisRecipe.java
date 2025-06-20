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
package org.patryk3211.powergrid.chemistry.electrolysis;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentIngredient;

import java.util.List;

public class ElectrolysisRecipe implements Recipe<Inventory> {
    public static final Identifier ID = new Identifier(PowerGrid.MOD_ID, "electrolysis");
    public static final RecipeType<ElectrolysisRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return ID.toString();
        }
    };

    private Identifier id;
    private final List<ReagentIngredient> ingredients;
    private final List<ElectrolysisResult> results;
    private final float minimumPotential;

    public ElectrolysisRecipe(List<ReagentIngredient> ingredients, List<ElectrolysisResult> results, float minimumPotential) {
        this.ingredients = ingredients;
        this.results = results;
        this.minimumPotential = minimumPotential;
    }

    public void setId(Identifier id) {
        this.id = id;
    }

    @Override
    public boolean matches(Inventory inventory, World world) {
        return false;
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager registryManager) {
        return null;
    }

    @Override
    public boolean fits(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return null;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ElectrolysisRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public List<ReagentIngredient> getReagentIngredients() {
        return ingredients;
    }

    public List<ElectrolysisResult> getReagentResults() {
        return results;
    }

    public float getMinimumPotential() {
        return minimumPotential;
    }
}
