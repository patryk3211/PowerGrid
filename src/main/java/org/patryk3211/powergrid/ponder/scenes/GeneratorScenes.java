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
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModIcons;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlock;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlock;
import org.patryk3211.powergrid.kinetics.generator.rotor.ShaftDirection;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;

public class GeneratorScenes {
    public static void rotor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_rotor", "Spinning magnets");
        scene.configureBasePlate(2, 0, 5);

        scene.showBasePlate();
        scene.world.showSection(util.select.position(1, 0, 2), Direction.UP);
        scene.world.showSection(util.select.position(1, 1, 3), Direction.UP);
        scene.idle(5);

        scene.world.showSection(util.select.position(2, 1, 3), Direction.DOWN);
        scene.idle(2);
        scene.world.showSection(util.select.position(2, 1, 2), Direction.DOWN);
        scene.idle(2);
        scene.world.showSection(util.select.position(2, 1, 1), Direction.DOWN);
        scene.idle(2);
        scene.world.showSection(util.select.position(3, 1, 3), Direction.DOWN);
        scene.idle(2);

        var target = util.grid.at(4, 1, 3);
        scene.world.setBlock(target, ModdedBlocks.GENERATOR_ROTOR.getDefaultState()
                .with(RotorBlock.AXIS, Direction.Axis.X).with(RotorBlock.SHAFT_DIRECTION, ShaftDirection.NEGATIVE), false);
        scene.world.showSection(util.select.position(target), Direction.DOWN);
        scene.idle(5);

        var secondTarget = util.grid.at(5, 1, 3);
        scene.world.setBlock(secondTarget, ModdedBlocks.GENERATOR_ROTOR.getDefaultState()
                .with(RotorBlock.AXIS, Direction.Axis.X).with(RotorBlock.SHAFT_DIRECTION, ShaftDirection.NONE), false);
        scene.world.showSection(util.select.position(secondTarget), Direction.DOWN);
        scene.idle(5);

