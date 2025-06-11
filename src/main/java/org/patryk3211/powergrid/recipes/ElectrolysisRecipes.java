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

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import org.patryk3211.powergrid.chemistry.reagent.Reagents;

public class ElectrolysisRecipes extends ElectrolysisRecipeProvider {
    GeneratedRecipe

    WATER = create(Reagents.WATER.liquid(), b -> b
            .result(Reagents.HYDROGEN, 2, true)
            .result(Reagents.OXYGEN, 1, false)
            .minimumPotential(1.5f)
    )

    ;

    public ElectrolysisRecipes(FabricDataOutput output) {
        super(output);
    }
}
