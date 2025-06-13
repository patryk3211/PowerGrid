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

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.chemistry.reagent.ReagentConvertible;
import org.patryk3211.powergrid.chemistry.reagent.ReagentIngredient;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ElectrolysisJsonBuilder implements RecipeJsonProvider {
    private String namespace;
    private String path;

    private final List<ReagentIngredient> ingredients = new ArrayList<>();
    private final List<ElectrolysisResult> results = new ArrayList<>();
    private float minimumPotential;

    public ElectrolysisJsonBuilder(Identifier id) {
        namespace = id.getNamespace();
        path = id.getPath();
    }

    public ElectrolysisJsonBuilder ingredient(ReagentConvertible reagent, int amount) {
        ingredients.add(ReagentIngredient.fromReagent(reagent, amount));
        return this;
    }

    public ElectrolysisJsonBuilder result(ReagentConvertible reagent, int amount, boolean negative) {
        results.add(new ElectrolysisResult(negative, new ReagentStack(reagent, amount)));
        return this;
    }

    public ElectrolysisJsonBuilder minimumPotential(float potential) {
        minimumPotential = potential;
        return this;
    }

    @Override
    public void serialize(JsonObject json) {
        if(ingredients.isEmpty() || results.isEmpty() || minimumPotential <= 0)
            throw new IllegalStateException("Recipe not fully defined");

        var recipe = new ElectrolysisRecipe(ingredients, results, minimumPotential);
        var result = ElectrolysisRecipeSerializer.CODEC.encode(recipe, JsonOps.INSTANCE, new JsonObject());
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
        return new Identifier(namespace, path);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ElectrolysisRecipeSerializer.INSTANCE;
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