        scene.overlay.showText(80)
                .text("The generator rotor is a weakly coupled kinetic device that will try to spin up to the speed provided at its input")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(target))
                .placeNearTarget();

        scene.idle(40);
        scene.world.toggleRedstonePower(util.select.fromTo(2, 1, 1, 2, 1, 3));
        scene.effects.indicateRedstone(util.grid.at(2, 1, 1));
        scene.world.setKineticSpeed(util.select.fromTo(2, 1, 3, 4, 1, 3), -64);
        scene.idle(50);

        scene.overlay.showText(80)
                .text("You can extend the rotor by placing more rotor blocks at the back")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(secondTarget))
                .placeNearTarget();
        scene.idle(90);

        scene.world.hideSection(util.select.fromTo(1, 1, 1, 3, 1, 3), Direction.WEST);
        scene.world.hideSection(util.select.position(1, 0, 2), Direction.WEST);
        scene.world.setKineticSpeed(util.select.position(target), 0);
        scene.idle(15);

        scene.overlay.showText(80)
                .text("There can only ever be one input shaft")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(secondTarget))
                .placeNearTarget();
        scene.overlay.showControls(new InputWindowElement(util.vector.blockSurface(secondTarget, Direction.EAST), Pointing.RIGHT).withWrench(), 30);
        scene.idle(20);

        scene.world.setBlock(target, ModdedBlocks.GENERATOR_ROTOR.getDefaultState()
                .with(RotorBlock.AXIS, Direction.Axis.X).with(RotorBlock.SHAFT_DIRECTION, ShaftDirection.NONE), false);
        scene.world.setBlock(secondTarget, ModdedBlocks.GENERATOR_ROTOR.getDefaultState()
                .with(RotorBlock.AXIS, Direction.Axis.X).with(RotorBlock.SHAFT_DIRECTION, ShaftDirection.POSITIVE), false);
        scene.idle(70);

        scene.markAsFinished();
    }

    public static void coil(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_coil", "Coils of wire");
        scene.configureBasePlate(0, 0, 5);

        scene.world.showSection(util.select.layer(0), Direction.UP);
        scene.idle(10);

        var coil = util.grid.at(2, 1, 2);
        scene.world.showSection(util.select.position(coil), Direction.DOWN);
        scene.idle(10);

        scene.overlay.showText(60)
                .text("A generator coil alone can't do much")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(coil))
                .placeNearTarget();
        scene.idle(70);

        scene.overlay.showText(80)
                .text("By using the wrench you can add terminals to the coil")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(coil))
                .placeNearTarget();
        scene.idle(50);

        scene.overlay.showControls(new InputWindowElement(util.vector.topOf(coil), Pointing.RIGHT).withWrench(), 30);
        scene.idle(20);
        scene.world.setBlock(coil, ModdedBlocks.GENERATOR_COIL.getDefaultState()
                .with(CoilBlock.FACING, Direction.DOWN).with(CoilBlock.HAS_TERMINALS, true), false);
        scene.idle(40);

        var state = ModdedBlocks.GENERATOR_COIL.getDefaultState()
                .with(CoilBlock.FACING, Direction.DOWN).with(CoilBlock.HAS_TERMINALS, false);
        var posList = new BlockPos[] {
                coil.west(),
                coil.north(),
                coil.east(),
                coil.west().south()
        };
        for(var pos : posList) {
            scene.world.setBlock(pos, state, false);
            scene.world.showSection(util.select.position(pos), Direction.DOWN);
            scene.idle(5);
        }

        var vec = util.vector.topOf(coil.west());
        scene.idle(10);
        scene.overlay.showText(80)
                .text("Coils placed next to each other connect to form one large coil")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay.showText(60)
                .text("You can select between different aggregation types")
                .placeNearTarget();
        scene.idle(70);

        scene.overlay.showText(80)
                .text("Coils connected in series sum their voltage but also their resistance")
                .attachKeyFrame()
                .pointAt(vec)
                .placeNearTarget();
        scene.idle(10);
        scene.overlay.showControls(new InputWindowElement(vec, Pointing.RIGHT).showing(ModIcons.I_SERIES), 50);
        scene.idle(80);

        scene.overlay.showText(80)
                .text("Voltage of coils connected in parallel is limited by the smallest voltage out of all of the coils...")
                .attachKeyFrame()
                .pointAt(vec)
                .placeNearTarget();
        scene.idle(10);
        scene.overlay.showControls(new InputWindowElement(vec, Pointing.RIGHT).showing(ModIcons.I_PARALLEL), 50);
        scene.idle(80);

        scene.overlay.showText(80)
                .text("...but their resistance gets smaller when you add more coils")
                .pointAt(util.vector.topOf(coil.north()))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay.showText(80)
                .text("You can have only one coil be the output of the whole aggregate")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(coil))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void generator(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("generator", "Turning rotation to electricity");
        scene.configureBasePlate(1, 0, 5);
        electric.tickFor(10);

        scene.showBasePlate();
        scene.world.showSection(util.select.position(0, 0, 2), Direction.UP);
        scene.idle(10);

        scene.world.showSection(util.select.fromTo(0, 1, 3, 2, 1, 3), Direction.EAST);
        scene.idle(5);
        scene.world.showSection(util.select.fromTo(3, 1, 3, 4, 1, 3), Direction.NORTH);
        scene.idle(5);
        scene.world.showSection(util.select.fromTo(3, 2, 3, 4, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world.showSection(util.select.fromTo(4, 1, 1, 4, 2, 1), Direction.DOWN);
        scene.idle(5);

        var gauge = util.grid.at(4, 2, 1);
        var coil = util.grid.at(4, 2, 3);
        electric.connect(gauge, 0, coil, 0);
        electric.connect(gauge, 1, coil, 1);
        scene.idle(5);

        scene.overlay.showText(80)
                .text("When coils are facing a spinning rotor they start generating electricity")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(coil))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay.showText(80)
                .text("The generated voltage is directly proportional to the speed of the rotor")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(40);
        electric.tickFor(10);
        scene.world.multiplyKineticSpeed(util.select.everywhere(), 2.0f);
        scene.effects.rotationSpeedIndicator(util.grid.at(2, 1, 3));
        scene.idle(50);

        scene.overlay.showText(80)
                .text("When you draw current from the coils, the rotor will slow down and its stress impact will increase")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(40);

        electric.connect(coil, 0, coil, 1, 0.01f);
        electric.tickFor(10);
        scene.idle(50);

        scene.overlay.showText(80)
                .text("If you draw too much current your coils will start to overheat and explode!")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(coil.west()))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
        electric.unload();
    }

    public static void housing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_housing", "Connecting coils at an angle");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);

        scene.world.showSection(util.select.position(2, 1, 2), Direction.SOUTH);
        scene.world.showSection(util.select.position(2, 3, 2), Direction.SOUTH);
        scene.world.showSection(util.select.position(1, 2, 2), Direction.SOUTH);
        scene.world.showSection(util.select.position(3, 2, 2), Direction.SOUTH);
        scene.idle(5);

        scene.world.showSection(util.select.fromTo(2, 2, 1, 2, 2, 3), Direction.NORTH);
        scene.idle(5);

        scene.world.showSection(util.select.position(3, 1, 2), Direction.WEST);
        scene.world.showSection(util.select.position(3, 3, 2), Direction.WEST);
        scene.world.showSection(util.select.position(1, 1, 2), Direction.EAST);
        scene.world.showSection(util.select.position(1, 3, 2), Direction.EAST);
        scene.idle(10);

        scene.overlay.showText(80)
                .text("The generator housing can be used to aggregate all coils around the rotor")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(1, 3, 2))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }
}
