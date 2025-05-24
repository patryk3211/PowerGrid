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
package org.patryk3211.powergrid.ponder.scenes;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;

public class GaugeScenes {
    public static void voltage(SceneBuilder scene, SceneBuildingUtil util) {
        gauge(scene, util, true);
    }

    public static void current(SceneBuilder scene, SceneBuildingUtil util) {
        gauge(scene, util, false);
    }

    public static void gauge(SceneBuilder scene, SceneBuildingUtil util, boolean voltage) {
        var electric = ElectricInstructions.of(scene);
        var component = voltage ? "Voltage Gauge" : "Current Gauge";
        String title = "Monitoring Electricity using the " + component;
        scene.title(voltage ? "voltage_gauge" : "current_gauge", title);
        scene.configureBasePlate(0, 0, 5);

        BlockPos sourcePos = util.grid.at(2, 1, 4);
        BlockPos gaugePos = util.grid.at(2, 1, 2);
        if(!voltage) {
            scene.world.setBlock(gaugePos, ModdedBlocks.ANDESITE_CURRENT_METER.getDefaultState(), false);
        }

        scene.showBasePlate();
        scene.idle(5);

        scene.world.showSection(util.select.position(0, 1, 2), Direction.DOWN);
        scene.world.showSection(util.select.position(0, 2, 2), Direction.DOWN);
        scene.idle(2);
        scene.world.showSection(util.select.position(4, 1, 2), Direction.DOWN);
        scene.world.showSection(util.select.position(4, 2, 2), Direction.DOWN);
        scene.idle(2);

        scene.world.showSection(util.select.position(gaugePos), Direction.DOWN);
        scene.idle(2);

        electric.connect(util.grid.at(0, 2, 2), 0, gaugePos, 1);
        var wire2 = electric.connect(util.grid.at(4, 2, 2), 0, gaugePos, 0);
        electric.connectInvisible(util.grid.at(0, 2, 2), 0, sourcePos, 0);
        electric.connectInvisible(util.grid.at(4, 2, 2), 0, sourcePos, 1);
        scene.idle(2);

        if(voltage) {
            electric.tickFor(10);
            electric.setSource(sourcePos, 5);
            scene.idle(10);

            scene.overlay.showText(80)
                    .text("The " + component + " displays the voltage potential between it's terminals")
                    .attachKeyFrame()
                    .pointAt(util.vector.topOf(gaugePos))
                    .placeNearTarget();

            scene.idle(40);
            electric.tickFor(10);
            electric.setSource(sourcePos, 15);
            scene.idle(40);
            scene.effects.indicateSuccess(gaugePos);
            scene.idle(10);
        } else {
            scene.overlay.showText(60)
                    .text("The " + component + " displays the current flowing through it's terminals")
                    .attachKeyFrame()
                    .pointAt(util.vector.topOf(gaugePos))
                    .placeNearTarget();
            scene.idle(60);

            electric.removeWire(wire2);
            scene.idle(10);

            var heater1Pos = util.grid.at(3, 1, 1);
            var heater2Pos = util.grid.at(3, 1, 3);
            scene.world.setBlock(heater1Pos, ModdedBlocks.HEATING_COIL.getDefaultState(), true);
            scene.world.setBlock(heater2Pos, ModdedBlocks.HEATING_COIL.getDefaultState(), true);
            scene.world.showSection(util.select.position(heater1Pos), Direction.DOWN);
            scene.world.showSection(util.select.position(heater2Pos), Direction.DOWN);
            scene.idle(5);

            electric.connect(util.grid.at(4, 2,2), 0, heater1Pos, 0);
            electric.connect(util.grid.at(4, 2,2), 0, heater2Pos, 0);
            electric.connect(gaugePos, 0, heater1Pos, 1);
            electric.connect(gaugePos, 0, heater2Pos, 1);

            electric.setSource(sourcePos, 20);
            electric.tickFor(10);
            scene.idle(40);

            scene.overlay.showText(80)
                    .text("Current gauges are in series with the circuit so they can have an effect on the transferred power")
                    .attachKeyFrame()
                    .pointAt(util.vector.topOf(gaugePos))
                    .placeNearTarget();
            scene.idle(40);
            electric.setSource(sourcePos, 10);
            electric.tickFor(10);
            scene.idle(50);
        }

        Vec3d blockSurface = util.vector.blockSurface(gaugePos, Direction.NORTH);
        scene.overlay.showControls(new InputWindowElement(blockSurface, Pointing.RIGHT).withItem(AllItems.GOGGLES.asStack()), 80);
        scene.idle(7);
        scene.overlay.showText(80)
                .text("When wearing Engineers' Goggles, the player can get more detailed information from the Gauge")
                .attachKeyFrame()
                .colored(PonderPalette.MEDIUM)
                .pointAt(blockSurface)
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
        electric.unload();
    }
}
