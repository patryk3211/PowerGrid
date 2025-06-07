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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.Items;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

public class CraftingRecipes extends StandardRecipeProvider {
    GeneratedRecipe

    GENERATOR_COIL = create(ModdedBlocks.GENERATOR_COIL)
            .unlockedBy(ModdedItems.WIRE::get)
            .shaped(b -> b
                    .pattern("CIC")
                    .pattern("CIC")
                    .input('C', ModdedItems.COPPER_COIL)
                    .input('I', RecipeTags.ironSheet())
            ),

    WIRE_CONNECTOR = create(ModdedBlocks.WIRE_CONNECTOR)
            .unlockedBy(AllItems.ANDESITE_ALLOY::get)
            .shaped(b -> b
                    .pattern(" C ")
                    .pattern("CAC")
                    .input('C', RecipeTags.copperNugget())
                    .input('A', AllItems.ANDESITE_ALLOY)
            ),

    HEAVY_WIRE_CONNECTOR = create(ModdedBlocks.HEAVY_WIRE_CONNECTOR)
            .unlockedBy(() -> Items.TERRACOTTA)
            .shaped(b -> b
                    .pattern(" I ")
                    .pattern("ITI")
                    .pattern(" T ")
                    .input('I', RecipeTags.ironNugget())
                    .input('T', Items.TERRACOTTA)
            ),

    LIGHT_BULB = create(ModdedItems.LIGHT_BULB)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .shaped(b -> b
                    .pattern(" G ")
                    .pattern("GWG")
                    .pattern(" I ")
                    .input('G', Items.GLASS_PANE)
                    .input('W', ModdedItems.IRON_WIRE)
                    .input('I', RecipeTags.ironSheet())
            ),

    GROWTH_LAMP = create(ModdedItems.GROWTH_LAMP)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .shaped(b -> b
                    .pattern("GQG")
                    .pattern("GWG")
                    .pattern(" I ")
                    .input('G', Items.GLASS_PANE)
                    .input('W', ModdedItems.IRON_WIRE)
                    .input('Q', Items.QUARTZ)
                    .input('I', RecipeTags.ironSheet())
            ),

    RESISTIVE_COIL = create(ModdedItems.RESISTIVE_COIL)
            .unlockedBy(ModdedItems.IRON_WIRE::get)
            .shapeless(b -> b
                    .input(ModdedItems.IRON_WIRE, 4)
                    .input(Items.STICK, 1)
            ),

    COPPER_COIL = create(ModdedItems.COPPER_COIL)
            .unlockedBy(ModdedItems.WIRE::get)
            .shapeless(b -> b
                    .input(ModdedItems.WIRE, 4)
                    .input(Items.STICK, 1)
            ),

    HEATING_COIL = create(ModdedBlocks.HEATING_COIL)
            .unlockedBy(ModdedItems.RESISTIVE_COIL::get)
            .shaped(b -> b
                    .pattern("C C")
                    .pattern("IRI")
                    .pattern("IRI")
                    .input('C', RecipeTags.copperNugget())
                    .input('I', RecipeTags.ironSheet())
                    .input('R', ModdedItems.RESISTIVE_COIL)
            ),

    GENERATOR_HOUSING = create(ModdedBlocks.GENERATOR_HOUSING)
            .unlockedBy(AllItems.IRON_SHEET::get)
            .shaped(b -> b
                    .pattern("II")
                    .pattern(" I")
                    .input('I', RecipeTags.ironSheet())
            ),

    ANDESITE_VOLTAGE_GAUGE = create(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .shaped(b -> b
                    .pattern("NCN")
                    .pattern(" A ")
                    .input('N', RecipeTags.copperNugget())
                    .input('A', AllBlocks.ANDESITE_CASING)
                    .input('C', Items.COMPASS)
            ),

    BRASS_VOLTAGE_GAUGE = create(ModdedBlocks.BRASS_VOLTAGE_METER)
            .unlockedBy(AllBlocks.BRASS_CASING::get)
            .shaped(b -> b
                    .pattern("NCN")
                    .pattern(" B ")
                    .input('N', RecipeTags.copperNugget())
                    .input('B', AllBlocks.BRASS_CASING)
                    .input('C', Items.COMPASS)
            ),

    ANDESITE_CURRENT_GAUGE = create(ModdedBlocks.ANDESITE_CURRENT_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .shapeless(b -> b.input(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            ),

    BRASS_CURRENT_GAUGE = create(ModdedBlocks.BRASS_CURRENT_METER)
            .unlockedBy(AllBlocks.BRASS_CASING::get)
            .shapeless(b -> b.input(ModdedBlocks.BRASS_VOLTAGE_METER)
            ),

    ANDESITE_VOLTAGE_GAUGE_BACK = create(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .suffix("_convert")
            .shapeless(b -> b.input(ModdedBlocks.ANDESITE_CURRENT_METER)
            ),

    BRASS_VOLTAGE_GAUGE_BACK = create(ModdedBlocks.BRASS_VOLTAGE_METER)
            .unlockedBy(AllBlocks.BRASS_CASING::get)
            .suffix("_convert")
            .shapeless(b -> b.input(ModdedBlocks.BRASS_CURRENT_METER)
            ),

    LIGHT_FIXTURE = create(ModdedBlocks.LIGHT_FIXTURE)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .shaped(b -> b
                    .pattern(" I ")
                    .pattern("CAC")
                    .input('I', RecipeTags.ironSheet())
                    .input('C', RecipeTags.copperNugget())
                    .input('A', AllBlocks.ANDESITE_CASING)
            ),

    ELECTROMAGNET = create(ModdedBlocks.ELECTROMAGNET)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .shaped(b -> b
                    .pattern("CCC")
                    .pattern("CIC")
                    .pattern("CCC")
                    .input('C', ModdedItems.COPPER_COIL)
                    .input('I', RecipeTags.ironSheet()))
            ;

    public CraftingRecipes(FabricDataOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "Power Grid's Crafting Recipes";
    }
}
