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

import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.instruction.EmitParticlesInstruction;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;

public class WireScenes {
    public static void simple(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("wire_simple", "Transferring electricity using wires");
        scene.configureBasePlate(0, 0, 7);

        var sourcePos = util.grid.at(0, 1, 0);
        var lightPos = util.grid.at(6, 1, 3);

        electric.connectInvisible(util.grid.at(0, 1, 2), 0, sourcePos, 0);
        electric.connectInvisible(util.grid.at(0, 1, 4), 0, sourcePos, 1);
        electric.setSource(sourcePos, 62);

        scene.showBasePlate();
        scene.idle(5);

        scene.world.showSection(util.select.fromTo(0, 1, 2, 5, 1, 4), Direction.DOWN);
        scene.idle(5);

        scene.world.showSection(util.select.position(lightPos), Direction.DOWN);
        scene.idle(5);

        electric.connect(util.grid.at(0, 1, 4), 0, util.grid.at(5, 1, 4), 0);
        scene.idle(5);
        electric.connect(util.grid.at(5, 1, 4), 0, lightPos, 1);
        scene.idle(5);
        electric.connect(util.grid.at(5, 1, 2), 0, lightPos, 0);
        scene.idle(5);
        electric.connect(util.grid.at(0, 1, 2), 0, util.grid.at(5, 1, 2), 0);
        scene.idle(10);

        scene.world.modifyBlock(lightPos, state -> state.with(LightFixtureBlock.POWER, 2), false);

        scene.overlay.showText(80)
                .text("Wires are used to transfer electricity between terminals of electric devices")
                .attachKeyFrame()
                .pointAt(util.vector.of(3, 1.3, 2.5))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
        electric.unload();
    }

    public static void voltageDrop(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("wire_voltage_drop", "Transfer losses");
        scene.configureBasePlate(0, 0, 7);

        var sourcePos = util.grid.at(0, 1, 0);
        electric.connectInvisible(util.grid.at(0, 1, 2), 0, sourcePos, 0);
        electric.connectInvisible(util.grid.at(0, 1, 4), 0, sourcePos, 1);
        electric.setSource(sourcePos, 20);

        scene.showBasePlate();
        scene.idle(5);

        scene.world.showSection(util.select.fromTo(0, 1, 2, 6, 1, 4), Direction.DOWN);
        scene.idle(5);

        electric.connect(util.grid.at(0, 1, 2), 0, util.grid.at(5, 1, 2), 0, 5.0f);
        electric.connect(util.grid.at(0, 1, 4), 0, util.grid.at(5, 1, 4), 0, 5.0f);

        electric.connect(util.grid.at(5, 1, 2), 0, util.grid.at(6, 1, 3), 1);
        electric.connect(util.grid.at(5, 1, 4), 0, util.grid.at(6, 1, 3), 0);

        electric.connect(util.grid.at(5, 1, 2), 0, util.grid.at(5, 1, 3), 0);
        electric.connect(util.grid.at(5, 1, 4), 0, util.grid.at(5, 1, 3), 1);

        electric.connect(util.grid.at(0, 1, 2), 0, util.grid.at(1, 1, 3), 0);
        electric.connect(util.grid.at(0, 1, 4), 0, util.grid.at(1, 1, 3), 1);
        scene.idle(10);

        scene.overlay.showText(80)
                .text("Every wire has a resistance, the longer the wire, the more resistance it has")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);
        electric.tickFor(10);

        scene.overlay.showText(80)
                .text("This causes a voltage drop to occur when current is flowing")
                .pointAt(util.vector.blockSurface(util.grid.at(5, 1, 3), Direction.WEST))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay.showText(60)
                .text("Lost power is turned into heat")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);

        scene.overlay.showText(80)
                .text("If too much power is drawn, the wires can overheat and break")
                .placeNearTarget();
        scene.idle(20);

        for(int i = 0; i < 10; ++i) {
            scene.effects.emitParticles(util.vector.of(0.75f + i * 0.5f, 1.3, 2.5), EmitParticlesInstruction.Emitter.simple(ParticleTypes.SMOKE, Vec3d.ZERO), 0.2f, 60);
            scene.effects.emitParticles(util.vector.of(0.75f + i * 0.5f, 1.3, 4.5), EmitParticlesInstruction.Emitter.simple(ParticleTypes.SMOKE, Vec3d.ZERO), 0.2f, 60);
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
                util.grid.at(1, 1, 1),
                util.grid.at(3, 1, 2),
                util.grid.at(1, 1, 4),
                util.grid.at(5, 1, 2),
                util.grid.at(4, 1, 4),
                util.grid.at(3, 1, 5),
                util.grid.at(6, 1, 0),
                util.grid.at(0, 1, 5)
        };

        for(var pos : positions) {
            scene.world.showSection(util.select.position(pos), Direction.DOWN);
            scene.idle(5);
        }

        electric.connect(positions[0], 0, positions[1], 0);
        scene.idle(5);
        electric.connect(positions[0], 0, positions[2], 0);
        scene.idle(5);
        electric.connect(positions[1], 0, positions[6], 0);
        scene.idle(5);

        scene.overlay.showText(80)
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
}
