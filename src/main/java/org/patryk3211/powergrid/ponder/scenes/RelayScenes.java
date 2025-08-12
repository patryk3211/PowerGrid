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

import com.tterrag.registrate.util.entry.BlockEntry;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.SurfaceSwitchBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseState;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.kinetics.variac.VariacBlockEntity;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

public class RelayScenes {
    public static PonderStoryBoard switchSceneFor(BlockEntry<? extends SurfaceSwitchBlock> block, String suffix) {
        return (scene, util) -> switchScene(scene, util, block.get(), suffix);
    }

    public static void switchScene(SceneBuilder scene, SceneBuildingUtil util, SurfaceSwitchBlock block, String suffix) {
        var electric = ElectricInstructions.of(scene);
        scene.title("switch_" + suffix, "Manually switching electricity");
        scene.configureBasePlate(0, 0, 5);

        var source = util.grid().at(2, 1, 3);
        var target = util.grid().at(1, 2, 2);
        var bulb = util.grid().at(3, 2, 2);
        scene.world().setBlock(target, block.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.DOWN)
                .setValue(CustomProperties.ALONG_FIRST_AXIS, false)
                .setValue(BlockStateProperties.OPEN, true), false);

        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(0, 2, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 2, 2), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().position(1, 2, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(3, 2, 2), Direction.DOWN);
        scene.idle(10);

        electric.connectInvisible(util.grid().at(0, 2, 2), 0, source, 0);
        electric.connectInvisible(util.grid().at(4, 2, 2), 0, source, 1);
        electric.connect(util.grid().at(0, 2, 2), 0, target, 1);
        electric.connect(util.grid().at(4, 2, 2), 0, bulb, 0);
        electric.connect(target, 0, bulb, 1);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Switches and buttons allow you to manually toggle electricity")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(50);

        scene.world().modifyBlock(target, state -> state.setValue(BlockStateProperties.OPEN, false), false);
        scene.world().modifyBlock(bulb, state -> state.setValue(LightFixtureBlock.POWER, 2), false);
        scene.effects().indicateSuccess(target);
        if(block.isButton()) {
            scene.idle(10);
            scene.world().modifyBlock(target, state -> state.setValue(BlockStateProperties.OPEN, true), false);
            scene.world().modifyBlock(bulb, state -> state.setValue(LightFixtureBlock.POWER, 0), false);
            scene.idle(30);
        } else {
            scene.idle(40);
        }

