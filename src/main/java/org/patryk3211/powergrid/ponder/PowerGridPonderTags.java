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
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

public class PowerGridPonderTags {
    public static final ResourceLocation
            GENERATOR_ASSEMBLY = id("generator_assembly"),
            ELECTRIC_RELAYS = id("electric_relays"),
            ELECTRIC_DEVICES = id("electric_devices");

    private static ResourceLocation id(String name) {
        return PowerGrid.asResource(name);
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(GENERATOR_ASSEMBLY)
                .title("Generator Parts")
                .description("Components which can be used to build a generator")
                .item(ModdedBlocks.GENERATOR_ROTOR)
                .item(ModdedBlocks.GENERATOR_CLUTCH)
                .item(ModdedBlocks.GENERATOR_INDUCTION_ROTOR)
                .item(ModdedBlocks.GENERATOR_COMMUTATOR)
                .item(ModdedBlocks.GENERATOR_VERTICAL_COMMUTATOR)
                .item(ModdedItems.COPPER_COIL)
                .item(ModdedBlocks.GENERATOR_HOUSING)
                .item(ModdedBlocks.VERTICAL_GENERATOR_HOUSING)
                .addToIndex()
                .register();

        helper.registerTag(ELECTRIC_RELAYS)
                .title("Electricity Relays")
                .description("Components which help guide electricity in the right direction")
                .item(ModdedBlocks.LV_SWITCH)
                .item(ModdedBlocks.LV_BUTTON)
                .item(ModdedBlocks.MV_SWITCH)
                .item(ModdedBlocks.HV_SWITCH)
                .item(ModdedBlocks.CONTACTOR)
                .item(ModdedBlocks.SPARK_GAP)
                .item(ModdedBlocks.FUSE_HOLDER)
                .item(ModdedBlocks.TRANSFORMER_CORE)
                .item(ModdedBlocks.VARIAC)
                .item(ModdedBlocks.WIRE_CONNECTOR)
                .item(ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                .item(ModdedBlocks.DEVICE_CONNECTOR)
                .addToIndex()
                .register();

        helper.registerTag(ELECTRIC_DEVICES)
                .title("Electric Devices")
                .description("Components which use electricity to do something")
                .item(ModdedBlocks.ELECTRIC_MOTOR)
                .item(ModdedBlocks.SERVO)
                .item(ModdedBlocks.HEATING_COIL)
                .item(ModdedBlocks.ELECTRIC_FAN)
                .item(ModdedBlocks.BASIN_HEATER)
                .item(ModdedItems.LV_LIGHT_BULB)
                .item(ModdedItems.LIGHT_BULB)
                .item(ModdedItems.GROWTH_LAMP)
                .addToIndex()
                .register();

        helper.addToTag(AllCreatePonderTags.KINETIC_APPLIANCES)
                .add(ModdedBlocks.GENERATOR_CLUTCH.getId());

        helper.addToTag(AllCreatePonderTags.KINETIC_SOURCES)
                .add(ModdedBlocks.ELECTRIC_MOTOR.getId())
                .add(ModdedBlocks.SERVO.getId());

        helper.addToTag(AllCreatePonderTags.DISPLAY_SOURCES)
                .add(ModdedBlocks.VOLTAGE_METER.getId())
                .add(ModdedBlocks.CURRENT_METER.getId())
                .add(ModdedBlocks.GENERATOR_CLUTCH.getId())
                .add(ModdedBlocks.BATTERY.getId());
    }
}
