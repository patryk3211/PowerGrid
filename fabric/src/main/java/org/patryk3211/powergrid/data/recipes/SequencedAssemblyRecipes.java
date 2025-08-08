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

import com.google.gson.JsonObject;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedFluids;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
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
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(Items.GOLD_NUGGET))),

    UNETCHED_CIRCUIT_BOARD = create("unetched_circuit_board", b -> b.require(ModdedItems.CIRCUIT_SCHEMATIC)
            .transitionTo(ModdedItems.INCOMPLETE_UNETCHED_CIRCUIT)
            .addOutput(ModdedItems.UNETCHED_CIRCUIT, 100)
            .loops(2)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(ModdedItems.EMPTY_CIRCUIT))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(AllItems.DOUGH))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.copperSheet()))
            .addStep(PressingRecipe::new, rb -> rb)),

    BATTERY = create("battery", b -> b.require(ModdedBlocks.CONDUCTIVE_CASING)
            .transitionTo(ModdedItems.INCOMPLETE_BATTERY)
            .addOutput(ModdedBlocks.BATTERY, 100)
            .addOutput(ModdedBlocks.CONDUCTIVE_CASING, 5)
            .addOutput(AllItems.COPPER_SHEET, 2)
            .addOutput(ModdedItems.ZINC_SHEET, 2)
            .loops(3)
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.copperSheet()))
            .addStep(DeployerApplicationRecipe::new, rb -> rb.require(RecipeTags.zincSheet()))
            .addStep(FillingRecipe::new, rb -> rb.require(ModdedFluids.ACID.getSource(), FluidConstants.BOTTLE)))

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

    protected GeneratedRecipe createSpecial(RecipeSerializer<?> serializer) {
        GeneratedRecipe recipe = c -> c.accept(new RecipeJsonProvider() {
            @Override
            public void serialize(JsonObject json) {
            }

            @Override
            public Identifier getRecipeId() {
                var serializerId = Registries.RECIPE_SERIALIZER.getId(serializer).getPath();
                return new Identifier(PowerGrid.MOD_ID, "special/" + serializerId);
            }

            @Override
            public RecipeSerializer<?> getSerializer() {
                return serializer;
            }

            @Override
            public @Nullable JsonObject toAdvancementJson() {
                return null;
            }

            @Override
            public @Nullable Identifier getAdvancementId() {
                return null;
            }
        });
        all.add(recipe);
        return recipe;
    }

    @Override
    public String getName() {
        return "Power Grid's Sequenced Assembly Recipes";
    }
}
