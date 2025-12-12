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
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlockEntity;
import org.patryk3211.powergrid.kinetics.rheostat.RheostatBlockEntity;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class GeneratorScenes {
    public static void rotor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_rotor", "Spinning weight");
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
                .text("Rotors are the most basic part of a generator assembly, they generate electricity when spinning in a magnetic field.")
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
                .setValue(WindingBlock.AXIS, Direction.Axis.X)
                .setValue(WindingBlock.ALONG_FIRST_AXIS, false)
                .setValue(WindingBlock.CASE_RIGHT, false)
                .setValue(WindingBlock.CASE_LEFT, false);
        scene.world().setBlock(util.grid().at(4, 1, 2), state.setValue(WindingBlock.PART, 2), true);
        scene.world().setBlock(util.grid().at(3, 1, 2), state.setValue(WindingBlock.PART, 1), true);
        scene.world().setBlock(util.grid().at(2, 1, 2), state.setValue(WindingBlock.PART, 1), true);
        scene.world().setBlock(util.grid().at(1, 1, 2), state.setValue(WindingBlock.PART, 1), true);
        scene.world().setBlock(util.grid().at(0, 1, 2), state.setValue(WindingBlock.PART, 0), true);
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
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 1, 3, 1, 1), state -> state.setValue(WindingBlock.CASE_RIGHT, true), false);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 2, 3, 1, 2), state -> state.setValue(WindingBlock.CASE_LEFT, true), false);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(1, 1, 3, 3, 1, 3), Direction.SOUTH);
        scene.idle(10);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 3, 3, 1, 3), state -> state.setValue(WindingBlock.CASE_LEFT, true), false);
        scene.world().modifyBlocks(util.select().fromTo(1, 1, 2, 3, 1, 2), state -> state.setValue(WindingBlock.CASE_RIGHT, true), false);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Windings placed next to each other will connect in series")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("This allows for a cleaner generator setup with less wires connected around rotors")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
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
                .setValue(GeneratorHousing.HORIZONTAL_FACING, Direction.EAST)
                .setValue(GeneratorHousing.UP, false), true);
        scene.world().modifyBlock(util.grid().at(1, 1, 2), state -> state.setValue(WindingBlock.CASE_RIGHT, true), false);
        scene.world().modifyBlock(util.grid().at(2, 2, 2), state -> state.setValue(WindingBlock.CASE_LEFT, true), false);

        scene.overlay().showText(80)
                .text("The generator housing can be used to connect windings around a rotor")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(target, Direction.NORTH))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("This allows for a cleaner generator setup with less wires connected around rotors")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void shuntGenerator(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("generator_shunt", "Constant voltage generator");
        scene.configureBasePlate(1, 0, 7);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(0, 0, 4), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().layer(2), Direction.DOWN);
        scene.world().showSection(util.select().layer(3), Direction.DOWN);
        scene.idle(10);

        var meter = util.grid().at(2, 1, 2);
        var commutator = util.grid().at(3, 1, 3);
        var winding = util.grid().at(6, 3, 3);
        var connector = util.grid().at(4, 3, 3);
        var rheo = util.grid().at(7, 1, 1);
        var resistor = util.grid().at(5, 1, 1);

        scene.electric().connect(meter, 0, commutator, 1);
        scene.electric().connect(meter, 1, connector, 0);
        scene.electric().connect(connector, 0, commutator, 0);
        scene.electric().connect(winding, 0, connector, 0);
        scene.electric().connect(winding, 1, rheo, 2);
        scene.electric().connect(rheo, 1, resistor, 0);
        scene.electric().connect(resistor, 1, commutator, 1);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The first and most common generator configuration is the shunt generator.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("It is a self-excited configuration which generates a constant voltage.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(100)
                .text("When everything is wired correctly, the generator should start from the residual magnetic field of the windings")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);
        scene.world().modifyBlockEntityNBT(util.select().fromTo(4, 1, 3, 6, 2, 4), WindingBlockEntity.class,
                tag -> tag.putFloat("Current", 2.0f));
        scene.electric().tickFor(60);
        scene.idle(60);
        scene.effects().indicateSuccess(meter);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Its voltage can be tuned by changing the resistance in series with the field windings")
                .pointAt(util.vector().blockSurface(rheo, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        scene.electric().tickFor(80);
        scene.world().setKineticSpeed(util.select().fromTo(rheo, rheo.above()), -16.0f);
        scene.world().modifyBlockEntity(rheo, RheostatBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.idle(20);
        scene.world().setKineticSpeed(util.select().fromTo(rheo, rheo.above()), 0.0f);
        scene.world().modifyBlockEntity(rheo, RheostatBlockEntity.class, be -> be.onSpeedChanged(-16f));
        scene.idle(40);

        scene.effects().indicateSuccess(meter);
        scene.effects().indicateSuccess(rheo);
        scene.idle(20);

        scene.markAsFinished();
    }
}
