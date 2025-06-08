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

import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.ponder.scenes.DeviceScenes;
import org.patryk3211.powergrid.ponder.scenes.GaugeScenes;
import org.patryk3211.powergrid.ponder.scenes.GeneratorScenes;
import org.patryk3211.powergrid.ponder.scenes.WireScenes;

public class PonderIndex {
    static final PonderRegistrationHelper HELPER = new PonderRegistrationHelper(PowerGrid.MOD_ID);

    public static void register() {
        HELPER.addStoryBoard(ModdedBlocks.ANDESITE_VOLTAGE_METER, "gauges", GaugeScenes::voltage);
        HELPER.addStoryBoard(ModdedBlocks.BRASS_VOLTAGE_METER, "gauges", GaugeScenes::voltage);
        HELPER.addStoryBoard(ModdedBlocks.ANDESITE_CURRENT_METER, "gauges", GaugeScenes::current);
        HELPER.addStoryBoard(ModdedBlocks.BRASS_CURRENT_METER, "gauges", GaugeScenes::current);

        HELPER.forComponents(ModdedBlocks.HEATING_COIL)
                .addStoryBoard("heating_coil/basic", DeviceScenes::heatingCoilBasic)
                .addStoryBoard("heating_coil/speed", DeviceScenes::heatingCoilSpeed);

        HELPER.forComponents(ModdedBlocks.GENERATOR_ROTOR)
                .addStoryBoard("generator/rotor", GeneratorScenes::rotor)
                .addStoryBoard("generator/generator", GeneratorScenes::generator);

        HELPER.forComponents(ModdedBlocks.GENERATOR_COIL)
                .addStoryBoard("generator/coil", GeneratorScenes::coil)
                .addStoryBoard("generator/generator", GeneratorScenes::generator);

        HELPER.addStoryBoard(ModdedBlocks.GENERATOR_HOUSING, "generator/housing", GeneratorScenes::housing);

        HELPER.forComponents(ModdedItems.WIRE, ModdedItems.IRON_WIRE, ModdedItems.SILVER_WIRE)
                .addStoryBoard("wire/simple", WireScenes::simple)
                .addStoryBoard("wire/voltage_drop", WireScenes::voltageDrop);

        HELPER.forComponents(ModdedBlocks.TRANSFORMER_CORE)
                .addStoryBoard("transformer/sizes", DeviceScenes::transformerSizes)
                .addStoryBoard("transformer/winding", DeviceScenes::transformerWinding);

        HELPER.forComponents(ModdedBlocks.LIGHT_FIXTURE, ModdedItems.LIGHT_BULB)
                .addStoryBoard("lightbulb", DeviceScenes::light);
        HELPER.addStoryBoard(ModdedItems.GROWTH_LAMP, "growth_lamp", DeviceScenes::growthLamp);

        HELPER.forComponents(ModdedBlocks.WIRE_CONNECTOR, ModdedBlocks.HEAVY_WIRE_CONNECTOR)
                .addStoryBoard("wire/connector", WireScenes::connector);

        HELPER.addStoryBoard(ModdedBlocks.ELECTRIC_MOTOR, "motor", DeviceScenes::motor);
    }
}
