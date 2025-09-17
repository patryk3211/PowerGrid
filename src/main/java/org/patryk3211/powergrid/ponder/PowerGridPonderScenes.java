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
package org.patryk3211.powergrid.ponder;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.ponder.scenes.*;

public class PowerGridPonderScenes {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);

        HELPER.addStoryBoard(ModdedBlocks.VOLTAGE_METER, "gauges", GaugeScenes::voltage);
        HELPER.addStoryBoard(ModdedBlocks.CURRENT_METER, "gauges", GaugeScenes::current);

        HELPER.forComponents(ModdedBlocks.HEATING_COIL)
                .addStoryBoard("heating_coil/basic", DeviceScenes::heatingCoilBasic, PowerGridPonderTags.ELECTRIC_DEVICES)
                .addStoryBoard("heating_coil/speed", DeviceScenes::heatingCoilSpeed, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.forComponents(ModdedBlocks.GENERATOR_ROTOR)
                .addStoryBoard("generator/rotor", GeneratorScenes::rotor, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator", GeneratorScenes::generator, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.WINDING, ModdedItems.COPPER_COIL)
                .addStoryBoard("generator/winding", GeneratorScenes::winding, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/parallel_windings", GeneratorScenes::parallelWinding, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator", GeneratorScenes::generator, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.GENERATOR_CLUTCH)
                .addStoryBoard("generator/clutch", GeneratorScenes::clutch, PowerGridPonderTags.GENERATOR_ASSEMBLY, AllCreatePonderTags.KINETIC_APPLIANCES)
                .addStoryBoard("generator/generator", GeneratorScenes::generator, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.GENERATOR_HOUSING, ModdedBlocks.VERTICAL_GENERATOR_HOUSING)
                .addStoryBoard("generator/housing", GeneratorScenes::housing, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.GENERATOR_INDUCTION_ROTOR, ModdedBlocks.GENERATOR_COMMUTATOR, ModdedBlocks.GENERATOR_VERTICAL_COMMUTATOR)
                .addStoryBoard("generator/inductive", GeneratorScenes::inductive, PowerGridPonderTags.GENERATOR_ASSEMBLY, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.forComponents(ModdedItems.WIRE, ModdedItems.IRON_WIRE, ModdedItems.GOLDEN_WIRE)
                .addStoryBoard("wire/simple", WireScenes::simple)
                .addStoryBoard("wire/voltage_drop", WireScenes::voltageDrop);

        HELPER.forComponents(ModdedBlocks.TRANSFORMER_CORE)
                .addStoryBoard("transformer/sizes", DeviceScenes::transformerSizes, PowerGridPonderTags.ELECTRIC_RELAYS)
                .addStoryBoard("transformer/winding", DeviceScenes::transformerWinding, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.VARIAC, "variac", RelayScenes::variac, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.RHEOSTAT, "rheostat", RelayScenes::rheostat, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.RESISTOR, "power_resistor", RelayScenes::powerResistor, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.forComponents(ModdedBlocks.LIGHT_FIXTURE, ModdedItems.LIGHT_BULB, ModdedItems.LV_LIGHT_BULB)
                .addStoryBoard("lightbulb", DeviceScenes::light, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(ModdedItems.GROWTH_LAMP, "growth_lamp", DeviceScenes::growthLamp, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.forComponents(ModdedBlocks.WIRE_CONNECTOR, ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                .addStoryBoard("wire/connector", WireScenes::connector, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.DEVICE_CONNECTOR, "device_connector", RelayScenes::deviceConnector, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.ELECTRIC_MOTOR, "motor", DeviceScenes::motor, AllCreatePonderTags.KINETIC_SOURCES, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(ModdedBlocks.SERVO, "servo", DeviceScenes::servo, AllCreatePonderTags.KINETIC_SOURCES, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.forComponents(ModdedItems.MAGNET)
                .addStoryBoard("magnet", MagnetScenes::magnet)
                .addStoryBoard("lightning_attractor", MagnetScenes::lightningAttractor);
        helper.addStoryBoard(new ResourceLocation("lightning_rod"), "lightning_attractor", MagnetScenes::lightningAttractor);

        HELPER.addStoryBoard(ModdedBlocks.BASIN_HEATER, "basin_heater", DeviceScenes::basinHeater, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.LV_SWITCH, "switch", RelayScenes.switchSceneFor(ModdedBlocks.LV_SWITCH, "lv_switch"), PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.LV_BUTTON, "switch", RelayScenes.switchSceneFor(ModdedBlocks.LV_BUTTON, "lv_button"), PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.MV_SWITCH, "switch", RelayScenes.switchSceneFor(ModdedBlocks.MV_SWITCH, "mv_switch"), PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.HV_SWITCH, "hv_switch", RelayScenes::hvSwitch, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.forComponents(ModdedBlocks.CONTACTOR)
                .addStoryBoard("contactor", RelayScenes::contactor, PowerGridPonderTags.ELECTRIC_RELAYS)
                .addStoryBoard("contactor_stack", RelayScenes::contactorStack, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.FUSE_HOLDER, "fuse", RelayScenes::fuse, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.ALARM_BELL, "bell", DeviceScenes::bell, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.ELECTROMAGNET, "electromagnet", DeviceScenes::electromagnet, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.BATTERY, "battery", DeviceScenes::battery);
    }
}
