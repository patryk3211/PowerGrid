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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

@SuppressWarnings("unused")
public class MechanicalCraftingRecipes extends MechanicalCraftingRecipeGen {
    GeneratedRecipe

    ELECTRIC_MOTOR = create(ModdedBlocks.ELECTRIC_MOTOR::get)
            .recipe(b -> b
                    .key('C', ModdedItems.COPPER_COIL)
                    .key('M', ModdedItems.MAGNET)
                    .key('I', RecipeTags.ironSheet())
                    .key('S', AllBlocks.SHAFT)
                    .key('E', RecipeTags.conductiveCasing())
                    .patternLine(" ICI ")
                    .patternLine("CMSMC")
                    .patternLine(" ICI ")
                    .patternLine("  E  ")
            ),

    GENERATOR_ROTOR = create(ModdedBlocks.GENERATOR_ROTOR::get)
            .recipe(b -> b
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .key('M', ModdedItems.MAGNET)
                    .key('S', AllBlocks.SHAFT)
                    .patternLine("AMA")
                    .patternLine("MSM")
                    .patternLine("AMA")
            ),

    GENERATOR_INDUCTION_ROTOR = create(ModdedBlocks.GENERATOR_INDUCTION_ROTOR::get)
            .recipe(b -> b
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .key('M', RecipeTags.copperCoil())
                    .key('S', AllBlocks.SHAFT)
                    .patternLine("AMA")
                    .patternLine("MSM")
                    .patternLine("AMA")
            ),

    GENERATOR_COMMUTATOR = create(ModdedBlocks.GENERATOR_COMMUTATOR::get)
            .recipe(b -> b
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .key('S', AllBlocks.SHAFT)
                    .key('C', AllBlocks.ANDESITE_CASING)
                    .key('M', RecipeTags.copperSheet())
                    .key('G', RecipeTags.coal())
                    .key('N', RecipeTags.copperNugget())
                    .patternLine("N N")
                    .patternLine("GMG")
                    .patternLine("ASA")
                    .patternLine(" C ")
            ),

    INTEGRATED_CIRCUIT = create(ModdedItems.INTEGRATED_CIRCUIT::get)
            .recipe(b -> b
                    .key('L', Items.LAPIS_LAZULI)
                    .key('R', Items.REDSTONE)
                    .key('G', Items.GOLD_NUGGET)
                    .key('Q', AllItems.ROSE_QUARTZ)
                    .patternLine("  L  ")
                    .patternLine("RRQRR")
                    .patternLine(" GGG ")
            ),

    ELECTROZAPPER = create(ModdedItems.ELECTROZAPPER::get)
            .recipe(b -> b
                    .key('G', ModdedItems.ELECTRICAL_GIZMO)
                    .key('Z', RecipeTags.zincIngot())
                    .key('C', RecipeTags.copperCoil())
                    .key('N', RecipeTags.copperNugget())
                    .key('S', RecipeTags.copperSheet())
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .patternLine("GCZZSN")
                    .patternLine(" A    ")
            ),

    ELECTROBATON = create(ModdedItems.ELECTROBATON::get)
            .recipe(b -> b
                    .key('G', ModdedItems.ELECTRICAL_GIZMO)
                    .key('Z', RecipeTags.zincIngot())
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .key('C', RecipeTags.copperCoil())
                    .patternLine("C")
                    .patternLine("G")
                    .patternLine("Z")
                    .patternLine("Z")
                    .patternLine("A")
            ),

    HV_SWITCH = create(ModdedBlocks.HV_SWITCH::get)
            .recipe(b -> b
                    .key('I', RecipeTags.ironSheet())
                    .key('A', AllBlocks.ANDESITE_CASING)
                    .key('G', AllBlocks.COGWHEEL)
                    .patternLine("I  ")
                    .patternLine("I  ")
                    .patternLine("AGI")),

    VARIAC = create(ModdedBlocks.VARIAC::get)
            .recipe(b -> b
                    .key('C', RecipeTags.coal())
                    .key('T', RecipeTags.transformerCore())
                    .key('W', RecipeTags.copperCoil())
                    .key('E', RecipeTags.conductiveCasing())
                    .key('S', RecipeTags.shaft())
                    .patternLine("CS ")
                    .patternLine("WTW")
                    .patternLine("WEW")),

    BASIN_HEATER = create(ModdedBlocks.BASIN_HEATER::get)
            .recipe(b -> b
                    .key('R', RecipeTags.resistiveCoil())
                    .key('E', RecipeTags.conductiveCasing())
                    .key('C', RecipeTags.copperSheet())
                    .patternLine("RRRRR")
                    .patternLine(" C C ")
                    .patternLine(" CEC "))
            ;


    public MechanicalCraftingRecipes(FabricDataOutput output) {
        super(output, PowerGrid.MOD_ID);
    }
}
