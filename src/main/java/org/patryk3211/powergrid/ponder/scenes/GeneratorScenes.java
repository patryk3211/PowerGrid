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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileCoilBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileCoilBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.clutch.GeneratorClutchBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;
import org.patryk3211.powergrid.kinetics.rheostat.RheostatBlock;
import org.patryk3211.powergrid.kinetics.rheostat.RheostatBlockEntity;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class GeneratorScenes {
    public static void rotor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("generator_rotor", "Spinning weight");
        scene.configureBasePlate(1, 0, 5);
        scene.setNextUpEnabled(true);

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
        scene.setNextUpEnabled(true);

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
        scene.setNextUpEnabled(true);

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
        scene.setNextUpEnabled(true);

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

    public static void generator1(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("generator_1", "Generator construction");
        scene.configureBasePlate(0, 0, 7);
        scene.setNextUpEnabled(true);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(7, 0, 4, 7, 2, 4), Direction.UP);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("A Generator consists of two main parts")
                .placeNearTarget();
        scene.idle(70);

        scene.world().showSection(util.select().fromTo(2, 1, 3, 6, 1, 4), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3, 2, 3, 4, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("The Stator, built out of Coils strung on Shafts (and optionally Housings)")
                .pointAt(util.vector().of(4, 2, 4))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80)
                .text("This part generates a magnetic field, through a process called Excitation")
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(2, 2, 4, 6, 2, 4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("As well as the Armature, built out of Rotors, a Commutator, and a Clutch")
                .pointAt(util.vector().of(4, 3, 4.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("This is where electricity is generated. In typical setups, electrical load should be connected to the commutator.")
                .pointAt(util.vector().of(2.5, 3, 4.5))
                .placeNearTarget();
        scene.idle(90);

        var meter1 = util.grid().at(1, 2, 3);
        var commutator = util.grid().at(2, 2, 4);
        scene.world().showSection(util.select().fromTo(1, 1, 3, 1, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.electric().connect(meter1, 0, commutator, 1);
        scene.electric().connect(meter1, 1, commutator, 0);
        scene.idle(10);
        scene.effects().indicateSuccess(commutator);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("To start generating electricity you must provide an Excitation Current to the Stator, through a Device Connector")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        var meter2 = util.grid().at(4, 1, 1);
        var conn = util.grid().at(4, 2, 2);
        scene.world().showSection(util.select().position(conn), Direction.SOUTH);
        scene.idle(10);
        scene.world().showSection(util.select().position(meter2), Direction.DOWN);
        scene.idle(10);

        scene.electric().connect(meter2, 0, conn, 0);
        scene.electric().connect(meter2, 1, conn, 1);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("Now lets crank up the voltage!")
                .placeNearTarget();
        scene.idle(70);
        scene.electric().tickForever();
        scene.electric().addSource(meter2, 0, 0);
        scene.electric().addSource(meter2, 1, 50);

        scene.idle(30);
        scene.effects().indicateSuccess(meter1);
        scene.effects().indicateSuccess(meter2);
        scene.idle(30);

        scene.markAsFinished();
    }

    public static void generator2(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("generator_2", "Self-starting generator");
        scene.configureBasePlate(0, 0, 7);
        scene.setNextUpEnabled(true);

        scene.showBasePlate();
        scene.idle(10);

        var commutator = util.grid().at(3, 2, 3);
        var meter = util.grid().at(3, 2, 2);
        var conn = util.grid().at(1, 2, 4);
        var rheo = util.grid().at(6, 2, 1);
        var wireconn1 = util.grid().at(5, 3, 3);
        var wireconn2 = util.grid().at(1, 2, 1);

        scene.world().showSection(util.select().fromTo(1, 1, 2, 4, 3, 6), Direction.DOWN);
        scene.idle(10);
        scene.electric().connect(commutator, 1, meter, 0, DyeColor.GRAY);
        scene.electric().connect(commutator, 0, meter, 1, DyeColor.GRAY);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("At this point you might have thought of a small problem.")
                .placeNearTarget();
        scene.idle(70);
        scene.overlay().showText(80)
                .text("How do you provide excitation current if you have no other power source?")
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showText(70)
                .text("This is where the concept of Self-Excitation comes into play.")
                .placeNearTarget();
        scene.idle(80);
        scene.overlay().showText(80)
                .text("The stator coils posses a small residual magnetic field which can be used to start a feedback reaction.")
                .pointAt(util.vector().of(2.5, 2.5, 4))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(6, 1, 1, 6, 3, 1), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("To have better control of the output voltage, we'll use a Rheostat.")
                .pointAt(util.vector().of(6.5, 2.75, 1.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().fromTo(5, 1, 3, 5, 3, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(1, 1, 1, 1, 2, 1), Direction.DOWN);
        scene.idle(10);

        scene.electric().connect(commutator, 0, wireconn1, 0, DyeColor.GRAY);
        scene.electric().connect(commutator, 1, conn, 0, DyeColor.GRAY);
        scene.electric().connect(conn, 1, wireconn2, 0, DyeColor.GRAY);
        scene.electric().connect(wireconn2, 0, rheo, 1, DyeColor.GRAY);
        scene.electric().connect(rheo, 2, wireconn1, 0, DyeColor.GRAY);
        scene.electric().tickForever();
        scene.idle(50);

        scene.effects().indicateSuccess(meter);
        scene.overlay().showText(60)
                .text("A self-excited generator is sensitive to a couple of factors.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);
        scene.overlay().showText(100)
                .text("1.Resistance - The 'excitation loop resistance' must be low enough to allow for the field to build up, but not too low as to not overpower the coils.")
                .pointAt(util.vector().of(6.5, 2.75, 1.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(100)
                .text("2.Rotation speed - The generator must spin fast enough to generate enough of the initial starting current and build up the field.")
                .pointAt(util.vector().of(3.5, 2.75, 4))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(110);

        scene.markAsFinished();
    }
    public static void generator3(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("generator_3", "Regulated generator");
        scene.configureBasePlate(0, 0, 7);
        scene.setSceneOffsetY(-3);

        var commutator = util.grid().at(3, 7, 5);
        var meter = util.grid().at(3, 8, 6);
        var rheo = util.grid().at(6, 7, 5);
        var wirec1 = util.grid().at(2, 7, 4);
        var wirec2 = util.grid().at(4, 7, 4);
        var wirec3 = util.grid().at(2, 5, 3);
        var wirec4 = util.grid().at(4, 5, 3);
        var conn = util.grid().at(3, 6, 3);
        var traf = util.grid().at(3, 1, 3);
        var basin = util.grid().at(3, 1, 1);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(2, 1, 4, 4, 8, 6), Direction.UP);
        scene.world().showSection(util.select().position(conn), Direction.UP);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(5, 5, 1, 6, 8, 6), Direction.WEST);
        scene.idle(20);

        scene.electric().connect(commutator, 0, meter, 1, DyeColor.GRAY);
        scene.electric().connect(commutator, 1, meter, 0, DyeColor.GRAY);
        scene.electric().connect(commutator, 1, wirec1, 0, DyeColor.GRAY);
        scene.electric().connect(commutator, 0, wirec2, 0, DyeColor.GRAY);
        scene.idle(5);
        var wirerheo1 = scene.electric().connect(commutator, 0, rheo, 2, DyeColor.RED);
        var wirerheo2 = scene.electric().connect(rheo, 1, conn, 1, DyeColor.RED);
        scene.electric().connect(conn, 0, wirec1, 0, DyeColor.RED);
        scene.electric().tickForever();
        scene.idle(20);

        scene.overlay().showText(60)
                .text("Generator voltage will drop when a load is connected")
                .pointAt(util.vector().blockSurface(meter, Direction.NORTH))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);
        scene.effects().indicateSuccess(meter);
        scene.idle(20);

        scene.world().showSection(util.select().fromTo(wirec3, wirec4), Direction.SOUTH);
        scene.world().showSection(util.select().position(traf), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(basin, basin.north()), Direction.DOWN);
        scene.idle(20);

        var wire1 = scene.electric().connect(wirec1, 0, wirec3, 0, DyeColor.GRAY);
        var wire2 = scene.electric().connect(wirec2, 0, wirec4, 0, DyeColor.GRAY);
        scene.electric().connect(wirec3, 0, traf, 3, DyeColor.GRAY);
        scene.electric().connect(wirec4, 0, traf, 2, DyeColor.GRAY);
        scene.electric().connect(traf, 0, basin, 0, DyeColor.GRAY);
        scene.electric().connect(traf, 1, basin, 1, DyeColor.GRAY);
        scene.idle(20);
        scene.world().setBlock(basin.north(), ModdedBlocks.BASIN_HEATER.getDefaultState().setValue(BasinHeaterBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        scene.idle(20);
        scene.effects().indicateSuccess(basin.north());
        scene.effects().indicateRedstone(meter);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("This effect can be reduced by utilizing the Carbon Pile Regulator")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.electric().removeWire(wire1);
        scene.electric().removeWire(wire2);
        scene.idle(10);
        scene.world().setBlock(basin.north(), ModdedBlocks.BASIN_HEATER.getDefaultState().setValue(BasinHeaterBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.NONE), false);
        scene.electric().removeWire(wirerheo1);
        scene.electric().removeWire(wirerheo2);
        scene.idle(10);

        scene.world().hideSection(util.select().fromTo(rheo, rheo.above()), Direction.UP);
        scene.idle(20);

        var pile = rheo;
        scene.world().setBlock(pile, ModdedBlocks.CARBON_PILE_COIL.getDefaultState().setValue(CarbonPileCoilBlock.HORIZONTAL_FACING, Direction.WEST), false);
        scene.world().setBlock(pile.above(), ModdedBlocks.CARBON_PILE.getDefaultState().setValue(CarbonPileBlock.TOP, true), false);
        scene.world().showSection(util.select().fromTo(pile, pile.above()), Direction.DOWN);
        scene.idle(20);

        scene.world().modifyBlockEntity(pile, CarbonPileCoilBlockEntity.class, be -> be.pileChanged());
        scene.electric().connect(pile, 2, commutator, 0, DyeColor.RED);
        scene.electric().connect(pile, 3, conn, 1, DyeColor.RED);
        scene.idle(20);

        rheo = util.grid().at(6, 6, 3);
        scene.world().hideSection(util.select().fromTo(rheo, rheo.above()), Direction.DOWN);

        scene.overlay().showText(80)
                .text("Without any extra connections, the carbon pile is just a simple resistor.")
                .pointAt(util.vector().blockSurface(pile, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.world().setBlock(rheo, ModdedBlocks.RHEOSTAT.getDefaultState().setValue(RheostatBlock.HORIZONTAL_FACING, Direction.WEST), false);
        scene.world().setBlock(rheo.above(), AllBlocks.COPPER_VALVE_HANDLE.getDefaultState().setValue(ValveHandleBlock.FACING, Direction.UP), false);
        scene.world().modifyBlockEntityNBT(util.select().position(rheo), RheostatBlockEntity.class, tag -> {
            tag.putInt("ScrollValue", 18);
            var arm = tag.getCompound("Arm");
            arm.putFloat("Value", 0.1746f);
            arm.putFloat("Target", 0.1746f);
        });
        scene.world().showSection(util.select().fromTo(rheo, rheo.above()), Direction.DOWN);
        scene.idle(20);

        scene.electric().connect(pile, 0, commutator, 0, DyeColor.BLUE);
        scene.electric().connect(pile, 1, rheo, 2, DyeColor.BLUE);
        scene.electric().connect(rheo, 1, wirec1, 0, DyeColor.BLUE);
        scene.idle(20);

        scene.overlay().showText(50)
                .text("Lets connect the load again")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);

        scene.electric().connect(wirec1, 0, wirec3, 0, DyeColor.GRAY);
        scene.electric().connect(wirec2, 0, wirec4, 0, DyeColor.GRAY);
        scene.idle(20);
        scene.world().setBlock(basin.north(), ModdedBlocks.BASIN_HEATER.getDefaultState().setValue(BasinHeaterBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        scene.idle(20);
        scene.effects().indicateSuccess(basin.north());
        scene.effects().indicateSuccess(meter);
        scene.idle(20);

        scene.markAsFinished();
    }
}
