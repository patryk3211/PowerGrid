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

    GENERATOR_CLUTCH = create(ModdedBlocks.GENERATOR_CLUTCH)
            .unlockedBy(AllBlocks.CLUTCH::get)
            .shapeless(b -> b
                    .input(AllBlocks.CLUTCH)
                    .input(AllItems.ANDESITE_ALLOY)
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
                    .input('I', RecipeTags.ironSheet())),

    RELAY = create(ModdedItems.RELAY)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .shapeless(b -> b
                    .input(ModdedItems.COPPER_COIL)
                    .input(RecipeTags.ironSheet())
                    .input(AllItems.ANDESITE_ALLOY)),

    REDSTONE_RELAY = create(ModdedItems.REDSTONE_RELAY)
            .unlockedBy(() -> ModdedItems.RELAY)
            .shapeless(b -> b
                    .input(ModdedItems.RELAY)
                    .input(Items.REDSTONE)),

    RESISTOR = create(ModdedItems.RESISTOR)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .shapeless(b -> b
                    .input(ModdedItems.RESISTIVE_COIL)
                    .input(RecipeTags.coal())),

    CIRCUIT_DESIGN_TABLE = create(ModdedBlocks.CIRCUIT_DESIGN_TABLE)
            .unlockedBy(() -> AllItems.EMPTY_SCHEMATIC)
            .shaped(b -> b
                    .pattern("ES")
                    .pattern("WW")
                    .pattern("WW")
                    .input('E', AllItems.ELECTRON_TUBE)
                    .input('S', AllItems.EMPTY_SCHEMATIC)
                    .input('W', RecipeTags.planks())),

    GENERATOR_HOUSING = create(ModdedBlocks.GENERATOR_HOUSING)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .shaped(b -> b
                    .pattern("II")
                    .pattern("CI")
                    .input('I', RecipeTags.ironSheet())
                    .input('C', RecipeTags.copperSheet())),

    LV_SWITCH = create(ModdedBlocks.LV_SWITCH)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .shaped(b -> b
                    .pattern(" L ")
                    .pattern("CAC")
                    .input('L', Items.LEVER)
                    .input('C', RecipeTags.copperNugget())
                    .input('A', AllBlocks.ANDESITE_CASING)),

    LV_BUTTON = create(ModdedBlocks.LV_BUTTON)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .shaped(b -> b
                    .pattern(" B ")
                    .pattern("CAC")
                    .input('B', Items.STONE_BUTTON)
                    .input('C', RecipeTags.copperNugget())
                    .input('A', AllBlocks.ANDESITE_CASING)),

    MV_SWITCH = create(ModdedBlocks.MV_SWITCH)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .shaped(b -> b
                    .pattern(" L ")
                    .pattern("SAS")
                    .input('L', Items.LEVER)
                    .input('S', RecipeTags.copperSheet())
                    .input('A', AllBlocks.ANDESITE_CASING)),

    CAPACITOR = create(ModdedItems.CAPACITOR)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .shaped(b -> b
                    .pattern(" I ")
                    .pattern("ZPC")
                    .pattern(" I ")
                    .input('Z', RecipeTags.zincSheet())
                    .input('P', Items.PAPER)
                    .input('C', RecipeTags.copperSheet())
                    .input('I', RecipeTags.ironSheet())),

    DIODE = create(ModdedItems.DIODE)
            .unlockedBy(() -> AllItems.POLISHED_ROSE_QUARTZ)
            .shaped(b -> b
                    .pattern("RC")
                    .input('R', AllItems.POLISHED_ROSE_QUARTZ)
                    .input('C', RecipeTags.copperSheet())),

    LED = create(ModdedItems.LED)
            .unlockedBy(() -> Items.AMETHYST_SHARD)
            .shaped(b -> b
                    .pattern("A")
                    .pattern("I")
                    .input('A', Items.AMETHYST_SHARD)
                    .input('I', RecipeTags.ironSheet())),

    POTENTIOMETER = create(ModdedItems.POTENTIOMETER)
            .unlockedBy(() -> ModdedItems.RESISTOR)
            .shaped(b -> b
                    .pattern("A")
                    .pattern("N")
                    .pattern("C")
                    .input('A', AllItems.ANDESITE_ALLOY)
                    .input('N', RecipeTags.copperNugget())
                    .input('C', RecipeTags.coal())),

    WIRE_CUTTER = create(ModdedItems.WIRE_CUTTER)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .shaped(b -> b
                    .pattern("I ")
                    .pattern(" I")
                    .input('I', RecipeTags.ironSheet())),

    ELECTRIC_FAN = create(ModdedBlocks.ELECTRIC_FAN)
            .unlockedBy(() -> ModdedBlocks.ELECTRIC_MOTOR)
            .shapeless(b -> b
                    .input(ModdedBlocks.ELECTRIC_MOTOR)
                    .input(AllBlocks.ENCASED_FAN)),

    SERVO = create(ModdedBlocks.SERVO)
            .unlockedBy(() -> ModdedBlocks.ELECTRIC_MOTOR)
            .shapeless(b -> b
                    .input(ModdedBlocks.ELECTRIC_MOTOR)
                    .input(ModdedItems.ELECTRICAL_GIZMO)
                    .input(AllItems.PRECISION_MECHANISM)),

    FUSE_HOLDER = create(ModdedBlocks.FUSE_HOLDER)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .shaped(b -> b
                    .pattern("I")
                    .pattern("A")
                    .pattern("C")
                    .input('I', RecipeTags.ironSheet())
                    .input('A', AllBlocks.ANDESITE_CASING)
                    .input('C', RecipeTags.copperSheet())),

    DEVICE_CONNECTOR = create(ModdedBlocks.DEVICE_CONNECTOR)
            .unlockedBy(() -> AllItems.COPPER_SHEET)
            .shaped(b -> b
                    .pattern(" Z ")
                    .pattern("CAC")
                    .input('C', RecipeTags.copperSheet())
                    .input('Z', RecipeTags.zincSheet())
                    .input('A', AllItems.ANDESITE_ALLOY)),

    ALARM_BELL = create(ModdedBlocks.ALARM_BELL)
            .unlockedBy(() -> AllItems.BRASS_SHEET)
            .shaped(b -> b
                    .pattern(" B ")
                    .pattern("BAB")
                    .pattern(" CI")
                    .input('B', RecipeTags.brassSheet())
                    .input('A', AllItems.ANDESITE_ALLOY)
                    .input('C', RecipeTags.copperCoil())
                    .input('I', RecipeTags.ironIngot()))
            ;

    public CraftingRecipes(FabricDataOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "Power Grid's Crafting Recipes";
    }
}
