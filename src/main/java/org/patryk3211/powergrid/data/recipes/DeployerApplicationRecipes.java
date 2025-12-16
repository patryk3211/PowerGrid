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
package org.patryk3211.powergrid.data.recipes;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.DeployingRecipeGen;
import net.minecraft.data.PackOutput;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedItems;

@SuppressWarnings("unused")
public class DeployerApplicationRecipes extends DeployingRecipeGen {
    GeneratedRecipe

    SCHEMATIC_COPY = create("schematic_copy", b -> b
            .require(AllItems.EMPTY_SCHEMATIC)
            .require(ModdedItems.CIRCUIT_SCHEMATIC)
            .toolNotConsumed()
            .output(ModdedItems.CIRCUIT_SCHEMATIC)
    ),

    STRING_LIGHT_CORD = create("string_light_cord", b -> b
            .require(ModdedItems.CORD)
            .require(ModdedItems.LIGHT_BULB)
            .output(ModdedItems.STRING_LIGHT_CORD)
    );

    public DeployerApplicationRecipes(PackOutput generator) {
        super(generator, PowerGrid.MOD_ID);
    }
}
