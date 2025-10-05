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
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

;

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
}
