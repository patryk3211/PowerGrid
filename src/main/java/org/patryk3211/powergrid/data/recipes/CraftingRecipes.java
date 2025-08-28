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
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.List;

@SuppressWarnings("unused")
public class CraftingRecipes extends StandardRecipeProvider {
    GeneratedRecipe

    WIRE_CONNECTOR = create(ModdedBlocks.WIRE_CONNECTOR)
            .unlockedBy(AllItems.ANDESITE_ALLOY::get)
            .viaShaped(b -> b
                    .pattern(" C ")
                    .pattern("CAC")
                    .define('C', RecipeTags.copperNugget())
                    .define('A', AllItems.ANDESITE_ALLOY)
            ),

    HEAVY_WIRE_CONNECTOR = create(ModdedBlocks.HEAVY_WIRE_CONNECTOR)
            .unlockedBy(() -> Items.TERRACOTTA)
            .viaShaped(b -> b
                    .pattern(" I ")
                    .pattern("ITI")
                    .pattern(" T ")
                    .define('I', RecipeTags.ironNugget())
                    .define('T', Items.TERRACOTTA)
            ),

    LIGHT_BULB = create(ModdedItems.LIGHT_BULB)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .viaShaped(b -> b
                    .pattern(" G ")
                    .pattern("GWG")
                    .pattern(" I ")
                    .define('G', Items.GLASS_PANE)
                    .define('W', ModdedItems.IRON_WIRE)
                    .define('I', RecipeTags.ironSheet())
            ),

    GROWTH_LAMP = create(ModdedItems.GROWTH_LAMP)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .viaShaped(b -> b
                    .pattern("GQG")
                    .pattern("GWG")
                    .pattern(" I ")
                    .define('G', Items.GLASS_PANE)
                    .define('W', ModdedItems.IRON_WIRE)
                    .define('Q', Items.QUARTZ)
                    .define('I', RecipeTags.ironSheet())
            ),

    RESISTIVE_COIL = create(ModdedItems.RESISTIVE_COIL)
            .unlockedBy(ModdedItems.IRON_WIRE::get)
            .viaShapeless(b -> b
                    .requires(ModdedItems.IRON_WIRE, 4)
                    .requires(Items.STICK, 1)
            ),

    COPPER_COIL = create(ModdedItems.COPPER_COIL)
            .unlockedBy(ModdedItems.WIRE::get)
            .viaShapeless(b -> b
                    .requires(ModdedItems.WIRE, 4)
                    .requires(Items.STICK, 1)
            ),

    HEATING_COIL = create(ModdedBlocks.HEATING_COIL)
            .unlockedBy(ModdedItems.RESISTIVE_COIL::get)
            .viaShaped(b -> b
                    .pattern("C C")
                    .pattern("IRI")
                    .pattern("IRI")
                    .define('C', RecipeTags.copperNugget())
                    .define('I', RecipeTags.ironSheet())
                    .define('R', ModdedItems.RESISTIVE_COIL)
            ),

    GENERATOR_CLUTCH = create(ModdedBlocks.GENERATOR_CLUTCH)
            .unlockedBy(AllBlocks.CLUTCH::get)
            .viaShapeless(b -> b
                    .requires(AllBlocks.CLUTCH)
                    .requires(AllItems.ANDESITE_ALLOY)
            ),

    VOLTAGE_GAUGE = create(ModdedBlocks.VOLTAGE_METER)
            .unlockedBy(ModdedBlocks.CONDUCTIVE_CASING::get)
            .viaShaped(b -> b
                    .pattern("C")
                    .pattern("c")
                    .pattern("A")
                    .define('A', RecipeTags.conductiveCasing())
                    .define('c', RecipeTags.copperCoil())
                    .define('C', Items.COMPASS)
            ),

