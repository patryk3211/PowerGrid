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

import static org.patryk3211.powergrid.data.recipes.RecipeTags.*;

@SuppressWarnings("unused")
public class CraftingRecipes extends StandardRecipeProvider {
    GeneratedRecipe

    WIRE_CONNECTOR = create(ModdedBlocks.WIRE_CONNECTOR)
            .unlockedBy(AllItems.ANDESITE_ALLOY::get)
            .viaShaped(b -> b
                    .pattern(" C ")
                    .pattern("CAC")
                    .define('C', copperNugget())
                    .define('A', andesiteAlloy())
            ),

    SOLAR_PANEL_BEARING= create(ModdedBlocks.SOLAR_PANEL_BEARING)
            .unlockedBy(AllItems.ANDESITE_ALLOY::get)
            .viaShaped(b -> b
                    .pattern("S")
                    .pattern("P")
                    .pattern("Z")
                    .define('S', Items.OAK_SLAB)
                    .define('P', ModdedBlocks.COPPER_PLATING)
                    .define('Z', ModdedBlocks.CONDUCTIVE_CASING)
            ),
            FACTORY_LIGHT = create(ModdedBlocks.FACTORY_LIGHT)
                    .unlockedBy(AllItems.ANDESITE_ALLOY::get)
                    .viaShaped(b -> b
                            .pattern("I")
                            .pattern("C")
                            .pattern("G")
                            .define('I', ironSheet())
                            .define('C', andesiteAlloy())
                            .define('G', glasspane())
                    ),

    HEAVY_WIRE_CONNECTOR = create(ModdedBlocks.HEAVY_WIRE_CONNECTOR)
            .unlockedBy(() -> Items.TERRACOTTA)
            .viaShaped(b -> b
                    .pattern(" I ")
                    .pattern("ITI")
                    .pattern(" T ")
                    .define('I', ironNugget())
                    .define('T', Items.TERRACOTTA)
            ),

    PINS = create(ModdedItems.PINS)
            .unlockedBy(() -> AllItems.COPPER_NUGGET)
            .viaShapeless(b -> b
                    .requires(copperNugget())
                    .requires(copperNugget())),

    GROUNDING_ROD = create(ModdedBlocks.GROUNDING_ROD)
            .unlockedBy(() -> AllItems.COPPER_SHEET)
            .viaShaped(b -> b
                    .pattern("P")
                    .pattern("I")
                    .pattern("I")
                    .define('P', copperSheet())
                    .define('I', copperIngot())
            ),

    LV_LIGHT_BULB = create(ModdedItems.LV_LIGHT_BULB)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .viaShaped(b -> b
                    .pattern(" G ")
                    .pattern("GFG")
                    .pattern(" I ")
                    .define('G', Items.GLASS_PANE)
                    .define('F', coal())
                    .define('I', ironSheet())
            ),

    LIGHT_BULB = create(ModdedItems.LIGHT_BULB)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .viaShaped(b -> b
                    .pattern(" G ")
                    .pattern("GWG")
                    .pattern(" I ")
                    .define('G', Items.GLASS_PANE)
                    .define('W', ironWire())
                    .define('I', ironSheet())
            ),

    GROWTH_LAMP = create(ModdedItems.GROWTH_LAMP)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .viaShaped(b -> b
                    .pattern("GQG")
                    .pattern("GWG")
                    .pattern(" I ")
                    .define('G', Items.GLASS_PANE)
                    .define('W', ironWire())
                    .define('Q', Items.QUARTZ)
                    .define('I', ironSheet())
            ),

    RESISTIVE_COIL = create(ModdedItems.RESISTIVE_COIL)
            .unlockedBy(ModdedItems.IRON_WIRE::get)
            .viaShapeless(b -> b
                    .requires(ironWire())
                    .requires(ironWire())
                    .requires(ironWire())
                    .requires(ironWire())
                    .requires(Items.STICK, 1)
            ),

    COPPER_COIL = create(ModdedItems.COPPER_COIL)
            .unlockedBy(ModdedItems.WIRE::get)
            .viaShaped(b -> b
                    .pattern(" WC")
                    .pattern("WSW")
                    .pattern("CW ")
                    .define('C', cardboard())
                    .define('S', Items.STICK)
                    .define('W', copperWire())
            ),

    HEATING_COIL = create(ModdedBlocks.HEATING_COIL)
            .unlockedBy(ModdedItems.RESISTIVE_COIL::get)
            .viaShaped(b -> b
                    .pattern(" P ")
                    .pattern("IRI")
                    .pattern("IRI")
                    .define('P', pins())
                    .define('I', ironSheet())
                    .define('R', resistiveCoil())
            ),

