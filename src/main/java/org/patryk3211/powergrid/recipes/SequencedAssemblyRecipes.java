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

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.function.UnaryOperator;

public class SequencedAssemblyRecipes extends CreateRecipeProvider {
    GeneratedRecipe

    TRANSFORMER_CORE = create("transformer_core", b -> b.require(RecipeTags.ironSheet())
            .transitionTo(ModdedItems.INCOMPLETE_TRANSFORMER_CORE.get())
            .addOutput(ModdedBlocks.TRANSFORMER_CORE.get(), 100)
            .addOutput(Items.IRON_DOOR, 5)
            .addOutput(Items.CAULDRON, 3)
            .addOutput(AllItems.IRON_SHEET.get(), 1)
            .addOutput(Items.IRON_INGOT, 1)
            .loops(3)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.ironSheet()))
            .addStep(PressingRecipe::new, rb -> rb)),

    ELECTRICAL_GIZMO = create("electrical_gizmo", b -> b.require(RecipeTags.zincSheet())
            .transitionTo(ModdedItems.INCOMPLETE_ELECTRICAL_GIZMO)
            .addOutput(ModdedItems.ELECTRICAL_GIZMO, 100)
            .addOutput(ModdedItems.ZINC_SHEET, 7)
            .addOutput(AllItems.POLISHED_ROSE_QUARTZ, 5)
            .addOutput(Items.GOLD_NUGGET, 3)
            .addOutput(AllItems.COPPER_NUGGET, 2)
            .addOutput(Items.REPEATER, 3)
            .loops(1)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModdedItems.COPPER_COIL))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllItems.ELECTRON_TUBE))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModdedItems.INTEGRATED_CIRCUIT))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(Items.GOLD_NUGGET)))

            ;

    public SequencedAssemblyRecipes(FabricDataOutput output) {
        super(output);
    }

    protected GeneratedRecipe create(String name, UnaryOperator<SequencedAssemblyRecipeBuilder> transform) {
        GeneratedRecipe recipe = c -> transform.apply(new SequencedAssemblyRecipeBuilder(PowerGrid.asResource(name)))
                .build(c);
        all.add(recipe);
        return recipe;
    }

    @Override
    public String getName() {
        return "Power Grid's Sequenced Assembly Recipes";
    }
}
