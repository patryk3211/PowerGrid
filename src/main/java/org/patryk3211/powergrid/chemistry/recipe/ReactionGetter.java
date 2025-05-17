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

import net.minecraft.recipe.RecipeManager;
import org.patryk3211.powergrid.chemistry.reagent.mixture.ReagentMixture;

import java.util.ArrayList;
import java.util.List;

public class ReactionGetter {
    /**
     * Get a list of recipes which can potentially be applied from ingredients in the mixture.
     * @param recipeManager Recipe manager
     * @param mixture Mixture of reagents
     * @return List of reactions
     */
    public static List<ReactionRecipe> getPossibleRecipes(RecipeManager recipeManager, ReagentMixture mixture) {
        var possibleRecipes = new ArrayList<ReactionRecipe>();

        var recipes = recipeManager.listAllOfType(ReactionRecipe.TYPE);
        recipes.forEach(recipe -> {
            var ingredients = recipe.getReagentIngredients();
            for(var ingredient : ingredients) {
                if(!mixture.hasReagent(ingredient.getReagent())) {
                    // Mixture doesn't have this reagent so it is not a possible recipe.
                    return;
                }
            }
            // All ingredients are present.
            possibleRecipes.add(recipe);
        });

        return possibleRecipes;
    }

    /**
     * Get a list of recipes which can be applied from ingredients and conditions in the mixture.
     * @param recipeManager Recipe manager
     * @param mixture Mixture of reagents
     * @return List of reactions
     */
    public static List<ReactionRecipe> getValidRecipes(RecipeManager recipeManager, ReagentMixture mixture) {
        var possibleRecipes = getPossibleRecipes(recipeManager, mixture);
        possibleRecipes.removeIf(recipe -> !recipe.test(mixture));
        return possibleRecipes;
    }
}
