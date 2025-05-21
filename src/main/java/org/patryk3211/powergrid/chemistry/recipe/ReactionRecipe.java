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

import com.google.common.collect.ImmutableCollection;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentIngredient;
import org.patryk3211.powergrid.chemistry.reagent.mixture.ReagentMixture;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;
import org.patryk3211.powergrid.chemistry.recipe.condition.IReactionCondition;
import org.patryk3211.powergrid.chemistry.recipe.condition.RecipeTemperatureCondition;
import org.patryk3211.powergrid.chemistry.recipe.equation.ConstEquation;
import org.patryk3211.powergrid.chemistry.recipe.equation.IReactionEquation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ReactionRecipe implements Recipe<Inventory>, Predicate<ReagentMixture> {
    public static final Identifier ID = new Identifier(PowerGrid.MOD_ID, "reaction");
    public static final RecipeType<ReactionRecipe> TYPE = new RecipeType<>() {
        public String toString() {
            return ID.toString();
        }
    };

    private Identifier id;
    private final List<ReagentIngredient> ingredients;
    private final List<IReactionCondition> conditions;
    private final List<ReagentStack> results;
    private final BitSet flags;
    private final float energy;
    private final IReactionEquation rate;

    @NotNull
    private RecipeTemperatureCondition temperatureCondition;

    public ReactionRecipe(Identifier id, RecipeConstructorParameters params) {
        this.id = id;
        this.ingredients = params.ingredients;
        this.results = params.results;
        this.flags = params.flags;
        this.energy = params.energy;
        this.rate = params.rate;

        temperatureCondition = null;
        for(var condition : params.conditions) {
            if(condition instanceof RecipeTemperatureCondition tempCond) {
                temperatureCondition = tempCond;
                break;
            }
        }

        if(temperatureCondition == null) {
            if(params.conditions instanceof ImmutableCollection) {
                conditions = new ArrayList<>(params.conditions);
            } else {
                conditions = params.conditions;
            }
            temperatureCondition = new RecipeTemperatureCondition(Optional.of(0.0f), Optional.empty());
            conditions.add(temperatureCondition);
        } else {
            conditions = params.conditions;
        }
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
        return ReactionRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public float getReactionEnergy() {
        return energy;
    }

    public IReactionEquation getReactionRate() {
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

    public List<ReactionFlag> getFlagList() {
        var list = new ArrayList<ReactionFlag>();
        for(var flag : ReactionFlag.values()) {
            if(hasFlag(flag))
                list.add(flag);
        }
        return list;
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

    public float calculateRate(ReagentConditions conditions, float progressOffset) {
        var maxRate = rate.evaluate(conditions) + progressOffset;
        if(maxRate <= 0)
            return 0;
        if(energy > 0 && temperatureCondition.getMax().isPresent()) {
            var maxDiff = temperatureCondition.getMax().get() - conditions.temperature();
            var deltaT = energy / conditions.heatMass();
            maxRate = (float) Math.min(maxRate, maxDiff / deltaT);
        } else if(energy < 0 && temperatureCondition.getMin().isPresent()) {
            var maxDiff = temperatureCondition.getMin().get() - conditions.temperature();
            var deltaT = energy / conditions.heatMass();
            maxRate = (float) Math.min(maxRate, maxDiff / deltaT);
        }
        return maxRate <= 0 ? 0 : maxRate;
    }

    public static class RecipeConstructorParameters {
        public List<ReagentIngredient> ingredients = new ArrayList<>();
        public List<IReactionCondition> conditions = new ArrayList<>();
        public List<ReagentStack> results = new ArrayList<>();
        public BitSet flags = new BitSet();
        public float energy = 0;
        public IReactionEquation rate = new ConstEquation(1);

        public RecipeConstructorParameters() { }
    }
}
