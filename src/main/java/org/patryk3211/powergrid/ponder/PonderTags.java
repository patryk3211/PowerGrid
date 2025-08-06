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

import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderTag;
import com.simibubi.create.infrastructure.ponder.AllPonderTags;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

public class PonderTags {
    public static final PonderTag
        GENERATOR_ASSEMBLY = create("generator_assembly")
            .item(ModdedBlocks.GENERATOR_ROTOR)
            .defaultLang("Generator Parts", "Components which can be used to build a generator")
            .addToIndex();

    public static final PonderTag
        ELECTRIC_RELAYS = create("electric_relays")
            .item(ModdedBlocks.MV_SWITCH)
            .defaultLang("Electric Relays", "Components which help guide electricity in the right direction")
            .addToIndex();

    public static final PonderTag
        ELECTRIC_DEVICES = create("electric_devices")
            .item(ModdedBlocks.ELECTRIC_MOTOR)
            .defaultLang("Electric Devices", "Components which use electricity to do something")
            .addToIndex();

    private static PonderTag create(String id) {
        return new PonderTag(PowerGrid.asResource(id));
    }

    public static void register() {
        PonderRegistry.TAGS.forTag(GENERATOR_ASSEMBLY)
                .add(ModdedBlocks.GENERATOR_ROTOR)
                .add(ModdedBlocks.GENERATOR_CLUTCH)
                .add(ModdedBlocks.GENERATOR_INDUCTION_ROTOR)
                .add(ModdedBlocks.GENERATOR_COMMUTATOR)
                .add(ModdedItems.COPPER_COIL)
                .add(ModdedBlocks.GENERATOR_HOUSING);

        PonderRegistry.TAGS.forTag(ELECTRIC_RELAYS)
                .add(ModdedBlocks.LV_SWITCH)
                .add(ModdedBlocks.LV_BUTTON)
                .add(ModdedBlocks.MV_SWITCH)
                .add(ModdedBlocks.HV_SWITCH)
                .add(ModdedBlocks.CONTACTOR)
                .add(ModdedBlocks.SPARK_GAP)
                .add(ModdedBlocks.FUSE_HOLDER)
                .add(ModdedBlocks.TRANSFORMER_CORE)
                .add(ModdedBlocks.VARIAC)
                .add(ModdedBlocks.WIRE_CONNECTOR)
                .add(ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                .add(ModdedBlocks.DEVICE_CONNECTOR);

        PonderRegistry.TAGS.forTag(ELECTRIC_DEVICES)
                .add(ModdedBlocks.ELECTRIC_MOTOR)
                .add(ModdedBlocks.SERVO)
                .add(ModdedBlocks.HEATING_COIL)
                .add(ModdedBlocks.ELECTRIC_FAN)
                .add(ModdedBlocks.BASIN_HEATER)
                .add(ModdedItems.LIGHT_BULB)
                .add(ModdedItems.GROWTH_LAMP);

        PonderRegistry.TAGS.forTag(AllPonderTags.KINETIC_APPLIANCES)
                .add(ModdedBlocks.GENERATOR_CLUTCH);

        PonderRegistry.TAGS.forTag(AllPonderTags.KINETIC_SOURCES)
                .add(ModdedBlocks.ELECTRIC_MOTOR)
                .add(ModdedBlocks.SERVO);
    }
}
