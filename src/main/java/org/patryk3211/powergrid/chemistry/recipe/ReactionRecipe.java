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
package org.patryk3211.powergrid.chemistry.recipe;

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
import org.patryk3211.powergrid.chemistry.reagent.mixture.ReagentMixture;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Predicate;

public class ReactionRecipe implements Recipe<Inventory>, Predicate<ReagentMixture> {
    public static final Identifier ID = new Identifier(PowerGrid.MOD_ID, "reaction");
    public static final RecipeType<ReactionRecipe> TYPE = new RecipeType<>() {
        public String toString() {
            return ID.toString();
        }
    };

    private final Identifier id;
    private final List<ReagentIngredient> ingredients;
    private final List<IReactionCondition> conditions;
    private final List<ReagentStack> results;
    private final BitSet flags;
    private final int energy;
    private final int rate;

    public ReactionRecipe(Identifier id, RecipeConstructorParameters params) {
        this.id = id;
        this.ingredients = params.ingredients;
        this.conditions = params.conditions;
        this.results = params.results;
        this.flags = params.flags;
        this.energy = params.energy;
        this.rate = params.rate;

        var hasTemperature = false;
        for(var condition : conditions) {
            if(condition instanceof RecipeTemperatureCondition) {
                hasTemperature = true;
                break;
            }
        }
        if(!hasTemperature) {
            conditions.add(new RecipeTemperatureCondition());
        }
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
        return ReactionRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public int getReactionEnergy() {
        return energy;
    }

    public int getReactionRate() {
        return rate;
    }

    public List<ReagentIngredient> getReagentIngredients() {
        return ingredients;
    }

    public List<IReactionCondition> getReactionConditions() {
        return conditions;
    }

    public List<ReagentStack> getReagentResults() {
        return results;
    }

    public BitSet getFlagBits() {
        return flags;
    }

    public boolean hasFlag(ReactionFlag flag) {
        return flags.get(flag.getBit());
    }

    @Override
    public boolean test(ReagentMixture mixture) {
        for(var ingredient : ingredients) {
            if(mixture.getAmount(ingredient.getReagent()) < ingredient.getRequiredAmount())
                return false;
        }
        boolean burning = mixture.isBurning();
        for(var condition : conditions) {
            // Temperature condition doesn't apply to burning mixtures and combustion reactions.
            if(condition instanceof RecipeTemperatureCondition && hasFlag(ReactionFlag.COMBUSTION) && burning)
                continue;
            if(!condition.test(mixture))
                return false;
        }
        return true;
    }

    public static class RecipeConstructorParameters {
        public final List<ReagentIngredient> ingredients = new ArrayList<>();
        public final List<IReactionCondition> conditions = new ArrayList<>();
        public final List<ReagentStack> results = new ArrayList<>();
        public BitSet flags = new BitSet();
        public int energy = 0;
        public int rate = 0;

        public RecipeConstructorParameters() { }
    }
}
