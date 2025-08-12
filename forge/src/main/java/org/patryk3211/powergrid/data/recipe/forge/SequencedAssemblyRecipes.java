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
package org.patryk3211.powergrid.data.recipe.forge;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import net.minecraft.data.PackOutput;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedFluids;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.data.FluidConstants;
import org.patryk3211.powergrid.data.recipes.RecipeTags;

@SuppressWarnings("unused")
public class SequencedAssemblyRecipes extends SequencedAssemblyRecipeGen {
    GeneratedRecipe

    BATTERY = create("battery", b -> b.require(ModdedBlocks.CONDUCTIVE_CASING)
            .transitionTo(ModdedItems.INCOMPLETE_BATTERY)
            .addOutput(ModdedBlocks.BATTERY, 100)
            .addOutput(ModdedBlocks.CONDUCTIVE_CASING, 5)
            .addOutput(AllItems.COPPER_SHEET, 2)
            .addOutput(ModdedItems.ZINC_SHEET, 2)
            .loops(3)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.copperSheet()))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.zincSheet()))
            .addStep(FillingRecipe::new, rb -> rb.require(ModdedFluids.acid(), FluidConstants.BOTTLE)))

            ;

    public SequencedAssemblyRecipes(PackOutput output) {
        super(output, PowerGrid.MOD_ID);
    }

//    @Override
//    public String getName() {
//        return "Power Grid's Platform Sequenced Assembly Recipes";
//    }
}
