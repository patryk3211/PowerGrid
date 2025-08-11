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

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class GeneratorScenes {
    public static void rotor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_rotor", "Spinning magnets");
        scene.configureBasePlate(1, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(0, 0, 2), Direction.UP);
        scene.world().showSection(util.select().position(0, 1, 3), Direction.UP);
        scene.idle(10);

        scene.world().showSection(util.select().position(1, 1, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(5);

        var target = util.grid().at(3, 1, 3);
        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(5);

        scene.overlay().showText(80)
                .text("The Rotor is the most basic part of a generator assembly. It provides a constant magnetic field.")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(target))
                .placeNearTarget();

        scene.idle(40);
        scene.world().toggleRedstonePower(util.select().fromTo(2, 1, 2, 2, 1, 3));
        scene.effects().indicateRedstone(util.grid().at(2, 1, 2));
        scene.world().modifyBlockEntity(util.grid().at(2, 1, 3), GeneratorClutchBlockEntity.class,
                be -> be.updateStrength(0));
        scene.idle(50);

        scene.world().showSection(util.select().position(target.east()), Direction.NORTH);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("You can extend the rotor assembly by placing more rotor blocks")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(target.east()))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void winding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_winding", "Coils of wire");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.idle(10);

        var stack = ModdedItems.COPPER_COIL.asStack();
        scene.overlay().showControls(util.vector().topOf(4, 1, 2), Pointing.LEFT, 50)
                .rightClick()
                .withItem(stack);
        scene.idle(20);
        scene.overlay().showControls(util.vector().topOf(0, 1, 2), Pointing.LEFT, 30)
                .rightClick()
                .withItem(stack);
        scene.idle(20);

        var state = ModdedBlocks.WINDING.getDefaultState()
                .with(WindingBlock.AXIS, Direction.Axis.X)
                .with(WindingBlock.ALONG_FIRST_AXIS, false)
                .with(WindingBlock.CASE_RIGHT, false)
                .with(WindingBlock.CASE_LEFT, false);
        scene.world().setBlock(util.grid().at(4, 1, 2), state.with(WindingBlock.PART, 2), true);
        scene.world().setBlock(util.grid().at(3, 1, 2), state.with(WindingBlock.PART, 1), true);
        scene.world().setBlock(util.grid().at(2, 1, 2), state.with(WindingBlock.PART, 1), true);
        scene.world().setBlock(util.grid().at(1, 1, 2), state.with(WindingBlock.PART, 1), true);
        scene.world().setBlock(util.grid().at(0, 1, 2), state.with(WindingBlock.PART, 0), true);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Right-Clicking two shafts with a coil item will create a winding")
                .pointAt(util.vector().topOf(2, 1, 2))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void parallelWinding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("parallel_generator_winding", "Connected coils of wire");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 1, 2), Direction.DOWN);
        scene.idle(20);

        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 1), Direction.SOUTH);
        scene.idle(10);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 1, 3, 1, 1), state -> state.with(WindingBlock.CASE_RIGHT, true), false);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 2, 3, 1, 2), state -> state.with(WindingBlock.CASE_LEFT, true), false);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(1, 1, 3, 3, 1, 3), Direction.SOUTH);
        scene.idle(10);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 3, 3, 1, 3), state -> state.with(WindingBlock.CASE_LEFT, true), false);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 2, 3, 1, 2), state -> state.with(WindingBlock.CASE_RIGHT, true), false);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Windings placed next to each other will connect. This reduces their resistance and allows higher current to flow...")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("...but their voltage is limited to the lowest voltage generated by a single winding.")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void generator(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("generator", "Turning rotation to electricity");
        scene.configureBasePlate(1, 0, 5);
        scene.electric().tickFor(10);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(0, 0, 2), Direction.UP);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(0, 1, 3, 2, 1, 3), Direction.EAST);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 1, 3, 4, 1, 3), Direction.NORTH);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(3, 2, 3, 4, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(4, 1, 1, 4, 2, 1), Direction.DOWN);
        scene.idle(5);

        var gauge = util.grid().at(4, 2, 1);
        var windingP = util.grid().at(3, 2, 3);
        var windingN = util.grid().at(4, 2, 3);
        scene.electric().connect(gauge, 1, windingP, 0);
        scene.electric().connect(gauge, 0, windingN, 1);
        scene.idle(5);

        scene.overlay().showText(80)
                .text("When windings are facing a spinning rotor they start generating scene.electric()ity")
                .attachKeyFrame()
                .pointAt(util.vector().of(4, 2, 3))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("The generated voltage is directly proportional to the speed of the rotor")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(40);
        scene.electric().tickFor(10);
        scene.world().multiplyKineticSpeed(util.select().everywhere(), 2.0f);
        scene.effects().rotationSpeedIndicator(util.grid().at(1, 1, 3));
        scene.idle(50);

        scene.overlay().showText(80)
                .text("When you draw current from the coils, the rotor will slow down and its stress impact will increase")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(40);

        scene.electric().connect(gauge, 0, gauge, 1, 0.01f);
        scene.electric().tickFor(10);
        scene.idle(50);

        scene.overlay().showText(80)
                .text("If you draw too much current your coils will start to overheat and explode!")
                .attachKeyFrame()
                .pointAt(util.vector().of(4, 3, 3.5))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void clutch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_clutch", "Dosing kinetic energy");
        scene.configureBasePlate(1, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(0, 0, 2), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(0, 1, 3, 2, 1, 3), Direction.DOWN);
        scene.idle(10);

        var target = util.grid().at(3, 1, 3);
        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(target.east()), Direction.WEST);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("The Generator Clutch is the base of all generator assemblies")
                .pointAt(util.vector().topOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showText(80)
                .text("It provides a weak coupling between your kinetic network and the generator shaft")
                .pointAt(util.vector().topOf(target.east()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(target.north()), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("You can use a redstone signal to change the coupling strength")
                .pointAt(util.vector().centerOf(target.north()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 2, 3, 1, 3));
        scene.world().modifyBlockEntity(target, GeneratorClutchBlockEntity.class, be -> be.updateStrength(15));
        scene.effects().indicateRedstone(target.north());
        scene.idle(50);

        scene.markAsFinished();
    }

    public static void housing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_housing", "Connecting windings at an angle");
        scene.configureBasePlate(0, 0, 5);

        var target = util.grid().at(1, 2, 2);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(1, 1, 1, 1, 1, 3), Direction.EAST);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(2, 2, 1, 2, 2, 3), Direction.DOWN);
        scene.idle(20);

        scene.world().setBlock(target, ModdedBlocks.GENERATOR_HOUSING.getDefaultState()
                .with(GeneratorHousing.HORIZONTAL_FACING, Direction.EAST)
                .with(GeneratorHousing.UP, false), true);
        scene.world().modifyBlock(util.grid().at(1, 1, 2), state -> state.with(WindingBlock.CASE_RIGHT, true), false);
        scene.world().modifyBlock(util.grid().at(2, 2, 2), state -> state.with(WindingBlock.CASE_LEFT, true), false);

        scene.overlay().showText(80)
                .text("The generator housing can be used to connect windings around a rotor")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(target, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void inductive(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("generator_inductive", "Variable magnetic fields");
        scene.configureBasePlate(0, 0, 7);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(7, 0, 2), Direction.UP);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(2, 1, 3, 7, 1, 3), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(3, 2, 3, 4, 2, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 1, 1, 4, 2, 1), Direction.DOWN);
        scene.idle(10);

        var rotor = util.grid().at(3, 1, 3);

        var source = util.grid().at(0, 1, 3);
        var commutator = util.grid().at(2, 1, 3);
        var meter1 = util.grid().at(1, 1, 2);
        var meter2 = util.grid().at(4, 2, 1);
        var windingP = util.grid().at(3, 2, 3);
        var windingN = util.grid().at(4, 2, 3);

        electric.connectInvisible(source, 0, commutator, 0);
        electric.connectInvisible(source, 1, commutator, 1);

        electric.connect(commutator, 0, meter1, 0);
        electric.connect(commutator, 1, meter1, 1);
        electric.connect(windingP, 0, meter2, 1);
        electric.connect(windingN, 1, meter2, 0);

        electric.setSource(source, 5);
        electric.tickFor(10);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Inductive Rotors let you control the strength of their magnetic field")
                .pointAt(util.vector().blockSurface(rotor, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("To power them you need to add a Commutator to your rotor assembly")
                .pointAt(util.vector().topOf(commutator))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("By changing the voltage, you essentially change the amount of kinetic energy that gets converted into electricity")
                .pointAt(util.vector().blockSurface(meter1, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        electric.setSource(source, 15);
        electric.tickFor(10);
        scene.idle(50);

        scene.markAsFinished();
        electric.unload();
    }
}
