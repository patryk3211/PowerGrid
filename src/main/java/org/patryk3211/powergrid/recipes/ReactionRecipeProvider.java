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

import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.chemistry.recipe.ReactionJsonBuilder;

import java.util.function.UnaryOperator;

public abstract class ReactionRecipeProvider extends CreateRecipeProvider {
    public ReactionRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    GeneratedRecipe create(String name, UnaryOperator<ReactionJsonBuilder> transform) {
        return create(new Identifier(PowerGrid.MOD_ID, name), transform);
    }

    protected GeneratedRecipe create(Identifier id, UnaryOperator<ReactionJsonBuilder> transform) {
        GeneratedRecipe generatedRecipe = c -> transform.apply(new ReactionJsonBuilder(id)).offerTo(c);
        all.add(generatedRecipe);
        return generatedRecipe;
    }

    @Override
    public String getName() {
        return "Reaction Recipes";
    }
}
