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

import com.simibubi.create.AllItems;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

public class PowerGridPonderTags {
    public static final ResourceLocation
            GENERATOR_ASSEMBLY = id("generator_assembly"),
            ELECTRIC_RELAYS = id("electric_relays"),
            ELECTRIC_DEVICES = id("electric_devices"),
            CIRCUIT_COMPONENTS = id("circuit_components");

    private static ResourceLocation id(String name) {
        return PowerGrid.asResource(name);
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(GENERATOR_ASSEMBLY)
                .title("Generator Parts")
                .description("Components which can be used to build a generator")
                .item(ModdedBlocks.GENERATOR_INDUCTION_ROTOR, true, false)
                .addToIndex()
                .register();

        helper.registerTag(ELECTRIC_RELAYS)
                .title("Electricity Relays")
                .description("Components which help guide electricity in the right direction")
                .item(ModdedBlocks.MV_SWITCH, true, false)
                .addToIndex()
                .register();

        helper.registerTag(ELECTRIC_DEVICES)
                .title("Electric Devices")
                .description("Components which use electricity to do something")
                .item(ModdedBlocks.ELECTRIC_MOTOR, true, false)
                .addToIndex()
                .register();

        helper.registerTag(CIRCUIT_COMPONENTS)
                .title("Circuit Components")
                .description("Components which can be placed on a circuit")
                .item(ModdedBlocks.CIRCUIT_BOARD, true, true)
                .addToIndex()
                .register();

        PonderTagRegistrationHelper<RegistryEntry<?>> HELPER = helper.withKeyFunction(RegistryEntry::getId);
        HELPER.addToTag(GENERATOR_ASSEMBLY)
                .add(ModdedBlocks.GENERATOR_CLUTCH)
                .add(ModdedBlocks.GENERATOR_INDUCTION_ROTOR)
                .add(ModdedBlocks.GENERATOR_COMMUTATOR)
                .add(ModdedBlocks.GENERATOR_VERTICAL_COMMUTATOR)
                .add(ModdedItems.COPPER_COIL)
                .add(ModdedBlocks.GENERATOR_HOUSING)
                .add(ModdedBlocks.VERTICAL_GENERATOR_HOUSING);

        HELPER.addToTag(ELECTRIC_RELAYS)
                .add(ModdedBlocks.LV_SWITCH)
                .add(ModdedBlocks.LV_BUTTON)
                .add(ModdedBlocks.MV_SWITCH)
                .add(ModdedBlocks.HV_SWITCH)
                .add(ModdedBlocks.HV_BREAKER)
                .add(ModdedBlocks.CONTACTOR)
                .add(ModdedBlocks.SPARK_GAP)
                .add(ModdedBlocks.FUSE_HOLDER)
                .add(ModdedBlocks.TRANSFORMER_CORE)
                .add(ModdedBlocks.VARIAC)
                .add(ModdedBlocks.RHEOSTAT)
                .add(ModdedBlocks.RESISTOR)
                .add(ModdedBlocks.CARBON_PILE_COIL)
                .add(ModdedBlocks.WIRE_CONNECTOR)
                .add(ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                .add(ModdedBlocks.DEVICE_CONNECTOR)
                .add(ModdedBlocks.CORD_JUNCTION)
                .add(ModdedBlocks.SOCKET)
                .add(ModdedBlocks.GROUNDING_ROD);

        HELPER.addToTag(ELECTRIC_DEVICES)
                .add(ModdedBlocks.ELECTRIC_MOTOR)
                .add(ModdedBlocks.CONSTANT_SPEED_MOTOR)
                .add(ModdedBlocks.SERVO)
                .add(ModdedBlocks.HEATING_COIL)
                .add(ModdedBlocks.ELECTRIC_FAN)
                .add(ModdedBlocks.BASIN_HEATER)
                .add(ModdedItems.LV_LIGHT_BULB)
                .add(ModdedItems.LIGHT_BULB)
                .add(ModdedItems.GROWTH_LAMP)
                .add(ModdedBlocks.PLOTTER)
                .add(ModdedBlocks.CRT)
                .add(ModdedBlocks.PUNCH_CARD_READER);

        HELPER.addToTag(CIRCUIT_COMPONENTS)
                .add(ModdedBlocks.WIRE_CONNECTOR)
                .add(AllItems.COPPER_NUGGET)
                .add(ModdedItems.RESISTOR)
                .add(ModdedItems.DIODE)
                .add(ModdedItems.CAPACITOR)
                .add(ModdedItems.COPPER_COIL)
                .add(ModdedItems.RELAY)
                .add(ModdedItems.REDSTONE_RELAY)
                .add(AllItems.ELECTRON_TUBE)
                .add(ModdedItems.REGULATOR_TUBE)
                .add(ModdedItems.BARRETTER_TUBE)
                .add(ModdedItems.NEON_BULB)
                .add(ModdedBlocks.LV_SWITCH)
                .add(ModdedBlocks.LV_BUTTON)
                .add(ModdedItems.POTENTIOMETER)
                .add(ModdedBlocks.VOLTAGE_METER)
                .add(ModdedBlocks.CURRENT_METER);

        HELPER.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
                .add(ModdedBlocks.GENERATOR_CLUTCH)
                .add(ModdedBlocks.PLOTTER)
                .add(ModdedBlocks.PUNCH_CARD_READER);

        HELPER.addToTag(AllCreatePonderTags.KINETIC_SOURCES)
                .add(ModdedBlocks.ELECTRIC_MOTOR)
                .add(ModdedBlocks.CONSTANT_SPEED_MOTOR)
                .add(ModdedBlocks.SERVO);

        HELPER.addToTag(AllCreatePonderTags.DISPLAY_SOURCES)
                .add(ModdedBlocks.VOLTAGE_METER)
                .add(ModdedBlocks.CURRENT_METER)
                .add(ModdedBlocks.POWER_METER)
                .add(ModdedBlocks.GENERATOR_CLUTCH)
                .add(ModdedBlocks.BATTERY);
    }
}