    CURRENT_GAUGE = create(ModdedBlocks.CURRENT_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .viaShapeless(b -> b.requires(ModdedBlocks.VOLTAGE_METER)
            ),

    VOLTAGE_GAUGE_BACK = create(ModdedBlocks.VOLTAGE_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .withSuffix("_convert")
            .viaShapeless(b -> b.requires(ModdedBlocks.CURRENT_METER)
            ),

    LIGHT_FIXTURE = create(ModdedBlocks.LIGHT_FIXTURE)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .viaShaped(b -> b
                    .pattern(" I ")
                    .pattern("CAC")
                    .define('I', RecipeTags.ironSheet())
                    .define('C', RecipeTags.copperNugget())
                    .define('A', AllBlocks.ANDESITE_CASING)
            ),

    ELECTROMAGNET = create(ModdedBlocks.ELECTROMAGNET)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .viaShaped(b -> b
                    .pattern(" E ")
                    .pattern("CIC")
                    .pattern("CCC")
                    .define('E', RecipeTags.conductiveCasing())
                    .define('C', ModdedItems.COPPER_COIL)
                    .define('I', RecipeTags.ironSheet())),

    RELAY = create(ModdedItems.RELAY)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .viaShapeless(b -> b
                    .requires(ModdedItems.COPPER_COIL)
                    .requires(RecipeTags.ironSheet())
                    .requires(AllItems.ANDESITE_ALLOY)),

    REDSTONE_RELAY = create(ModdedItems.REDSTONE_RELAY)
            .unlockedBy(() -> ModdedItems.RELAY)
            .viaShapeless(b -> b
                    .requires(ModdedItems.RELAY)
                    .requires(Items.REDSTONE)),

    RESISTOR = create(ModdedItems.RESISTOR)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .viaShapeless(b -> b
                    .requires(ModdedItems.RESISTIVE_COIL)
                    .requires(RecipeTags.coal())),

    CIRCUIT_DESIGN_TABLE = create(ModdedBlocks.CIRCUIT_DESIGN_TABLE)
            .unlockedBy(() -> AllItems.EMPTY_SCHEMATIC)
            .viaShaped(b -> b
                    .pattern("ES")
                    .pattern("WW")
                    .pattern("WW")
                    .define('E', AllItems.ELECTRON_TUBE)
                    .define('S', AllItems.EMPTY_SCHEMATIC)
                    .define('W', RecipeTags.planks())),

    GENERATOR_HOUSING = create(ModdedBlocks.GENERATOR_HOUSING)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern("IC")
                    .pattern("EI")
                    .define('I', RecipeTags.ironSheet())
                    .define('C', RecipeTags.copperSheet())
                    .define('E', RecipeTags.conductiveCasing())),

    LV_SWITCH = create(ModdedBlocks.LV_SWITCH)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShaped(b -> b
                    .pattern(" L ")
                    .pattern("CAC")
                    .define('L', Items.LEVER)
                    .define('C', RecipeTags.copperNugget())
                    .define('A', AllBlocks.ANDESITE_CASING)),

    LV_BUTTON = create(ModdedBlocks.LV_BUTTON)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShaped(b -> b
                    .pattern(" B ")
                    .pattern("CAC")
                    .define('B', Items.STONE_BUTTON)
                    .define('C', RecipeTags.copperNugget())
                    .define('A', AllBlocks.ANDESITE_CASING)),

    MV_SWITCH = create(ModdedBlocks.MV_SWITCH)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShaped(b -> b
                    .pattern(" L ")
                    .pattern("SAS")
                    .define('L', Items.LEVER)
                    .define('S', RecipeTags.copperSheet())
                    .define('A', AllBlocks.ANDESITE_CASING)),

    CONTACTOR = create(ModdedBlocks.CONTACTOR)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .viaShaped(b -> b
                    .pattern("PCP")
                    .pattern("PIP")
                    .pattern(" E ")
                    .define('P', RecipeTags.copperSheet())
                    .define('C', RecipeTags.copperCoil())
                    .define('I', RecipeTags.ironIngot())
                    .define('E', RecipeTags.conductiveCasing())),

    SPARK_GAP = create(ModdedBlocks.SPARK_GAP)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShaped(b -> b
                    .pattern("C C")
                    .pattern("I I")
                    .pattern(" A ")
                    .define('C', RecipeTags.copperNugget())
                    .define('I', RecipeTags.ironSheet())
                    .define('A', RecipeTags.andesiteCasing())),

    CAPACITOR = create(ModdedItems.CAPACITOR)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern(" I ")
                    .pattern("ZPC")
                    .pattern(" I ")
                    .define('Z', RecipeTags.zincSheet())
                    .define('P', Items.PAPER)
                    .define('C', RecipeTags.copperSheet())
                    .define('I', RecipeTags.ironSheet())),

