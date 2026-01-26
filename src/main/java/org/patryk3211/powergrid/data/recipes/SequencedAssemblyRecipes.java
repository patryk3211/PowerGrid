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
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

@SuppressWarnings("unused")
public class SequencedAssemblyRecipes extends SequencedAssemblyRecipeGen {
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
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.copperCoil()))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllItems.ELECTRON_TUBE))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModdedItems.INTEGRATED_CIRCUIT))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(Items.GOLD_NUGGET))),

    UNETCHED_CIRCUIT_BOARD = create("unetched_circuit_board", b -> b.require(ModdedItems.CIRCUIT_SCHEMATIC)
            .transitionTo(ModdedItems.INCOMPLETE_UNETCHED_CIRCUIT)
            .addOutput(ModdedItems.UNETCHED_CIRCUIT, 100)
            .loops(2)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModdedItems.EMPTY_CIRCUIT))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllItems.DOUGH))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.copperSheet()))
            .addStep(PressingRecipe::new, rb -> rb)),

    PUNCH_CARD = create("punch_card", b -> b.require(AllItems.CARDBOARD)
            .transitionTo(ModdedItems.INCOMPLETE_PUNCH_CARD)
            .addOutput(ModdedItems.PUNCH_CARD, 100)
            .addOutput(Items.PAPER, 5)
            .addOutput(AllItems.CARDBOARD, 5)
            .loops(2)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(Items.PAPER))
            .addStep(PressingRecipe::new, rb -> rb)),

    BJT_NPN = create("bjt_npn", b -> b.require(RecipeTags.ironSheet())
            .transitionTo(ModdedItems.INCOMPLETE_BJT_NPN)
            .addOutput(ModdedItems.BJT_NPN, 100)
            .addOutput(ModdedItems.BJT_PNP, 10)
            .addOutput(AllItems.IRON_SHEET, 3)
            .addOutput(AllItems.ELECTRON_TUBE, 3)
            .loops(1)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.polishedRoseQuartz()))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.goldWire()))
            .addStep(PressingRecipe::new, rb -> rb)),

    BJT_PNP = create("bjt_pnp", b -> b.require(RecipeTags.ironSheet())
            .transitionTo(ModdedItems.INCOMPLETE_BJT_PNP)
            .addOutput(ModdedItems.BJT_PNP, 100)
            .addOutput(ModdedItems.BJT_NPN, 10)
            .addOutput(AllItems.IRON_SHEET, 3)
            .addOutput(AllItems.ELECTRON_TUBE, 3)
            .loops(1)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.goldWire()))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.polishedRoseQuartz()))
            .addStep(PressingRecipe::new, rb -> rb))

            ;

    public SequencedAssemblyRecipes(PackOutput output) {
        super(output, PowerGrid.MOD_ID);
    }

    @Override
    public String getName() {
        return "Power Grid's Sequenced Assembly Recipes";
    }
}
