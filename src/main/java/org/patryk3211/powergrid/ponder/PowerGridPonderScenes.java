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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
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
        HELPER.addStoryBoard(ModdedBlocks.POWER_METER, "power_gauge", GaugeScenes::power);

        HELPER.addStoryBoard(ModdedBlocks.PLOTTER, "plotter", DeviceScenes::plotter);

        HELPER.forComponents(ModdedBlocks.HEATING_COIL)
                .addStoryBoard("heating_coil/basic", DeviceScenes::heatingCoilBasic, PowerGridPonderTags.ELECTRIC_DEVICES)
                .addStoryBoard("heating_coil/speed", DeviceScenes::heatingCoilSpeed, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.forComponents(ModdedBlocks.WINDING, ModdedItems.COPPER_COIL)
                .addStoryBoard("generator/winding", GeneratorScenes::winding, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/parallel_windings", GeneratorScenes::parallelWinding, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_1", GeneratorScenes::generator1, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_2", GeneratorScenes::generator2, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_3", GeneratorScenes::generator3, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.GENERATOR_CLUTCH)
                .addStoryBoard("generator/clutch", GeneratorScenes::clutch, PowerGridPonderTags.GENERATOR_ASSEMBLY, AllCreatePonderTags.KINETIC_APPLIANCES)
                .addStoryBoard("generator/generator_1", GeneratorScenes::generator1, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_2", GeneratorScenes::generator2, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_3", GeneratorScenes::generator3, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.GENERATOR_HOUSING, ModdedBlocks.VERTICAL_GENERATOR_HOUSING)
                .addStoryBoard("generator/housing", GeneratorScenes::housing, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.GENERATOR_INDUCTION_ROTOR, ModdedBlocks.GENERATOR_COMMUTATOR, ModdedBlocks.GENERATOR_VERTICAL_COMMUTATOR)
                .addStoryBoard("generator/rotor", GeneratorScenes::rotor, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_1", GeneratorScenes::generator1, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_2", GeneratorScenes::generator2, PowerGridPonderTags.GENERATOR_ASSEMBLY)
                .addStoryBoard("generator/generator_3", GeneratorScenes::generator3, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.addStoryBoard(ModdedItems.STRING_LIGHT_CORD, "wire/string_lights", WireScenes::stringLights);
        HELPER.addStoryBoard(ModdedItems.INSULATED_COPPER_WIRE, "wire/insulated_wire", WireScenes::insulatedWire);
        HELPER.forComponents(ModdedItems.WIRE, ModdedItems.IRON_WIRE, ModdedItems.GOLDEN_WIRE, ModdedItems.INSULATED_COPPER_WIRE)
                .addStoryBoard("wire/simple", WireScenes::simple)
                .addStoryBoard("wire/voltage_drop", WireScenes::voltageDrop);
        HELPER.forComponents(ModdedItems.CORD, ModdedItems.STRING_LIGHT_CORD)
                .addStoryBoard("wire/cord", WireScenes::cord);

        HELPER.forComponents(ModdedBlocks.GROUNDING_ROD)
                .addStoryBoard("ground1", WireScenes::grounding, PowerGridPonderTags.ELECTRIC_RELAYS)
                .addStoryBoard("ground2", WireScenes::improvedGround, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.forComponents(ModdedBlocks.TRANSFORMER_CORE)
                .addStoryBoard("transformer/sizes", DeviceScenes::transformerSizes, PowerGridPonderTags.ELECTRIC_RELAYS)
                .addStoryBoard("transformer/winding", DeviceScenes::transformerWinding, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.VARIAC, "variac", RelayScenes::variac, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.RHEOSTAT, "rheostat", RelayScenes::rheostat, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.RESISTOR, "power_resistor", RelayScenes::powerResistor, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.forComponents(ModdedBlocks.CARBON_PILE_COIL)
                .addStoryBoard("carbon_pile", RelayScenes::carbonPile, PowerGridPonderTags.ELECTRIC_RELAYS)
                .addStoryBoard("generator/generator_3", GeneratorScenes::generator3, PowerGridPonderTags.GENERATOR_ASSEMBLY);

        HELPER.forComponents(ModdedBlocks.LIGHT_FIXTURE, ModdedItems.LIGHT_BULB, ModdedItems.LV_LIGHT_BULB)
                .addStoryBoard("lightbulb", DeviceScenes::light, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(ModdedItems.GROWTH_LAMP, "growth_lamp", DeviceScenes::growthLamp, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.forComponents(ModdedBlocks.WIRE_CONNECTOR, ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                .addStoryBoard("wire/connector", WireScenes::connector, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.DEVICE_CONNECTOR, "device_connector", RelayScenes::deviceConnector, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.ELECTRIC_MOTOR, "motor", DeviceScenes::motor, AllCreatePonderTags.KINETIC_SOURCES, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(ModdedBlocks.CONSTANT_SPEED_MOTOR, "constant_speed_motor", DeviceScenes::constantSpeedMotor, AllCreatePonderTags.KINETIC_SOURCES, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(ModdedBlocks.SERVO, "servo", DeviceScenes::servo, AllCreatePonderTags.KINETIC_SOURCES, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.ELECTRIC_FAN, "electric_fan", DeviceScenes::electricFan, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(AllBlocks.ENCASED_FAN, "encased_fan", DeviceScenes::encasedFan);
        HELPER.forComponents(ModdedBlocks.SOLAR_PANEL)
                .addStoryBoard("solar_panel/solar_panel_conditions", DeviceScenes::solarPanel, PowerGridPonderTags.ELECTRIC_DEVICES)
                .addStoryBoard("solar_panel/solar_panel_power_creation", DeviceScenes::solarPanel2, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.addStoryBoard(ModdedBlocks.SOLAR_PANEL_BEARING, "solar_panel_bearing", DeviceScenes::solarPanelBearing, PowerGridPonderTags.ELECTRIC_DEVICES, AllCreatePonderTags.KINETIC_SOURCES);

        HELPER.forComponents(ModdedItems.MAGNET)
                .addStoryBoard("magnet", MagnetScenes::magnet)
                .addStoryBoard("lightning_attractor", MagnetScenes::lightningAttractor);
        helper.addStoryBoard(new ResourceLocation("lightning_rod"), "lightning_attractor", MagnetScenes::lightningAttractor);

        HELPER.addStoryBoard(ModdedBlocks.BASIN_HEATER, "basin_heater", DeviceScenes::basinHeater, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.LV_SWITCH, "switch", RelayScenes.switchSceneFor(ModdedBlocks.LV_SWITCH, "lv_switch"), PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.LV_BUTTON, "switch", RelayScenes.switchSceneFor(ModdedBlocks.LV_BUTTON, "lv_button"), PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.MV_SWITCH, "switch", RelayScenes.switchSceneFor(ModdedBlocks.MV_SWITCH, "mv_switch"), PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.HV_SWITCH, "hv_switch", RelayScenes::hvSwitch, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.HV_BREAKER, "hv_breaker", RelayScenes::hvBreaker, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.REDSTONE_CONVERTER, "redstone_converter", RelayScenes::redstoneConverter);

        HELPER.forComponents(ModdedBlocks.CONTACTOR)
                .addStoryBoard("contactor", RelayScenes::contactor, PowerGridPonderTags.ELECTRIC_RELAYS)
                .addStoryBoard("contactor_stack", RelayScenes::contactorStack, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.FUSE_HOLDER, "fuse", RelayScenes::fuse, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.ALARM_BELL, "bell", DeviceScenes::bell, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.ELECTROMAGNET, "electromagnet", DeviceScenes::electromagnet, PowerGridPonderTags.ELECTRIC_DEVICES);

        HELPER.addStoryBoard(ModdedBlocks.BATTERY, "battery", DeviceScenes::battery);

        HELPER.addStoryBoard(ModdedBlocks.CORD_JUNCTION, "wire/cord_junction", WireScenes::cordJunction, PowerGridPonderTags.ELECTRIC_RELAYS);
        HELPER.addStoryBoard(ModdedBlocks.SOCKET, "wire/cord_socket", WireScenes::cordSocket, PowerGridPonderTags.ELECTRIC_RELAYS);

        HELPER.addStoryBoard(ModdedBlocks.CRT, "crt", DeviceScenes::crt, PowerGridPonderTags.ELECTRIC_DEVICES);
        HELPER.forComponents(ModdedBlocks.PUNCH_CARD_READER, ModdedItems.PUNCH_CARD)
                .addStoryBoard("punch_cards", DeviceScenes::punchCardReader, PowerGridPonderTags.ELECTRIC_DEVICES, AllCreatePonderTags.KINETIC_APPLIANCES);

        HELPER.addStoryBoard(ModdedItems.RESISTOR, "circuit/resistor", CircuitScenes::resistor, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedBlocks.VOLTAGE_METER, "circuit/voltage", CircuitScenes::voltageGauge, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedBlocks.CURRENT_METER, "circuit/current", CircuitScenes::currentGauge, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedItems.CAPACITOR, "circuit/capacitor", CircuitScenes::capacitor, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.forComponents(ModdedItems.RELAY, ModdedItems.RELAY_DPDT)
                .addStoryBoard("circuit/relay", CircuitScenes::relay, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(AllItems.ELECTRON_TUBE, "circuit/electron_tube", CircuitScenes::electronTube, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedItems.REDSTONE_RELAY, "circuit/redstone_relay", CircuitScenes::redstoneRelay, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(AllItems.COPPER_NUGGET, "circuit/via", CircuitScenes::via, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedItems.POTENTIOMETER, "circuit/potentiometer", CircuitScenes::potentiometer, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedItems.REGULATOR_TUBE, "circuit/regulator_tube", CircuitScenes::regulatorTube, PowerGridPonderTags.CIRCUIT_COMPONENTS);
        HELPER.addStoryBoard(ModdedItems.NEON_BULB, "circuit/neon", CircuitScenes::neonBulb, PowerGridPonderTags.CIRCUIT_COMPONENTS);
    }
}
