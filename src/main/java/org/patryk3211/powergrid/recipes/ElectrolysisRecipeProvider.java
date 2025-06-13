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
import org.patryk3211.powergrid.chemistry.electrolysis.ElectrolysisJsonBuilder;
import org.patryk3211.powergrid.chemistry.reagent.ReagentConvertible;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;

import java.util.function.UnaryOperator;

public abstract class ElectrolysisRecipeProvider extends CreateRecipeProvider {
    private final String namespace;

    ElectrolysisRecipeProvider(FabricDataOutput output) {
        this(output, PowerGrid.MOD_ID);
    }

    public ElectrolysisRecipeProvider(FabricDataOutput output, String namespace) {
        super(output);
        this.namespace = namespace;
    }

    protected GeneratedRecipe create(String name, UnaryOperator<ElectrolysisJsonBuilder> transform) {
        return create(new Identifier(namespace, name), transform);
    }

    protected GeneratedRecipe create(Identifier id, UnaryOperator<ElectrolysisJsonBuilder> transform) {
        GeneratedRecipe generatedRecipe = c -> transform.apply(new ElectrolysisJsonBuilder(id)).offerTo(c);
        return register(generatedRecipe);
    }

    protected GeneratedRecipe create(ReagentConvertible singleIngredient, UnaryOperator<ElectrolysisJsonBuilder> transform) {
        return register(c -> transform
                .apply(new ElectrolysisJsonBuilder(ReagentRegistry.REGISTRY.getId(singleIngredient.asReagent()))
                        .ingredient(singleIngredient, 1))
                .offerTo(c));
    }

    @Override
    public String getName() {
        return "Electrolysis Recipes";
    }
}