        scene.markAsFinished();
        electric.unload();
    }

    public static void hvSwitch(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("hv_switch", "Mechanical switch");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(2, 0, 5), Direction.UP);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 3, 1, 2, 5), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(1, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The High Voltage Switch allows you to toggle electricity using a kinetic input")
                .pointAt(util.vector().topOf(1, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(50);
        scene.world().toggleRedstonePower(util.select().fromTo(1, 1, 3, 1, 2, 3));
        scene.world().modifyBlockEntity(util.grid().at(1, 1, 2), HvSwitchBlockEntity.class, be -> {
            be.setSpeed(-64);
            be.onSpeedChanged(64);
        });
        scene.idle(40);

        scene.markAsFinished();
    }

    public static void contactor(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("contactor", "Heavy-duty relay");
        scene.configureBasePlate(0, 0, 5);

        var target = util.grid().at(2, 2, 2);
        var meter1 = util.grid().at(2, 1, 1);
        var meter2 = util.grid().at(0, 1, 2);
        var source1 = util.grid().at(2, 1, 0);
        var source2 = util.grid().at(4, 1, 2);
        var connector = util.grid().at(2, 2, 1);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(2, 1, 2), Direction.UP);
        scene.idle(10);

        scene.world().showSection(util.select().position(4, 1, 1), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(0, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 1, 1), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 2, 1), Direction.DOWN);
        scene.idle(10);

        electric.connectInvisible(source1, 1, connector, 1);
        electric.connectInvisible(source1, 0, connector, 0);
        electric.connectInvisible(source2, 0, util.grid().at(4, 1, 1), 0);
        electric.connectInvisible(source2, 1, util.grid().at(4, 1, 3), 0);

        electric.connect(connector, 0, meter1, 1);
        electric.connect(connector, 1, meter1, 0);
        electric.connect(util.grid().at(4, 1, 1), 0, target, 2);
        electric.connect(util.grid().at(4, 1, 3), 0, target, 4);
        electric.connect(target, 3, meter2, 0);
        electric.connect(target, 5, meter2, 1);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("A Contactor lets you switch high currents with a lower voltage input.")
                .pointAt(util.vector().topOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        electric.setSource(source1, 25);
        electric.tickFor(10);
        scene.idle(5);
        scene.effects().indicateSuccess(meter2);
        scene.idle(45);

        scene.markAsFinished();
        electric.unload();
    }

    public static void contactorStack(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("contactor_stack", "Electrical Switchboard");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);
        var target = util.grid().at(2, 1, 2);

        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("Contactors can be stacked together")
                .pointAt(util.vector().blockSurface(target, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.world().showSection(util.select().position(target.north()), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(target.south()), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("And only one needs to be powered")
                .pointAt(util.vector().blockSurface(target.north(), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.world().showSection(util.select().position(target.north(2)), Direction.SOUTH);
        scene.idle(30);

        scene.markAsFinished();
    }

    public static void variac(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);

        scene.title("variac", "Variable transformer");
        scene.configureBasePlate(0, 0, 5);

        var source = util.grid().at(3, 1, 2);
        var meter1 = util.grid().at(3, 1, 1);
        var meter2 = util.grid().at(1, 1, 1);
        var target = util.grid().at(2, 2, 2);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(2, 1, 2), Direction.UP);
        scene.idle(10);

        scene.world().showSection(util.select().position(meter1), Direction.DOWN);
        scene.world().showSection(util.select().position(meter2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(target, target.above()), Direction.DOWN);
        scene.idle(10);

        scene.electric().connectInvisible(source, 0, meter1, 0);
        scene.electric().connectInvisible(source, 1, meter1, 1);
        scene.electric().connect(meter1, 0, target, 0);
        scene.electric().connect(meter1, 1, target, 1);
        scene.electric().connect(meter2, 0, target, 1);
        scene.electric().connect(meter2, 1, target, 2);
        scene.electric().setSource(source, 160);
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("A variac can be used to variably transform voltages using a kinetic input")
                .pointAt(util.vector().blockSurface(target, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setKineticSpeed(util.select().fromTo(target, target.above()), 16);
        scene.world().modifyBlockEntity(target, VariacBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.effects().rotationSpeedIndicator(target.above());
        scene.electric().tickFor(40);
        scene.idle(40);
        scene.world().setKineticSpeed(util.select().fromTo(target, target.above()), 0);
        scene.world().modifyBlockEntity(target, VariacBlockEntity.class, be -> be.onSpeedChanged(16));

        scene.markAsFinished();
    }

    public static void deviceConnector(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("device_connector", "Device connector");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);

        var target = util.grid().at(2, 2, 2);

        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("Some devices don't directly expose their electrical terminals")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(5);
        scene.effects().indicateSuccess(target.below());
        scene.idle(10);

        scene.overlay().showText(80)
                .text("To power them, you'll have to place a Device Connector and attach your wires to it")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("The Device Connector can also be used to power Forge Energy devices")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void fuse(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("fuse", "Protecting your equipment");
        scene.configureBasePlate(0, 0, 5);

        var target = util.grid().at(2, 1, 2);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(0, 1, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 1, 2), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(10);

        electric.connect(util.grid().at(0, 1, 2), 0, target, 1);
        electric.connect(util.grid().at(4, 1, 2), 0, target, 0);
        scene.idle(10);

        scene.overlay().showControls(util.vector().centerOf(target), Pointing.LEFT, 40)
                .rightClick()
                .withItem(ModdedItems.IRON_WIRE.asStack());
        scene.idle(30);
        scene.world().modifyBlock(target, state -> state.setValue(FuseHolderBlock.STATE, FuseState.CLOSED), false);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("A fuse can be used to protect your devices from too much current")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("It will burn if the current exceeds the set value")
                .pointAt(util.vector().centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.world().modifyBlock(target, state -> state.setValue(FuseHolderBlock.STATE, FuseState.BLOWN), false);
        scene.addInstruction(pScene -> {
            var pos = util.vector().centerOf(target);
            SparkParticleData.explodeParticles(pScene.getWorld(), (float) pos.x, (float) pos.y, (float) pos.z, Direction.UP, 5);
        });
        scene.idle(50);

        scene.markAsFinished();
        electric.unload();
    }
}
