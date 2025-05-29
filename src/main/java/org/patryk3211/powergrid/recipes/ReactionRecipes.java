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
import org.patryk3211.powergrid.chemistry.reagent.ReagentState;
import org.patryk3211.powergrid.chemistry.reagent.Reagents;
import org.patryk3211.powergrid.chemistry.recipe.ReactionFlag;

public class ReactionRecipes extends ReactionRecipeProvider {
    GeneratedRecipe

    SULFUR_COMBUSTION = create("combustion/sulfur", b -> b
            .result(Reagents.SULFUR_DIOXIDE, 1)
            .ingredient(Reagents.SULFUR, 1)
            .ingredient(Reagents.OXYGEN, 2)
            .minimumTemperatureCondition(232)
            .concentrationCondition(Reagents.OXYGEN, 0.1f, null, ReagentState.GAS)
            .flag(ReactionFlag.COMBUSTION)
            .energy(297)
            .rate()
                .multiply(eq -> eq.constant(15).concentration(Reagents.OXYGEN))
                .build()
    ),

    SULFUR_TRIOXIDE_OXIDATION = create("oxidation/sulfur_trioxide", b -> b
            .result(Reagents.SULFUR_TRIOXIDE, 1)
            .ingredient(Reagents.SULFUR_DIOXIDE, 1)
            .ingredient(Reagents.OXYGEN, 1)
            .temperatureCondition(100, 810)
            .catalyzerCondition(1)
            .energy(198)
            .rate()
                .polynomial(eq -> eq.temperature().constant(-0.00005f, 0.465f, -5.0f))
                .build()
    ),

    SULFUR_TRIOXIDE_DECOMPOSITION = create("decomposition/sulfur_trioxide", b -> b
            .result(Reagents.OXYGEN, 1)
            .result(Reagents.SULFUR_DIOXIDE, 1)
            .ingredient(Reagents.SULFUR_TRIOXIDE, 1)
            .minimumTemperatureCondition(300)
            .energy(-198)
            .rate()
                .polynomial(eq -> eq.temperature().constant(0.005f, -1.5f))
                .build()
    ),

    SULFURIC_ACID_SO3 = create("acid/so3_sulfuric_acid", b -> b
            .result(Reagents.SULFURIC_ACID, 1)
            .ingredient(Reagents.SULFUR_TRIOXIDE, 1)
            .ingredient(Reagents.WATER.liquid(), 1)
            .minimumTemperatureCondition(0)
            .energy(101)
            .rate(10)
    ),

    REDSTONE_SULFATE = create("acid/redstone_sulfate", b -> b
            .result(Reagents.REDSTONE_SULFATE, 1)
            .result(Reagents.HYDROGEN, 2)
            .ingredient(Reagents.REDSTONE, 1)
            .ingredient(Reagents.SULFURIC_ACID, 1)
            .minimumTemperatureCondition(0)
            .concentrationCondition(Reagents.SULFURIC_ACID, 0.25f, null, ReagentState.LIQUID)
            .concentrationCondition(Reagents.WATER.liquid(), 0.25f, null, ReagentState.LIQUID)
    )
    ;

    public ReactionRecipes(FabricDataOutput output) {
        super(output);
    }
}
