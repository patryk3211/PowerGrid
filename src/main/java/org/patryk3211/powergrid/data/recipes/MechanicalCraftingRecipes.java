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
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import static org.patryk3211.powergrid.data.recipes.RecipeTags.*;

@SuppressWarnings("unused")
public class MechanicalCraftingRecipes extends MechanicalCraftingRecipeGen {
    GeneratedRecipe

    ELECTRIC_MOTOR = create(ModdedBlocks.ELECTRIC_MOTOR::get)
            .recipe(b -> b
                    .key('C', copperCoil())
                    .key('M', ModdedItems.MAGNET)
                    .key('I', ironSheet())
                    .key('S', shaft())
                    .key('E', conductiveCasing())
                    .patternLine(" ICI ")
                    .patternLine("CMSMC")
                    .patternLine(" ICI ")
                    .patternLine("  E  ")
            ),

//    GENERATOR_ROTOR = create(ModdedBlocks.GENERATOR_ROTOR::get)
//            .recipe(b -> b
//                    .key('A', AllItems.ANDESITE_ALLOY)
//                    .key('M', ModdedItems.MAGNET)
//                    .key('S', AllBlocks.SHAFT)
//                    .patternLine("AMA")
//                    .patternLine("MSM")
//                    .patternLine("AMA")
//            ),

    GENERATOR_INDUCTION_ROTOR = create(ModdedBlocks.GENERATOR_INDUCTION_ROTOR::get)
            .recipe(b -> b
                    .key('A', andesiteAlloy())
                    .key('M', copperCoil())
                    .key('S', shaft())
                    .patternLine("AMA")
                    .patternLine("MSM")
                    .patternLine("AMA")
            ),

    GENERATOR_COMMUTATOR = create(ModdedBlocks.GENERATOR_COMMUTATOR::get)
            .recipe(b -> b
                    .key('A', andesiteAlloy())
                    .key('S', shaft())
                    .key('C', andesiteCasing())
                    .key('M', copperSheet())
                    .key('G', coal())
                    .key('N', copperNugget())
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
                    .key('Z', zincIngot())
                    .key('C', copperCoil())
                    .key('N', copperNugget())
                    .key('S', copperSheet())
                    .key('A', andesiteAlloy())
                    .patternLine("GCZZSN")
                    .patternLine(" A    ")
            ),

    ELECTROBATON = create(ModdedItems.ELECTROBATON::get)
            .recipe(b -> b
                    .key('G', ModdedItems.ELECTRICAL_GIZMO)
                    .key('Z', zincIngot())
                    .key('A', andesiteAlloy())
                    .key('C', copperCoil())
                    .patternLine("C")
                    .patternLine("G")
                    .patternLine("Z")
                    .patternLine("Z")
                    .patternLine("A")
            ),

    BASIN_HEATER = create(ModdedBlocks.BASIN_HEATER::get)
            .recipe(b -> b
                    .key('R', resistiveCoil())
                    .key('E', conductiveCasing())
                    .key('C', copperSheet())
                    .patternLine("RRRRR")
                    .patternLine(" C C ")
                    .patternLine(" CEC ")),

    CARBON_PILE_COIL = create(ModdedBlocks.CARBON_PILE_COIL::get)
            .recipe(b -> b
                    .key('C', copperCoil())
                    .key('E', conductiveCasing())
                    .key('Z', zincSheet())
                    .key('I', ironSheet())
                    .patternLine("  I  ")
                    .patternLine("CCICC")
                    .patternLine(" ZEZ ")),


    CRT = create(ModdedBlocks.CRT::get)
            .recipe(b -> b
                    .key('G', glass())
                    .key('C', copperCoil())
                    .key('E', electronTube())
                    .key('g', glowstoneDust())
                    .patternLine("  g  ")
                    .patternLine(" GGG ")
                    .patternLine("CCGCC")
                    .patternLine("  E  ")),

    PUNCH_CARD_READER = create(ModdedBlocks.PUNCH_CARD_READER::get)
            .recipe(b -> b
                    .key('C', RecipeTags.conductiveCasing())
                    .key('n', RecipeTags.copperNugget())
                    .key('S', RecipeTags.shaft())
                    .key('G', RecipeTags.smallCog())
                    .patternLine("nnCnn")
                    .patternLine(" GSG "))
        ;


    public MechanicalCraftingRecipes(PackOutput output) {
        super(output, PowerGrid.MOD_ID);
    }
}