    GENERATOR_CLUTCH = create(ModdedBlocks.GENERATOR_CLUTCH)
            .unlockedBy(AllBlocks.CLUTCH::get)
            .viaShapeless(b -> b
                    .requires(AllBlocks.CLUTCH)
                    .requires(AllItems.ANDESITE_ALLOY)
            ),

    VOLTAGE_GAUGE = create(ModdedBlocks.VOLTAGE_METER)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("C")
                    .pattern("c")
                    .pattern("A")
                    .define('A', conductiveCasing())
                    .define('c', copperCoil())
                    .define('C', Items.COMPASS)
            ),
    PLOTTER = create(ModdedBlocks.PLOTTER)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern(" P")
                    .pattern("SV")
                    .pattern(" A")
                    .define('P', paper())
                    .define('S', shaft())
                    .define('A', andesiteCasing())
                    .define('V', ModdedBlocks.VOLTAGE_METER)
            ),

    GAUGE_CYCLE = conversionCycle(List.of(ModdedBlocks.VOLTAGE_METER, ModdedBlocks.CURRENT_METER)),

    POWER_GAUGE = create(ModdedBlocks.POWER_METER)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShapeless(b -> b
                    .requires(ModdedBlocks.VOLTAGE_METER)
                    .requires(ModdedBlocks.CURRENT_METER)
            ),

    LIGHT_FIXTURE = create(ModdedBlocks.LIGHT_FIXTURE)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("I")
                    .pattern("Z")
                    .pattern("P")
                    .define('I', ironSheet())
                    .define('P', pins())
                    .define('Z', zincSheet())
            ),

    ELECTROMAGNET = create(ModdedBlocks.ELECTROMAGNET)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern(" E ")
                    .pattern("CIC")
                    .pattern("CCC")
                    .define('E', conductiveCasing())
                    .define('C', copperCoil())
                    .define('I', ironSheet())),

    RELAY = create(ModdedItems.RELAY)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .viaShapeless(b -> b
                    .requires(copperCoil())
                    .requires(ironSheet())
                    .requires(andesiteAlloy())),

    RELAY_DPDT = create(ModdedItems.RELAY_DPDT)
            .unlockedBy(() -> ModdedItems.COPPER_COIL)
            .viaShapeless(b -> b
                    .requires(copperCoil())
                    .requires(ironSheet())
                    .requires(ironSheet())
                    .requires(andesiteAlloy())),

    REDSTONE_RELAY = create(ModdedItems.REDSTONE_RELAY)
            .unlockedBy(() -> ModdedItems.RELAY)
            .viaShapeless(b -> b
                    .requires(ModdedItems.RELAY)
                    .requires(redstone())),

    RESISTOR = create(ModdedItems.RESISTOR)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .viaShapeless(b -> b
                    .requires(resistiveCoil())
                    .requires(coal())),

    VARISTOR = create(ModdedItems.VARISTOR)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .viaShapeless(b -> b
                    .requires(resistiveCoil())
                    .requires(coal())
                    .requires(redstone())),

    CIRCUIT_DESIGN_TABLE = create(ModdedBlocks.CIRCUIT_DESIGN_TABLE)
            .unlockedBy(() -> AllItems.EMPTY_SCHEMATIC)
            .viaShaped(b -> b
                    .pattern("ES")
                    .pattern("WW")
                    .pattern("WW")
                    .define('E', AllItems.ELECTRON_TUBE)
                    .define('S', AllItems.EMPTY_SCHEMATIC)
                    .define('W', planks())),

    GENERATOR_HOUSING = create(ModdedBlocks.GENERATOR_HOUSING)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("IC")
                    .pattern("EI")
                    .define('I', ironSheet())
                    .define('C', copperSheet())
                    .define('E', conductiveCasing())),

    LV_SWITCH = create(ModdedBlocks.LV_SWITCH)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShapeless(b -> b
                    .requires(Items.LEVER)
                    .requires(pins())
                    .requires(conductiveCasing())),

    LV_BUTTON = create(ModdedBlocks.LV_BUTTON)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShapeless(b -> b
                    .requires(Items.STONE_BUTTON)
                    .requires(pins())
                    .requires(conductiveCasing())),

    MV_SWITCH = create(ModdedBlocks.MV_SWITCH)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShapeless(b -> b
                    .requires(Items.LEVER)
                    .requires(copperSheet())
                    .requires(conductiveCasing())),

    HV_SWITCH = create(ModdedBlocks.HV_SWITCH::get)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShaped(b -> b
                    .pattern("I  ")
                    .pattern("I  ")
                    .pattern("AGI")
                    .define('I', ironSheet())
                    .define('A', andesiteCasing())
                    .define('G', AllBlocks.COGWHEEL)
            ),

    CONTACTOR = create(ModdedBlocks.CONTACTOR)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("PCP")
                    .pattern("PIP")
                    .pattern(" E ")
                    .define('P', copperSheet())
                    .define('C', copperCoil())
                    .define('I', ironIngot())
                    .define('E', conductiveCasing())),

    HV_BREAKER = create(ModdedBlocks.HV_BREAKER)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("PMP")
                    .pattern("PIP")
                    .pattern(" E ")
                    .define('P', copperSheet())
                    .define('M', precisionMechanism())
                    .define('I', ironIngot())
                    .define('E', conductiveCasing())),

    SPARK_GAP = create(ModdedBlocks.SPARK_GAP)
            .unlockedBy(() -> AllBlocks.ANDESITE_CASING)
            .viaShaped(b -> b
                    .pattern(" P ")
                    .pattern("I I")
                    .pattern(" A ")
                    .define('P', pins())
                    .define('I', ironSheet())
                    .define('A', andesiteCasing())),

    VARIAC = create(ModdedBlocks.VARIAC::get)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("CS ")
                    .pattern("WTW")
                    .pattern("WEW")
                    .define('C', coal())
                    .define('T', transformerCore())
                    .define('W', copperCoil())
                    .define('E', conductiveCasing())
                    .define('S', shaft())),

    RHEOSTAT = create(ModdedBlocks.RHEOSTAT::get)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("CS ")
                    .pattern("WWW")
                    .pattern("WEW")
                    .define('C', coal())
                    .define('W', resistiveCoil())
                    .define('E', conductiveCasing())
                    .define('S', shaft())),

    DIODE = create(ModdedItems.DIODE)
            .unlockedBy(() -> AllItems.POLISHED_ROSE_QUARTZ)
            .viaShaped(b -> b
                    .pattern("RC")
                    .define('R', AllItems.POLISHED_ROSE_QUARTZ)
                    .define('C', copperSheet())),

    VFET = create(ModdedItems.VFET)
            .unlockedBy(() -> AllItems.POLISHED_ROSE_QUARTZ)
            .viaShaped(b -> b
                    .pattern("C")
                    .pattern("R")
                    .pattern("I")
                    .define('R', AllItems.POLISHED_ROSE_QUARTZ)
                    .define('I', ironSheet())
                    .define('C', copperSheet())),

    CAPACITOR = create(ModdedItems.CAPACITOR)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern(" I ")
                    .pattern("ZPC")
                    .pattern(" I ")
                    .define('Z', zincSheet())
                    .define('P', Items.PAPER)
                    .define('C', copperSheet())
                    .define('I', ironSheet())),

    NEON_BULB = create(ModdedItems.NEON_BULB)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern("B")
                    .pattern("G")
                    .pattern("I")
                    .define('B', glass())
                    .define('G', glowstoneDust())
                    .define('I', ironSheet())),

    REGULATOR_TUBE = create(ModdedItems.REGULATOR_TUBE)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern("A")
                    .pattern("G")
                    .pattern("I")
                    .define('A', amethystShard())
                    .define('G', glowstoneDust())
                    .define('I', ironSheet())),

    BARRETTER_TUBE = create(ModdedItems.BARRETTER_TUBE)
            .unlockedBy(() -> Items.QUARTZ)
            .viaShaped(b -> b
                    .pattern("Q")
                    .pattern("I")
                    .pattern("P")
                    .define('Q', quartz())
                    .define('I', ironWire())
                    .define('P', ironSheet())),

    POTENTIOMETER = create(ModdedItems.POTENTIOMETER)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .viaShaped(b -> b
                    .pattern("A")
                    .pattern("N")
                    .pattern("C")
                    .define('A', andesiteAlloy())
                    .define('N', copperNugget())
                    .define('C', coal())),

    WIRE_CUTTER = create(ModdedItems.WIRE_CUTTER)
            .unlockedBy(() -> AllItems.IRON_SHEET)
            .viaShaped(b -> b
                    .pattern("I ")
                    .pattern(" I")
                    .define('I', ironSheet())),

    ELECTRIC_FAN = create(ModdedBlocks.ELECTRIC_FAN)
            .unlockedBy(() -> ModdedBlocks.ELECTRIC_MOTOR)
            .viaShapeless(b -> b
                    .requires(ModdedBlocks.ELECTRIC_MOTOR)
                    .requires(AllBlocks.ENCASED_FAN)),

    CONSTANT_SPEED_MOTOR = create(ModdedBlocks.CONSTANT_SPEED_MOTOR)
            .unlockedBy(() -> ModdedBlocks.ELECTRIC_MOTOR)
            .viaShapeless(b -> b
                    .requires(ModdedBlocks.ELECTRIC_MOTOR)
                    .requires(precisionMechanism())),

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
                    .define('I', ironSheet())
                    .define('A', andesiteCasing())
                    .define('C', copperSheet())),

    DEVICE_CONNECTOR = create(ModdedBlocks.DEVICE_CONNECTOR)
            .unlockedBy(() -> AllItems.COPPER_SHEET)
            .viaShaped(b -> b
                    .pattern(" Z ")
                    .pattern("CAC")
                    .define('C', copperSheet())
                    .define('Z', zincSheet())
                    .define('A', andesiteAlloy())),

    ALARM_BELL = create(ModdedBlocks.ALARM_BELL)
            .unlockedBy(() -> AllItems.BRASS_SHEET)
            .viaShaped(b -> b
                    .pattern(" B ")
                    .pattern("BAB")
                    .pattern(" CI")
                    .define('B', brassSheet())
                    .define('A', andesiteAlloy())
                    .define('C', copperCoil())
                    .define('I', ironIngot())),

    POTATO_BATTERY = create(ModdedBlocks.POTATO_BATTERY)
            .unlockedBy(() -> Items.POTATO)
            .viaShapeless(b -> b
                    .requires(Items.POTATO)
                    .requires(zincSheet())
                    .requires(copperSheet())),

    PORTABLE_BATTERY = create(ModdedItems.PORTABLE_BATTERY)
            .unlockedBy(() -> ModdedBlocks.BATTERY)
            .viaShaped(b -> b
                    .pattern(" C ")
                    .pattern("ZDZ")
                    .pattern("ZBZ")
                    .define('C', ModdedBlocks.DEVICE_CONNECTOR)
                    .define('Z', zincSheet())
                    .define('D', electricalGizmo())
                    .define('B', ModdedBlocks.BATTERY)),

    HOUSING_CYCLE = conversionCycle(List.of(ModdedBlocks.GENERATOR_HOUSING, ModdedBlocks.VERTICAL_GENERATOR_HOUSING)),
    COMMUTATOR_CYCLE = conversionCycle(List.of(ModdedBlocks.GENERATOR_COMMUTATOR, ModdedBlocks.GENERATOR_VERTICAL_COMMUTATOR)),

    POWER_RESISTOR = create(ModdedBlocks.RESISTOR)
            .unlockedBy(() -> ModdedBlocks.CONDUCTIVE_CASING)
            .viaShaped(b -> b
                    .pattern("CBC")
                    .pattern(" E ")
                    .define('C', copperSheet())
                    .define('B', coalBlock())
                    .define('E', conductiveCasing())),

    MULTIMETER = create(ModdedItems.MULTIMETER)
            .unlockedBy(() -> ModdedItems.ELECTRICAL_GIZMO)
            .viaShaped(b -> b
                    .pattern("WGW")
                    .pattern("CEV")
                    .define('W', copperWire())
                    .define('E', conductiveCasing())
                    .define('G', electricalGizmo())
                    .define('C', currentMeter())
                    .define('V', voltageMeter())),

    THERMOMETER = create(ModdedBlocks.THERMOMETER)
            .unlockedBy(() -> ModdedItems.RESISTIVE_COIL)
            .viaShaped(b -> b
                    .pattern(" R ")
                    .pattern("CSI")
                    .define('R', Items.COMPASS)
                    .define('C', copperSheet())
                    .define('S', resistiveCoil())
                    .define('I', ironSheet())),

    INSULATED_COPPER_WIRE = create(ModdedItems.INSULATED_COPPER_WIRE)
            .unlockedBy(() -> ModdedItems.WIRE)
            .viaShapeless(b -> b
                    .requires(copperWire())
                    .requires(Items.DRIED_KELP)),

    COPPER_CORD = create(ModdedItems.CORD)
            .unlockedBy(() -> ModdedItems.INSULATED_COPPER_WIRE)
            .viaShapeless(b -> b
                    .requires(ModdedItems.INSULATED_COPPER_WIRE)
                    .requires(ModdedItems.INSULATED_COPPER_WIRE)
                    .requires(Items.DRIED_KELP)),

    CORD_JUNCTION = create(ModdedBlocks.CORD_JUNCTION)
            .unlockedBy(() -> ModdedItems.CORD)
            .returns(3)
            .viaShapeless(b -> b
                    .requires(conductiveCasing())
                    .requires(ironNugget())
                    .requires(ironSheet())),

    POWER_PLUG = create(ModdedBlocks.SOCKET)
            .unlockedBy(() -> ModdedItems.CORD)
            .viaShapeless(b -> b
                    .requires(copperSheet())
                    .requires(conductiveCasing())
                    .requires(brassSheet()))

            ;

    public CraftingRecipes(PackOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "Power Grid's Crafting Recipes";
    }
}
