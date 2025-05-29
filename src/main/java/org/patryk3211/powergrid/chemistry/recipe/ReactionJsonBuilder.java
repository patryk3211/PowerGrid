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

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.tterrag.registrate.providers.DataGenContext;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.reagent.*;
import org.patryk3211.powergrid.chemistry.recipe.condition.RecipeCatalyzerCondition;
import org.patryk3211.powergrid.chemistry.recipe.condition.RecipeConcentrationCondition;
import org.patryk3211.powergrid.chemistry.recipe.condition.RecipeTemperatureCondition;
import org.patryk3211.powergrid.chemistry.recipe.equation.ConstEquation;
import org.patryk3211.powergrid.chemistry.recipe.equation.MapAggregateEquation;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ReactionJsonBuilder implements RecipeJsonProvider, ReactionRateEquationBuilder.Parent {
    private final String namespace;
    private String path;

    private final ReactionRecipe.RecipeConstructorParameters params = new ReactionRecipe.RecipeConstructorParameters();

    public ReactionJsonBuilder(Identifier id) {
        this.namespace = id.getNamespace();
        this.path = id.getPath();
    }

    public ReactionJsonBuilder(String namespace, String path) {
        this.namespace = namespace;
        this.path = path;
    }

    public static <T extends Reagent> ReactionJsonBuilder create(DataGenContext<Reagent, T> result, int amount) {
        return new ReactionJsonBuilder(result.getId()).result(result.getEntry(), amount);
    }

    public ReactionJsonBuilder ingredient(ReagentConvertible reagent, int amount) {
        params.ingredients.add(ReagentIngredient.fromReagent(reagent, amount));
        return this;
    }

    public ReactionJsonBuilder result(ReagentConvertible reagent, int amount) {
        params.results.add(new ReagentStack(reagent, amount));
        return this;
    }

    public ReactionJsonBuilder minimumTemperatureCondition(float min) {
        return temperatureCondition(min, null);
    }

    public ReactionJsonBuilder maximumTemperatureCondition(float max) {
        return temperatureCondition(null, max);
    }

    public ReactionJsonBuilder temperatureCondition(@Nullable Float min, @Nullable Float max) {
        params.conditions.add(new RecipeTemperatureCondition(Optional.ofNullable(min), Optional.ofNullable(max)));
        return this;
    }

    public ReactionJsonBuilder temperatureCondition(float min, float max) {
        return temperatureCondition((Float) min, (Float) max);
    }

    public ReactionJsonBuilder concentrationCondition(ReagentConvertible reagent, @Nullable Float min, @Nullable Float max) {
        return concentrationCondition(reagent, min, max, null);
    }

    public ReactionJsonBuilder concentrationCondition(ReagentConvertible reagent, @Nullable Float min, @Nullable Float max, @Nullable ReagentState inState) {
        params.conditions.add(new RecipeConcentrationCondition(reagent.asReagent(), Optional.ofNullable(min), Optional.ofNullable(max), Optional.ofNullable(inState)));
        return this;
    }

    public ReactionJsonBuilder catalyzerCondition(float strength) {
        params.conditions.add(new RecipeCatalyzerCondition(strength));
        return this;
    }

    public ReactionJsonBuilder energy(float energy) {
        params.energy = energy;
        return this;
    }

    public ReactionJsonBuilder flag(ReactionFlag flag) {
        params.flags.set(flag.getBit());
        return this;
    }

    public ReactionJsonBuilder flag(ReactionFlag... flags) {
        for(var flag : flags)
            flag(flag);
        return this;
    }

    public ReactionJsonBuilder suffix(String suffix) {
        this.path += suffix;
        return this;
    }

    public ReactionJsonBuilder rate(int rate) {
        params.rate = new ConstEquation(rate);
        return this;
    }

    public ReactionRateEquationBuilder<ReactionJsonBuilder> rate() {
        return new ReactionRateEquationBuilder<>(this);
    }

    public void use(ReactionRateEquationBuilder<?> builder) {
        params.rate = new MapAggregateEquation(builder.elements);
    }

    @Override
    public void serialize(JsonObject json) {
        var recipe = new ReactionRecipe(getRecipeId(), params);
        var result = ReactionRecipeSerializer.CODEC.encode(recipe, JsonOps.INSTANCE, new JsonObject());
        if(result.result().isEmpty())
            throw new RuntimeException("Recipe serialization failed");

        // Copy result json to output object.
        var resultJson = (JsonObject) result.result().get();
        for(var entry : resultJson.entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Identifier getRecipeId() {
        return new Identifier(namespace, "reactions/" + path);
    }

    @Override
    public RecipeSerializer<ReactionRecipe> getSerializer() {
        return ReactionRecipeSerializer.INSTANCE;
    }

    @Override
    public @Nullable JsonObject toAdvancementJson() {
        return null;
    }

    @Override
    public @Nullable Identifier getAdvancementId() {
        return null;
    }

    public void offerTo(Consumer<RecipeJsonProvider> consumer) {
        consumer.accept(this);
    }
}

