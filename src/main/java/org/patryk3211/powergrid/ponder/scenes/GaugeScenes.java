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
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.gauge.CurrentGaugeBlockEntity;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class GaugeScenes {
    public static void voltage(SceneBuilder scene, SceneBuildingUtil util) {
        gauge(scene, util, true);
    }

    public static void current(SceneBuilder scene, SceneBuildingUtil util) {
        gauge(scene, util, false);
    }

    public static void gauge(SceneBuilder builder, SceneBuildingUtil util, boolean voltage) {
        var scene = new PowerGridSceneBuilder(builder);
        var component = voltage ? "Voltage Gauge" : "Current Gauge";
        String title = "Monitoring Electricity using the " + component;
        scene.title(voltage ? "voltage_gauge" : "current_gauge", title);
        scene.configureBasePlate(0, 0, 5);

        BlockPos sourcePos = util.grid().at(2, 1, 4);
        BlockPos gaugePos = util.grid().at(2, 1, 2);
        if(!voltage) {
            scene.world().setBlock(gaugePos, ModdedBlocks.CURRENT_METER.getDefaultState(), false);
            scene.world().modifyBlockEntityNBT(util.select().position(2, 1, 2),
                    CurrentGaugeBlockEntity.class, tag -> tag.putInt("ScrollValue", 2));
        }

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().position(0, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(0, 2, 2), Direction.DOWN);
        scene.idle(2);
        scene.world().showSection(util.select().position(4, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 2, 2), Direction.DOWN);
        scene.idle(2);

        scene.world().showSection(util.select().position(gaugePos), Direction.DOWN);
        scene.idle(2);

        scene.electric().connect(util.grid().at(0, 2, 2), 0, gaugePos, 1);
        var wire2 = scene.electric().connect(util.grid().at(4, 2, 2), 0, gaugePos, 0);
        scene.electric().connectInvisible(util.grid().at(0, 2, 2), 0, sourcePos, 0);
        scene.electric().connectInvisible(util.grid().at(4, 2, 2), 0, sourcePos, 1);
        scene.idle(2);

        if(voltage) {
            scene.electric().tickFor(10);
            scene.electric().setSource(sourcePos, 5);
            scene.idle(10);

            scene.overlay().showText(80)
                    .text("The " + component + " displays the voltage potential between it's terminals")
                    .attachKeyFrame()
                    .pointAt(util.vector().topOf(gaugePos))
                    .placeNearTarget();

            scene.idle(40);
            scene.electric().tickFor(10);
            scene.electric().setSource(sourcePos, 15);
            scene.idle(40);
            scene.effects().indicateSuccess(gaugePos);
            scene.idle(10);
        } else {
            scene.overlay().showText(60)
                    .text("The " + component + " displays the current flowing through it's terminals")
                    .attachKeyFrame()
                    .pointAt(util.vector().topOf(gaugePos))
                    .placeNearTarget();
            scene.idle(60);

            scene.electric().removeWire(wire2);
            scene.idle(10);

            var heater1Pos = util.grid().at(3, 1, 1);
            var heater2Pos = util.grid().at(3, 1, 3);
            scene.world().setBlock(heater1Pos, ModdedBlocks.HEATING_COIL.getDefaultState(), true);
            scene.world().setBlock(heater2Pos, ModdedBlocks.HEATING_COIL.getDefaultState(), true);
            scene.world().showSection(util.select().position(heater1Pos), Direction.DOWN);
            scene.world().showSection(util.select().position(heater2Pos), Direction.DOWN);
            scene.idle(5);

            scene.electric().connect(util.grid().at(4, 2,2), 0, heater1Pos, 0);
            scene.electric().connect(util.grid().at(4, 2,2), 0, heater2Pos, 0);
            scene.electric().connect(gaugePos, 0, heater1Pos, 1);
            scene.electric().connect(gaugePos, 0, heater2Pos, 1);

            scene.electric().setSource(sourcePos, 20);
            scene.electric().tickFor(10);
            scene.idle(40);

            scene.overlay().showText(80)
                    .text("Current gauges are in series with the circuit so they can have an effect on the transferred power")
                    .attachKeyFrame()
                    .pointAt(util.vector().topOf(gaugePos))
                    .placeNearTarget();
            scene.idle(40);
            scene.electric().setSource(sourcePos, 10);
            scene.electric().tickFor(10);
            scene.idle(50);
        }

        scene.overlay().showText(60)
                .text("You can change the gauge's range by clicking on top of it")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(gaugePos))
                .placeNearTarget();
        scene.idle(70);

        Vec3 blockSurface = util.vector().blockSurface(gaugePos, Direction.NORTH);
        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 80).withItem(AllItems.GOGGLES.asStack());
        scene.idle(7);
        scene.overlay().showText(80)
                .text("When wearing Engineers' Goggles, the player can get more detailed information from the Gauge")
                .attachKeyFrame()
                .colored(PonderPalette.MEDIUM)
                .pointAt(blockSurface)
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void power(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("power_gauge", "Monitoring Electricity using the Power Gauge");
        scene.configureBasePlate(0, 0, 5);

        var gauge = util.grid().at(2, 1, 2);
        var common = util.grid().at(4, 1, 3);
        var input = util.grid().at(4, 1, 2);
        var bulb = util.grid().at(0, 2, 2);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().fromTo(input, common), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(bulb.below(), bulb), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(gauge), Direction.DOWN);
        scene.idle(5);

        scene.electric().addSource(common, 0, 0);
        scene.electric().connect(common, 0, gauge, 2);
        scene.electric().connect(input, 0, gauge, 0);
        scene.electric().connect(gauge, 1, bulb, 0);
        scene.electric().connect(gauge, 2, bulb, 1);
        scene.idle(10);

        scene.overlay().showText(100)
                .text("The Power Gauge can be used to measure power going into a section of the electrical network")
                .pointAt(util.vector().of(2.5, 1.5, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.electric().addSource(input, 0, 245);
        scene.electric().tickFor(10);
        scene.world().modifyBlock(bulb, state -> state.setValue(LightFixtureBlock.POWER, 2), false);
        scene.idle(20);
        scene.effects().indicateSuccess(gauge);
        scene.idle(20);

        scene.overlay().showText(60)
                .text("You can change the gauge's range by clicking on top of it")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(gauge))
                .placeNearTarget();
        scene.idle(70);

        Vec3 blockSurface = util.vector().blockSurface(gauge, Direction.NORTH);
        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 80).withItem(AllItems.GOGGLES.asStack());
        scene.idle(7);
        scene.overlay().showText(80)
                .text("When wearing Engineers' Goggles, the player can get more detailed information from the Gauge")
                .attachKeyFrame()
                .colored(PonderPalette.MEDIUM)
                .pointAt(blockSurface)
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }
}
