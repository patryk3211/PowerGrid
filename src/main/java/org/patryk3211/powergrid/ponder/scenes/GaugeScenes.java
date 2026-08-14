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
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.creative.CreativeResistorBlockEntity;
import org.patryk3211.powergrid.electricity.creative.CreativeSourceBlockEntity;
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

            scene.overlay().showText(60)
                    .text("It connects in parallel to your circuit")
                    .attachKeyFrame()
                    .placeNearTarget();
            scene.idle(70);
            scene.overlay().showText(80)
                    .text("If you were to connect it in series, virtually no current would be able to flow")
                    .placeNearTarget()
                    .attachKeyFrame()
                    .colored(PonderPalette.RED);
            scene.idle(90);
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

            scene.overlay().showText(90)
                    .text("If you were to connect it in parallel, huge currents might flow which can potentially damage the meter or nearby equipment")
                    .placeNearTarget()
                    .attachKeyFrame()
                    .colored(PonderPalette.RED);
            scene.idle(100);
        }

        scene.overlay().showText(60)
                .sharedText("gauge_range")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(gaugePos))
                .placeNearTarget();
        scene.idle(70);

        Vec3 blockSurface = util.vector().blockSurface(gaugePos, Direction.NORTH);
        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 80).withItem(AllItems.GOGGLES.asStack());
        scene.idle(7);
        scene.overlay().showText(80)
                .sharedText("gauge_goggles")
                .attachKeyFrame()
                .colored(PonderPalette.MEDIUM)
                .pointAt(blockSurface)
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 80)
                        .withItem(Items.NAME_TAG.getDefaultInstance()).rightClick();
        scene.idle(7);
        scene.overlay().showText(80)
                .sharedText("gauge_customize")
                .pointAt(blockSurface)
                .placeNearTarget()
                .attachKeyFrame();
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
                .sharedText("gauge_range")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(gauge))
                .placeNearTarget();
        scene.idle(70);

        Vec3 blockSurface = util.vector().blockSurface(gauge, Direction.NORTH);
        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 80).withItem(AllItems.GOGGLES.asStack());
        scene.idle(7);
        scene.overlay().showText(80)
                .sharedText("gauge_goggles")
                .attachKeyFrame()
                .colored(PonderPalette.MEDIUM)
                .pointAt(blockSurface)
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 80)
                .withItem(Items.NAME_TAG.getDefaultInstance()).rightClick();
        scene.idle(7);
        scene.overlay().showText(80)
                .sharedText("gauge_customize")
                .pointAt(blockSurface)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void energyMeter(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("energy_meter", "Monitoring Electricity usage with the Energy Meter");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(1.2f);

        var meter = util.grid().at(2, 1, 2);
        var source = util.grid().at(4, 1, 2);
        var pillars = util.select().fromTo(util.grid().at(0, 1, 3), util.grid().at(0, 1, 2))
                .add(util.select().position(4, 1, 1))
                .add(util.select().position(4, 1, 3));
        var connector1 = util.grid().at(4, 2, 1);
        var connector2 = util.grid().at(4, 2, 3);
        var connector3 = util.grid().at(0, 2, 3);
        var connector4 = util.grid().at(0, 2, 2);
        var comparator = util.grid().at(2, 1, 1);
        var nixie = util.grid().at(2, 1, 0);
        var resistor = util.grid().at(1, 1, 3);

        scene.showBasePlate();
        scene.addKeyframe();
        scene.idle(5);
        scene.world().showSection(util.select().position(meter), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(pillars, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().layer(2), Direction.DOWN);
        scene.idle(15);

        var mwire1 = scene.electric().connect(connector1, 0, meter, 0, DyeColor.RED);
        scene.idle(10);
        var mwire2 = scene.electric().connect(connector4, 0, meter, 1, DyeColor.RED);
        scene.idle(10);
        scene.electric().connect(connector2, 0, meter, 2, DyeColor.BLUE);
        scene.electric().connect(connector2, 0, connector3, 0, DyeColor.BLUE);
        scene.idle(10);
        scene.electric().connectInvisible(source, 0, connector1, 0);
        scene.electric().connectInvisible(source, 1, connector2, 0);
        scene.world().modifyBlockEntity(source, CreativeSourceBlockEntity.class, b -> {
            b.setValue(100000);
        });
        scene.world().modifyBlockEntity(resistor, CreativeResistorBlockEntity.class, b -> {
            b.setValue(3000);
        });
        scene.electric().connectInvisible(connector3, 0, resistor, 0);
        scene.electric().connectInvisible(connector4, 0, resistor, 1);
        scene.electric().tickFor(60);

        scene.overlay().showText(70)
                .text("This energy meter measures energy used over time")
                .pointAt(util.vector().blockSurface(meter, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showControls(util.select().position(meter).getCenter().add(0,0.5,0), Pointing.DOWN, 40).rightClick();
        scene.idle(10);

        scene.overlay().showText(90)
                .text("In the GUI you can see the usage in Wh or kWh by hovering over the gauges")
                .attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(100)
                .text("Caution, changing out the unit of measurement does not affect the previously measured value and only affects future measurements")
                .colored(PonderPalette.RED)
                .attachKeyFrame();
        scene.idle(110);

        scene.world().showSection(util.select().position(comparator), Direction.DOWN);
        scene.world().showSection(util.select().position(nixie), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(110)
                .text("The energy meter emits a redstone pulse through a comparator every 1 Wh or kWh depending on which mode it's set to")
                .pointAt(util.vector().blockSurface(meter, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(120);

        var delay = 20;
        scene.addKeyframe();
        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 1));
        scene.idle(1);
        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 0));
        scene.idle(delay);

        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 1));
        scene.idle(1);
        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 0));
        scene.idle(delay);

        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 1));
        scene.idle(1);
        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 0));
        scene.idle(delay);

        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 1));
        scene.idle(1);
        scene.world().toggleRedstonePower(util.select().position(comparator));
        scene.world().modifyBlockEntityNBT(util.select().position(nixie), NixieTubeBlockEntity.class,
                nbt -> nbt.putInt("RedstoneStrength", 0));
        scene.idle(delay);

        scene.world().hideSection(util.select().position(nixie), Direction.UP);
        scene.world().hideSection(util.select().position(comparator), Direction.UP);
        scene.idle(10);
        scene.electric().removeWire(mwire1);
        scene.electric().removeWire(mwire2);
        scene.idle(10);
        scene.electric().connect(connector1, 0, meter, 1, DyeColor.RED);
        scene.idle(10);
        scene.electric().connect(connector4, 0, meter, 0, DyeColor.RED);
        scene.idle(10);

        scene.overlay().showText(90)
                .text("You can also reverse the power and it will count backwards")
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }
}
