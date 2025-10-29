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

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.IRedstoneComponent;
import org.patryk3211.powergrid.circuits.components.PotentiometerComponent;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class CircuitScenes {
    public static void resistor(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_resistor", "Small resistors");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Resistors are one of the simplest electrical components, they limit current going through them.")
                .pointAt(util.vector().of(1.5, 1.25, 1.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(100)
                .text("You can calculate that current using using the V = I * R equation, where I is current, V is voltage and R is resistance")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(110);

        scene.markAsFinished();
    }

    public static void voltageGauge(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_vgauge", "Compact Voltage Gauge");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("The Voltage Gauge can be placed on a circuit board.")
                .pointAt(util.vector().of(1.5, 1.375, 1.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(80);

        scene.electric().addSource(util.grid().at(1, 1, 1), 0, 5);
        scene.electric().addSource(util.grid().at(1, 1, 1), 1, 0);
        scene.electric().tickFor(5);
        scene.idle(20);
        scene.effects().indicateSuccess(util.grid().at(1, 1, 1));
        scene.idle(20);

        scene.world().hideSection(util.select().position(1, 1, 1), Direction.UP);
        scene.idle(20);
        var c2 = scene.world().showIndependentSection(util.select().position(1, 1, 0), Direction.DOWN);
        scene.world().moveSection(c2, util.vector().of(0, 0, 1), 0);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("When placed on the indicated edge, it will emit a redstone signal proportional to the dial value")
                .pointAt(util.vector().of(1.5, 1.1875, 1.0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void currentGauge(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_cgauge", "Compact Current Gauge");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("The Current Gauge can be placed on a circuit board.")
                .pointAt(util.vector().of(1.5, 1.375, 1.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(80);

        scene.electric().addSource(util.grid().at(1, 1, 1), 0, 0.025f);
        scene.electric().addSource(util.grid().at(1, 1, 1), 1, 0);
        scene.electric().tickFor(5);
        scene.idle(20);
        scene.effects().indicateSuccess(util.grid().at(1, 1, 1));
        scene.idle(20);

        scene.world().hideSection(util.select().position(1, 1, 1), Direction.UP);
        scene.idle(20);
        var c2 = scene.world().showIndependentSection(util.select().position(1, 1, 0), Direction.DOWN);
        scene.world().moveSection(c2, util.vector().of(0, 0, 1), 0);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("When placed on the indicated edge, it will emit a redstone signal proportional to the dial value")
                .pointAt(util.vector().of(1.5, 1.1875, 1.0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void capacitor(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_capacitor", "Capacitor");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 1), Direction.DOWN);
        scene.world().showSection(util.select().position(1, 1, 0), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The capacitor is an electrical component which can store a small amount of electricity")
                .pointAt(util.vector().of(1.125, 1.375, 1.675))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        var wire1 = scene.electric().connect(util.grid().at(2, 1, 1), 0, util.grid().at(1, 1, 1), 0);
        var wire2 = scene.electric().connect(util.grid().at(1, 1, 0), 0, util.grid().at(1, 1, 1), 1);
        scene.electric().addSource(util.grid().at(2, 1, 1), 0, 10);
        scene.electric().addSource(util.grid().at(1, 1, 0), 0, 0);
        scene.electric().tickFor(80);
        scene.idle(80);

        scene.electric().removeWire(wire1);
        scene.electric().removeWire(wire2);
        scene.electric().tickFor(160);
        scene.idle(60);

        scene.markAsFinished();
    }

    public static void relay(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_relay", "Relay");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(2.0f);

        var circuit = util.grid().at(2, 1, 2);
        var relay1 = util.grid().at(3, 1, 3);
        var relay2 = util.grid().at(3, 1, 1);
        var common = util.grid().at(1, 1, 2);
        var bulbGnd = util.grid().at(0, 1, 2);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(circuit), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 1, 1, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3, 1, 1, 3, 1, 3), Direction.DOWN);
        scene.idle(5);
        scene.electric().connect(relay1, 0, circuit, 0);
        scene.electric().connect(relay2, 0, circuit, 1);
        scene.electric().connect(util.grid().at(1, 1, 3), 1, circuit, 3);
        scene.electric().connect(common, 0, circuit, 2);
        scene.electric().connect(util.grid().at(1, 1, 1), 1, circuit, 4);
        scene.electric().connect(util.grid().at(1, 1, 3), 0, bulbGnd, 0);
        scene.electric().connect(util.grid().at(1, 1, 1), 0, bulbGnd, 0);

        scene.electric().addSource(relay1, 0, 0);
        scene.electric().addSource(bulbGnd, 0, 0);
        scene.electric().addSource(common, 0, 121);
        scene.world().modifyBlock(util.grid().at(1, 1, 3), state -> state.setValue(LightFixtureBlock.POWER, 2), false);
        scene.electric().tickFor(40);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("The Relay is a smaller version of the Contactor")
                .pointAt(util.vector().of(2.5, 1.125, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showText(80)
                .text("When you give it the configured voltage it will switch on the normally open terminal")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.electric().addSource(relay2, 0, 13);
        scene.world().modifyBlock(util.grid().at(1, 1, 3), state -> state.setValue(LightFixtureBlock.POWER, 0), false);
        scene.world().modifyBlock(util.grid().at(1, 1, 1), state -> state.setValue(LightFixtureBlock.POWER, 2), false);
        scene.electric().tickFor(10);
        scene.idle(30);

        scene.markAsFinished();
    }

    public static void electronTube(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_electron_tube", "Electron tube");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);

        scene.electric().addSource(util.grid().at(1, 1, 1), 3, 0);
        scene.electric().addSource(util.grid().at(1, 1, 1), 0, 50);
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.overlay().showText(70)
                .text("The Electron Tube is a non-linear electrical component which can amplify signals")
                .pointAt(util.vector().of(1.5, 1.125, 1.25))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(60)
                .text("To make it work, first you need to power its heating element")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.electric().addSource(util.grid().at(1, 1, 1), 2, 6);
        scene.electric().tickFor(10);
        scene.effects().indicateSuccess(util.grid().at(1, 1, 1));
        scene.idle(30);

        scene.overlay().showText(80)
                .text("It will then start conducting current from anode to cathode. This current can be controlled with the Grid pin")
                .pointAt(util.vector().of(1.625, 1.25, 1.875))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("A negative voltage can be applied to the grid. A small change there can result in a major change of the anode current")
                .pointAt(util.vector().of(1.25, 1.25, 1.875))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.electric().addSource(util.grid().at(1, 1, 1), 1, -5);
        scene.electric().tickFor(10);
        scene.idle(30);

        scene.markAsFinished();
    }

    public static void redstoneRelay(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_redstone_relay", "Redstone relay");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 0), Direction.DOWN);
        scene.idle(10);

        scene.electric().addSource(util.grid().at(1, 1, 1), 0, 6);
        scene.electric().addSource(util.grid().at(1, 1, 1), 1, 0);

        scene.overlay().showText(80)
                .text("The Redstone Relay can be used to switch electricity using a redstone signal")
                .pointAt(util.vector().of(1.5, 1.25, 1.25))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().toggleRedstonePower(util.select().position(1, 1, 0));
        scene.effects().indicateRedstone(util.grid().at(1, 1, 0));
        scene.world().modifyBlockEntity(util.grid().at(1, 1, 1), CircuitBoardBlockEntity.class, be -> {
            for(var placed : be.getComponents(IRedstoneComponent.class)) {
                var redstone = (IRedstoneComponent) placed.component;
                if(!redstone.isReceiver())
                    continue;
                redstone.receiveRedstone(placed, 15);
            }
        });
        scene.electric().tickFor(10);
        scene.idle(30);

        scene.overlay().showText(60)
                .text("It must be placed on the arrow-indicated edge")
                .pointAt(util.vector().of(1.5, 1.125, 1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.markAsFinished();
    }

    public static void via(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_via", "Vias");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(2.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Copper nuggets can be used to place Vias on the circuit board")
                .pointAt(util.vector().of(2.75, 1.125, 2.375))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("They are a single pad that can be used to move traces between layers")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().hideSection(util.select().position(2, 1, 2), Direction.UP);
        scene.idle(20);

        var middle = scene.world().showIndependentSection(util.select().position(2, 1, 1), Direction.DOWN);
        scene.world().moveSection(middle, util.vector().of(0, 0, 1), 0);
        scene.idle(5);
        var left = scene.world().showIndependentSection(util.select().position(3, 1, 1), Direction.DOWN);
        scene.world().moveSection(left, util.vector().of(0, 0, 1), 0);
        scene.idle(5);
        var right = scene.world().showIndependentSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.world().moveSection(right, util.vector().of(0, 0, 1), 0);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Vias can also be used to interconnect multiple circuit boards")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void potentiometer(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_potentiometer", "Potentiometer");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.electric().addSource(util.grid().at(1, 1, 1), 0, 10);
        scene.electric().addSource(util.grid().at(1, 1, 1), 1, 0);
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("The Potentiometer is a smaller version of the Rheostat")
                .pointAt(util.vector().of(1.5, 1.25, 1.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showText(60)
                .text("Its value can be tuned by interacting with it")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().modifyBlockEntity(util.grid().at(1, 1, 1), CircuitBoardBlockEntity.class, be -> {
            for(var placed : be.getSchematic().components()) {
                if(placed.component instanceof PotentiometerComponent) {
                    placed.set(PotentiometerComponent.VALUE, 50);
                    placed.stateUpdated();
                }
            }
        });
        scene.effects().indicateSuccess(util.grid().at(1, 1, 1));
        scene.electric().tickFor(30);
        scene.idle(30);

        scene.markAsFinished();
    }

    public static void neonBulb(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_neon", "Neon Bulb");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Neon Bulb is a simple device which will glow when you pass a current though it")
                .pointAt(util.vector().of(1.375, 1.375, 1.375))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.electric().addSource(util.grid().at(1, 1, 1), 0, 90);
        scene.electric().addSource(util.grid().at(1, 1, 1), 1, 0);
        scene.electric().tickFor(30);
        scene.idle(30);

        scene.effects().indicateSuccess(util.grid().at(1, 1, 1));
        scene.idle(20);

        scene.overlay().showText(80)
                .text("It needs a current limiting resistor to not break immediately after lighting up")
                .pointAt(util.vector().of(1.625, 1.375, 1.625))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void regulatorTube(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("circuit_regulator_tube", "Regulator Tube");
        scene.configureBasePlate(0, 0, 3);
        scene.scaleSceneView(3.0f);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(90)
                .text("The Regulator Tube is a special purpose device which, under correct conditions, can provide a very stable voltage reference")
                .pointAt(util.vector().of(1.375, 1.375, 1.375))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        var v1 = scene.electric().addSource(util.grid().at(1, 1, 1), 0, 90);
        scene.electric().addSource(util.grid().at(1, 1, 1), 1, 0);
        scene.electric().tickFor(30);
        scene.idle(30);

        scene.electric().setSource(v1, 80);
        scene.electric().tickFor(30);
        scene.idle(30);

        scene.electric().setSource(v1, 100);
        scene.electric().tickFor(30);
        scene.idle(30);

        scene.effects().indicateSuccess(util.grid().at(1, 1, 1));
        scene.idle(20);

        scene.markAsFinished();
    }
}
