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

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.wire.BlockWireEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.SocketEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.SplitCordEndpoint;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class WireScenes {
    public static void simple(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("wire_simple", "Transferring electricity using wires");
        scene.configureBasePlate(0, 0, 7);

        var sourcePos = util.grid().at(0, 1, 0);
        var lightPos = util.grid().at(6, 1, 3);

        electric.connectInvisible(util.grid().at(0, 1, 2), 0, sourcePos, 0);
        electric.connectInvisible(util.grid().at(0, 1, 4), 0, sourcePos, 1);
        electric.setSource(sourcePos, 62);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().fromTo(0, 1, 2, 5, 1, 4), Direction.DOWN);
        scene.idle(5);

        scene.world().showSection(util.select().position(lightPos), Direction.DOWN);
        scene.idle(5);

        electric.connect(util.grid().at(0, 1, 4), 0, util.grid().at(5, 1, 4), 0);
        scene.idle(5);
        electric.connect(util.grid().at(5, 1, 4), 0, lightPos, 1);
        scene.idle(5);
        electric.connect(util.grid().at(5, 1, 2), 0, lightPos, 0);
        scene.idle(5);
        electric.connect(util.grid().at(0, 1, 2), 0, util.grid().at(5, 1, 2), 0);
        scene.idle(10);

        scene.world().modifyBlock(lightPos, state -> state.setValue(LightFixtureBlock.POWER, 2), false);

        scene.overlay().showText(80)
                .text("Wires are used to transfer electricity between terminals of electric devices")
                .attachKeyFrame()
                .pointAt(util.vector().of(3, 1.3, 2.5))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
        electric.unload();
    }

    public static void voltageDrop(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("wire_voltage_drop", "Transfer losses");
        scene.configureBasePlate(0, 0, 7);

        var sourcePos = util.grid().at(0, 1, 0);
        electric.connectInvisible(util.grid().at(0, 1, 2), 0, sourcePos, 0);
        electric.connectInvisible(util.grid().at(0, 1, 4), 0, sourcePos, 1);
        electric.setSource(sourcePos, 20);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().fromTo(0, 1, 2, 6, 1, 4), Direction.DOWN);
        scene.idle(5);

        electric.connect(util.grid().at(0, 1, 2), 0, util.grid().at(5, 1, 2), 0, 5.0f);
        electric.connect(util.grid().at(0, 1, 4), 0, util.grid().at(5, 1, 4), 0, 5.0f);

        electric.connect(util.grid().at(5, 1, 2), 0, util.grid().at(6, 1, 3), 1);
        electric.connect(util.grid().at(5, 1, 4), 0, util.grid().at(6, 1, 3), 0);

        electric.connect(util.grid().at(5, 1, 2), 0, util.grid().at(5, 1, 3), 0);
        electric.connect(util.grid().at(5, 1, 4), 0, util.grid().at(5, 1, 3), 1);

        electric.connect(util.grid().at(0, 1, 2), 0, util.grid().at(1, 1, 3), 0);
        electric.connect(util.grid().at(0, 1, 4), 0, util.grid().at(1, 1, 3), 1);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Every wire has a resistance, the longer the wire, the more resistance it has")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);
        electric.tickFor(10);

        scene.overlay().showText(80)
                .text("This causes a voltage drop to occur when current is flowing")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 1, 3), Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(60)
                .text("Lost power is turned into heat")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);

        scene.overlay().showText(80)
                .text("If too much power is drawn, the wires can overheat and break")
                .placeNearTarget();
        scene.idle(20);

        for(int i = 0; i < 10; ++i) {
            scene.effects().emitParticles(util.vector().of(0.75f + i * 0.5f, 1.3, 2.5), scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE, Vec3.ZERO), 0.2f, 60);
            scene.effects().emitParticles(util.vector().of(0.75f + i * 0.5f, 1.3, 4.5), scene.effects().simpleParticleEmitter(ParticleTypes.SMOKE, Vec3.ZERO), 0.2f, 60);
        }
        scene.idle(70);

        scene.markAsFinished();
        electric.unload();
    }

    public static void connector(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("wire_connector", "Wires on poles");
        scene.configureBasePlate(0, 0, 7);

        scene.showBasePlate();
        scene.idle(5);

        var positions = new BlockPos[] {
                util.grid().at(1, 1, 1),
                util.grid().at(3, 1, 2),
                util.grid().at(1, 1, 4),
                util.grid().at(5, 1, 2),
                util.grid().at(4, 1, 4),
                util.grid().at(3, 1, 5),
                util.grid().at(6, 1, 0),
                util.grid().at(0, 1, 5)
        };

        for(var pos : positions) {
            scene.world().showSection(util.select().position(pos), Direction.DOWN);
            scene.idle(5);
        }

        electric.connect(positions[0], 0, positions[1], 0);
        scene.idle(5);
        electric.connect(positions[0], 0, positions[2], 0);
        scene.idle(5);
        electric.connect(positions[1], 0, positions[6], 0);
        scene.idle(5);

        scene.overlay().showText(80)
                .text("Wire connectors can be used as attachment points for wires")
                .attachKeyFrame()
                .placeNearTarget();

        electric.connect(positions[1], 0, positions[3], 0);
        scene.idle(5);
        electric.connect(positions[3], 0, positions[4], 0);
        scene.idle(5);
        electric.connect(positions[4], 0, positions[5], 0);
        scene.idle(5);
        electric.connect(positions[1], 0, positions[4], 0);
        scene.idle(5);
        electric.connect(positions[5], 0, positions[7], 0);
        scene.idle(70);

        scene.markAsFinished();
        electric.unload();
    }

    public static void grounding(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("grounding", "Ground reference");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("The grounding rod provides a 0V ground reference to your grid")
                .pointAt(util.vector().of(2.5, 1.125, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showText(60)
                .text("You can use it to transfer power but you must be careful...")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showText(90)
                .text("High potential and bad ground conditions will cause the ground to become electrified and cause damage to nearby entities.")
                .colored(PonderPalette.RED)
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }

    public static void improvedGround(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("improved_grounding", "Better ground reference");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);
        var grass = scene.world().showIndependentSection(util.select().fromTo(1, 2, 1, 3, 3, 3), Direction.DOWN);
        scene.world().moveSection(grass, util.vector().of(0, -1, 0), 0);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Regular blocks are not that conductive and will cause your grounding rod to have a high impedance")
                .pointAt(util.vector().topOf(1, 1, 1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().hideIndependentSection(grass, Direction.UP);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 3), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("You can improve the ground conditions by adding conductive blocks close to the grounding rod")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(1, 2, 1, 3, 3, 3), Direction.DOWN);
        scene.idle(20);

        scene.markAsFinished();
    }

    public static void insulatedWire(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("insulated_wire", "Insulated Wires");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(5);

        var t1 = util.grid().at(0, 1, 2);
        var t2 = util.grid().at(2, 1, 1);
        var t3 = util.grid().at(2, 1, 3);
        var t4 = util.grid().at(4, 1, 2);
        scene.electric().connect(t1, 0, t2, 0, DyeColor.RED);
        scene.electric().connect(t2, 0, t4, 0, DyeColor.RED);
        scene.electric().connect(t1, 0, t4, 0, DyeColor.GREEN);
        scene.electric().connect(t1, 0, t3, 0, DyeColor.BLUE);
        scene.electric().connect(t3, 0, t4, 0, DyeColor.BLUE);
        scene.idle(10);

        scene.overlay().showText(100)
                .text("Insulated wires can be colored by right clicking them with a dye or by holding a dye in your offhand while placing them.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.markAsFinished();
    }

    public static void cordJunction(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("cord_junction", "Cord Junctions");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 1), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 1, 3, 1, 1, 4), Direction.DOWN);
        scene.idle(5);

        scene.electric().connectCord(
                new AutoCordEndpoint(util.grid().at(2, 1, 1), 0, 1, util.vector().of(2.5, 1.125, 1.5), null),
                new AutoCordEndpoint(util.grid().at(3, 1, 2), 0, 1, util.vector().of(3.875, 1.5, 2.5), null)
        );
        scene.idle(5);
        scene.electric().connectCord(
                new AutoCordEndpoint(util.grid().at(3, 1, 2), 0, 1, util.vector().of(3.875, 1.5, 2.5), null),
                new AutoCordEndpoint(util.grid().at(1, 1, 3), 0, 1, util.vector().of(1.5, 1.5, 3.875), null)
        );
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Cord junctions allow you to extend and split cords easily")
                .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.EAST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void cordSocket(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("cord_socket", "Cord Sockets");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.idle(5);
        scene.electric().connectCord(
                new SocketEndpoint(util.grid().at(1, 1, 2)),
                new SocketEndpoint(util.grid().at(3, 1, 2))
        );
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Cord sockets allow you to combine simple wires into cords with style")
                .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.EAST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void cord(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("cord", "Cords");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().fromTo(3, 1, 1, 4, 1, 1), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(1, 1, 1), Direction.DOWN);
        scene.idle(5);
        scene.electric().connectCord(
                new SocketEndpoint(util.grid().at(3, 1, 1)),
                new AutoCordEndpoint(util.grid().at(1, 1, 1), 0, 1, util.vector().of(1.5, 1.125, 1.5), null)
        );
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Cords are 2 strand wires which can make connecting things easier")
                .pointAt(util.vector().of(2.5, 1.25, 1.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().fromTo(0, 1, 4, 1, 1, 4), Direction.DOWN);
        scene.idle(10);
        scene.electric().connectCord(
                new AutoCordEndpoint(util.grid().at(1, 1, 1), 0, 1, util.vector().of(1.5, 1.125, 1.5), null),
                new SplitCordEndpoint(new BlockWireEndpoint(util.grid().at(1, 1, 4), 0), new BlockWireEndpoint(util.grid().at(0, 1, 4), 0))
        );
        scene.idle(20);
        scene.overlay().showText(80)
                .text("Cords can be attached to pairs of regular terminals at their ends")
                .pointAt(util.vector().of(1, 1.5, 4.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().showSection(util.select().position(4, 1, 3), Direction.DOWN);
        scene.idle(10);
        scene.electric().connectCord(
                new AutoCordEndpoint(util.grid().at(1, 1, 1), 0, 1, util.vector().of(1.5, 1.125, 1.5), null),
                new AutoCordEndpoint(util.grid().at(4, 1, 3), 0, 1, util.vector().of(4 - (3 / 16f), 1.25, 3.5), Direction.WEST)
        );
        scene.idle(20);
        scene.overlay().showText(60)
                .text("As well as most of consumer devices")
                .pointAt(util.vector().topOf(4, 1, 3))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.markAsFinished();
    }

    public static void stringLights(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("string_lights", "String Lights");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 2, 2), Direction.DOWN);
        scene.idle(10);
        var cord = scene.electric().connectLightCord(
                new AutoCordEndpoint(util.grid().at(0, 2, 2), 0, 1, util.vector().of(0.75, 2.125, 2.5), null),
                new AutoCordEndpoint(util.grid().at(4, 2, 2), 0, 1, util.vector().of(4.25, 2.125, 2.5), null),
                null
        );
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The String Light Cord is a decorative wire that will light up when powered")
                .pointAt(util.vector().of(2.5, 2, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.electric().addSource(util.grid().at(0, 2, 2), 0, 0);
        scene.electric().addSource(util.grid().at(0, 2, 2), 1, 120);
        scene.electric().tickForever();
        scene.idle(50);

        scene.electric().removeWire(cord);
        scene.idle(20);
        cord = scene.electric().connectLightCord(
                new AutoCordEndpoint(util.grid().at(0, 2, 2), 0, 1, util.vector().of(0.75, 2.125, 2.5), null),
                new AutoCordEndpoint(util.grid().at(4, 2, 2), 0, 1, util.vector().of(4.25, 2.125, 2.5), null),
                new DyeColor[] { DyeColor.RED, DyeColor.GREEN, DyeColor.BLUE }
        );
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Its pattern can be changed by combining it with any amount of dyes in the crafting table.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }
}
