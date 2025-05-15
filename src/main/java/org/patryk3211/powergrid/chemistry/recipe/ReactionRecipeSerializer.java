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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.reagent.ReagentIngredient;
import org.patryk3211.powergrid.chemistry.reagent.ReagentStack;

public class ReactionRecipeSerializer implements RecipeSerializer<ReactionRecipe> {
    public static final Identifier ID = new Identifier(PowerGrid.MOD_ID, "reaction");
    public static final ReactionRecipeSerializer INSTANCE = new ReactionRecipeSerializer();

    @Override
    public ReactionRecipe read(Identifier id, JsonObject json) {
        var recipeParameters = new ReactionRecipe.RecipeConstructorParameters();

        // Parse ingredients
        var ingredients = json.getAsJsonArray("ingredients");
        for(var element : ingredients) {
            var ingredient = element.getAsJsonObject();
            if(ingredient.has("reagent")) {
                recipeParameters.ingredients.add(ReagentIngredient.read(ingredient));
            } else {
                PowerGrid.LOGGER.warn("Recipe '{}' contains an invalid ingredient entry", id);
            }
        }

        // Parse conditions
        var conditions = json.getAsJsonArray("conditions");
        for(var element : conditions) {
            var condition = element.getAsJsonObject();
            var conditionObj = switch(condition.get("type").getAsString()) {
                case "temperature" -> new RecipeTemperatureCondition(condition);
                case "concentration" -> new RecipeConcentrationCondition(condition);
                default -> null;
            };
            if(conditionObj == null) {
                PowerGrid.LOGGER.warn("Recipe '{}' contains an invalid condition entry", id);
                continue;
            }
            recipeParameters.conditions.add(conditionObj);
        }

        // Parse results
        var results = json.getAsJsonArray("results");
        for(var element : results) {
            var result = element.getAsJsonObject();
            if(result.has("reagent")) {
                recipeParameters.results.add(ReagentStack.read(result));
            } else {
                PowerGrid.LOGGER.warn("Recipe '{}' contains an invalid result entry", id);
            }
        }

        // Parse flags
        if(json.has("flags")) {
            var flags = json.getAsJsonArray("flags");
            for(var element : flags) {
                var flagName = element.getAsString();
                var flag = ReactionFlag.fromString(flagName);
                recipeParameters.flags.set(flag.getBit(), true);
            }
        }

        // Reaction energy
        if(json.has("energy")) {
            recipeParameters.energy = json.get("energy").getAsInt();
        }

        // Maximum reaction rate
        if(json.has("rate")) {
            recipeParameters.rate = json.get("rate").getAsInt();
        }

        return new ReactionRecipe(id, recipeParameters);
    }

    @Override
    public ReactionRecipe read(Identifier id, PacketByteBuf buf) {
        var params = new ReactionRecipe.RecipeConstructorParameters();
        params.energy = buf.readInt();
        params.rate = buf.readInt();

        int count = buf.readInt();
        for(int i = 0; i < count; ++i) {
            params.ingredients.add(ReagentIngredient.read(buf));
        }

        count = buf.readInt();
        for(int i = 0; i < count; ++i) {
            params.conditions.add(IReactionCondition.read(buf));
        }

        count = buf.readInt();
        for(int i = 0; i < count; ++i) {
            params.results.add(ReagentStack.read(buf));
        }

        params.flags = buf.readBitSet();

        return new ReactionRecipe(id, params);
    }

    @Override
    public void write(PacketByteBuf buf, ReactionRecipe recipe) {
        buf.writeInt(recipe.getReactionEnergy());
        buf.writeInt(recipe.getReactionRate());

        var ingredients = recipe.getReagentIngredients();
        buf.writeInt(ingredients.size());
        for(var ingredient : ingredients) {
            ingredient.write(buf);
        }

        var conditions = recipe.getReactionConditions();
        buf.writeInt(conditions.size());
        for(var condition : conditions) {
            IReactionCondition.write(condition, buf);
        }

        var results = recipe.getReagentResults();
        buf.writeInt(results.size());
        for(var result : results) {
            result.write(buf);
        }

        buf.writeBitSet(recipe.getFlagBits());
    }
}