    DIODE = create(ModdedItems.DIODE)
            .unlockedBy(() -> AllItems.POLISHED_ROSE_QUARTZ)
            .viaShaped(b -> b
                    .pattern("RC")
                    .define('R', AllItems.POLISHED_ROSE_QUARTZ)
                    .define('C', RecipeTags.copperSheet())),

    LED = create(ModdedItems.LED)
            .unlockedBy(() -> Items.AMETHYST_SHARD)
            .viaShaped(b -> b
                    .pattern("A")
                    .pattern("I")
                    .define('A', Items.AMETHYST_SHARD)
                    .define('I', RecipeTags.ironSheet())),

    POTENTIOMETER = create(ModdedItems.POTENTIOMETER)
            .unlockedBy(() -> ModdedItems.RESISTOR)
            .viaShaped(b -> b
                    .pattern("A")
                    .pattern("N")
                    .pattern("C")
                    .define('A', AllItems.ANDESITE_ALLOY)
                    .define('N', RecipeTags.copperNugget())
                    .define('C', RecipeTags.coal())),

    WIRE_CUTTER = create(ModdedItems.WIRE_CUTTER)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern("I ")
                    .pattern(" I")
                    .define('I', RecipeTags.ironSheet())),

    ELECTRIC_FAN = create(ModdedBlocks.ELECTRIC_FAN)
            .unlockedBy(() -> ModdedBlocks.ELECTRIC_MOTOR)
            .viaShapeless(b -> b
                    .requires(ModdedBlocks.ELECTRIC_MOTOR)
                    .requires(AllBlocks.ENCASED_FAN)),

    SERVO = create(ModdedBlocks.SERVO)
            .unlockedBy(() -> ModdedBlocks.ELECTRIC_MOTOR)
            .viaShapeless(b -> b
                    .requires(ModdedBlocks.ELECTRIC_MOTOR)
                    .requires(ModdedItems.ELECTRICAL_GIZMO)
                    .requires(AllItems.PRECISION_MECHANISM)),

    FUSE_HOLDER = create(ModdedBlocks.FUSE_HOLDER)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .viaShaped(b -> b
                    .pattern("I")
                    .pattern("A")
                    .pattern("C")
                    .define('I', RecipeTags.ironSheet())
                    .define('A', AllBlocks.ANDESITE_CASING)
                    .define('C', RecipeTags.copperSheet())),

    DEVICE_CONNECTOR = create(ModdedBlocks.DEVICE_CONNECTOR)
            .unlockedBy(() -> AllItems.COPPER_SHEET)
            .viaShaped(b -> b
                    .pattern(" Z ")
                    .pattern("CAC")
                    .define('C', RecipeTags.copperSheet())
                    .define('Z', RecipeTags.zincSheet())
                    .define('A', AllItems.ANDESITE_ALLOY)),

    ALARM_BELL = create(ModdedBlocks.ALARM_BELL)
            .unlockedBy(() -> AllItems.BRASS_SHEET)
            .viaShaped(b -> b
                    .pattern(" B ")
                    .pattern("BAB")
                    .pattern(" CI")
                    .define('B', RecipeTags.brassSheet())
                    .define('A', AllItems.ANDESITE_ALLOY)
                    .define('C', RecipeTags.copperCoil())
                    .define('I', RecipeTags.ironIngot())),

    POTATO_BATTERY = create(ModdedBlocks.POTATO_BATTERY)
            .unlockedBy(() -> Items.POTATO)
            .viaShapeless(b -> b
                    .requires(Items.POTATO)
                    .requires(RecipeTags.zincSheet())
                    .requires(RecipeTags.copperSheet())),

    PORTABLE_BATTERY = create(ModdedItems.PORTABLE_BATTERY)
            .unlockedBy(() -> ModdedBlocks.BATTERY)
            .viaShaped(b -> b
                    .pattern(" C ")
                    .pattern("ZDZ")
                    .pattern("ZBZ")
                    .define('C', ModdedBlocks.DEVICE_CONNECTOR)
                    .define('Z', RecipeTags.zincSheet())
                    .define('D', RecipeTags.electricalGizmo())
                    .define('B', ModdedBlocks.BATTERY)),

    HOUSING_CYCLE = conversionCycle(List.of(ModdedBlocks.GENERATOR_HOUSING, ModdedBlocks.VERTICAL_GENERATOR_HOUSING))
            ;

    public CraftingRecipes(PackOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "Power Grid's Crafting Recipes";
    }
}
