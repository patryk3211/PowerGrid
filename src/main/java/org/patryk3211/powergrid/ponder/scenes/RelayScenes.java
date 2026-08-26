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
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileCoilBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.HvBreakerBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlockEntity;
import org.patryk3211.powergrid.electricity.electricswitch.SurfaceSwitchBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseState;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.particles.SparkParticleData;
import org.patryk3211.powergrid.electricity.redstoneconverter.RedstoneConverterBlockEntity;
import org.patryk3211.powergrid.electricity.transformer.NetherTransformerBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlock;
import org.patryk3211.powergrid.kinetics.rheostat.RheostatBlockEntity;
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
                .setValue(SurfaceSwitchBlock.ROTATION, 0)
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
        electric.connect(util.grid().at(0, 2, 2), 0, target, 0);
        electric.connect(util.grid().at(4, 2, 2), 0, bulb, 1);
        electric.connect(target, 1, bulb, 0);
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

    public static void hvBreaker(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("hv_breaker", "A high power switch");
        scene.configureBasePlate(0, 0, 5);

        var breaker = util.grid().at(2, 1, 2);
        var light = util.grid().at(2, 2, 2);
        var connector1 = util.grid().at(4, 2, 2);
        var connector2 = util.grid().at(2, 1, 4);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(breaker), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(connector1.below(), connector1), Direction.DOWN);
        scene.world().showSection(util.select().position(connector2), Direction.DOWN);
        scene.world().showSection(util.select().position(light), Direction.DOWN);

        scene.electric().connect(connector1, 0, light, 0);
        scene.electric().connect(light, 1, breaker, 1);
        scene.electric().connect(breaker, 0, connector2, 0);
        scene.electric().addSource(connector1, 0, 122);
        scene.electric().addSource(connector2, 0, 0);
        scene.electric().tickForever();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(2, 1, 0, 2, 1, 1), Direction.DOWN);
        scene.world().showSection(util.select().position(1, 1, 2), Direction.EAST);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("HV Breaker can be used to safely switch a high power load")
                .pointAt(util.vector().blockSurface(breaker, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(70)
                .text("To close it, you must first wind it up")
                .pointAt(util.vector().of(1.5, 1.5, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        scene.world().setKineticSpeed(util.select().fromTo(breaker, breaker.west()), 32);
        scene.world().modifyBlockEntity(breaker, HvBreakerBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.idle(90);
        scene.world().setKineticSpeed(util.select().fromTo(breaker, breaker.west()), 0);
        scene.world().modifyBlockEntity(breaker, HvBreakerBlockEntity.class, be -> be.onSpeedChanged(32));

        scene.effects().indicateSuccess(breaker);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Its state can then be toggled with a redstone pulse")
                .pointAt(util.vector().of(2.5, 1.0, 1.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().toggleRedstonePower(util.select().fromTo(breaker, breaker.north(3)));
        scene.idle(40);
        scene.world().toggleRedstonePower(util.select().fromTo(breaker, breaker.north(3)));
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The breaker can be opened without having to wind it up again")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

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

    public static void rheostat(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);

        scene.title("rheostat", "Variable resistor");
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
        scene.electric().connect(meter1, 1, target, 2);
        scene.electric().connect(meter2, 0, target, 1);
        scene.electric().connect(meter2, 1, target, 2);
        scene.electric().setSource(source, 20);
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("A rheostat can be used to vary electrical resistance using a kinetic input")
                .pointAt(util.vector().blockSurface(target, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().setKineticSpeed(util.select().fromTo(target, target.above()), -16);
        scene.world().modifyBlockEntity(target, RheostatBlockEntity.class, be -> be.onSpeedChanged(0));
        scene.effects().rotationSpeedIndicator(target.above());
        scene.electric().tickFor(40);
        scene.idle(40);
        scene.world().setKineticSpeed(util.select().fromTo(target, target.above()), 0);
        scene.world().modifyBlockEntity(target, RheostatBlockEntity.class, be -> be.onSpeedChanged(-16));

        scene.markAsFinished();
    }

    public static void powerResistor(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);

        scene.title("power_resistor", "High-power resistor");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);

        var target = util.grid().at(2, 2, 2);

        scene.world().showSection(util.select().fromTo(target.below(), target), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Power Resistor is a simple device which lets you limit the current flow in a circuit.")
                .pointAt(util.vector().blockSurface(target, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(60)
                .text("You can change the resistance by clicking on its top")
                .pointAt(util.vector().topOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        var connector1 = util.grid().at(0, 2, 2);
        var connector2 = util.grid().at(4, 2, 2);
        var gauge = util.grid().at(1, 1, 1);
        scene.world().showSection(util.select().fromTo(connector1.below(), connector1), Direction.EAST);
        scene.world().showSection(util.select().fromTo(connector2.below(), connector2), Direction.WEST);
        scene.idle(5);
        scene.world().showSection(util.select().position(gauge), Direction.DOWN);
        scene.idle(5);

        scene.electric().connect(connector1, 0, gauge, 1);
        scene.electric().connect(gauge, 0, target, 1);
        scene.electric().connect(connector2, 0, target, 0);
        scene.idle(5);

        scene.electric().addSource(connector1, 0, 1);
        scene.electric().addSource(connector2, 0, 0);
        scene.electric().tickFor(20);

        scene.overlay().showText(80)
                .text("The current flow is equal to the voltage across the resistor divided by its resistance (I = V / R)")
                .pointAt(util.vector().blockSurface(gauge, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

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
                .text("The Device Connector can also be used to power Forge Energy and Create: The Factory Must Grow devices (if installed)")
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

    public static void carbonPile(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("carbon_pile", "A pile of coal");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 2, 2), Direction.DOWN);
        scene.idle(10);

        var pile = util.grid().at(2, 2, 2);
        var currentMeter = util.grid().at(1, 1, 2);
        var voltageMeter = util.grid().at(2, 1, 1);
        var connector1 = util.grid().at(0, 2, 2);
        var connector2 = util.grid().at(4, 2, 2);

        scene.overlay().showText(80)
                .text("To assemble a Carbon Pile you must place Blocks of Coal on top of a Carbon Pile Coil")
                .pointAt(util.vector().topOf(pile))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(2, 3, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(2, 4, 2), Direction.DOWN);
        scene.idle(15);
        var state = ModdedBlocks.CARBON_PILE.getDefaultState();
        scene.world().setBlock(util.grid().at(2, 3, 2), state.setValue(CarbonPileBlock.TOP, false), false);
        scene.world().setBlock(util.grid().at(2, 4, 2), state.setValue(CarbonPileBlock.TOP, true), false);
        scene.world().modifyBlockEntity(util.grid().at(2, 2, 2), CarbonPileCoilBlockEntity.class, be -> be.pileChanged());
        scene.idle(10);
        scene.effects().indicateSuccess(util.grid().at(2, 2, 2));
        scene.effects().indicateSuccess(util.grid().at(2, 3, 2));
        scene.effects().indicateSuccess(util.grid().at(2, 4, 2));
        scene.idle(20);

        scene.world().showSection(util.select().fromTo(connector1.below(), connector1), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(connector2.below(), connector2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(currentMeter), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(voltageMeter), Direction.DOWN);
        scene.idle(5);

        scene.electric().connect(connector2, 0, pile, 3);
        scene.electric().connect(currentMeter, 1, pile, 2);
        scene.electric().connect(voltageMeter, 0, pile, 0);
        scene.electric().connect(voltageMeter, 1, pile, 1);
        scene.electric().connect(currentMeter, 0, connector1, 0);
        scene.electric().addSource(connector1, 0, 0);
        scene.electric().addSource(connector2, 0, 70);
        scene.electric().addSource(voltageMeter, 0, 0);
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Resistance of the Carbon Pile changes based on the current provided to the coil")
                .pointAt(util.vector().of(2.5, 2.5, 2.0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.electric().addSource(voltageMeter, 1, 10);
        scene.electric().tickFor(20);
        scene.idle(20);
        scene.effects().indicateSuccess(currentMeter);
        scene.effects().indicateSuccess(voltageMeter);
        scene.idle(30);

        scene.overlay().showText(80)
                .text("The resistance values of the pile increase with the height of the structure")
                .pointAt(util.vector().of(2.5, 3.5, 2.0))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("The overall resistance can also be tuned with a value panel on top of the pile")
                .pointAt(util.vector().of(2.5, 5.0, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void redstoneConverter(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("redstone_converter", "Reading redstone");
        scene.configureBasePlate(0, 0, 5);

        var converter = util.grid().at(2, 1, 2);
        var gauge = util.grid().at(2, 1, 3);
        var conn1 = util.grid().at(4, 2, 1);
        var conn2 = util.grid().at(4, 2, 3);

        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 1, 0, 2, 1, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 1, 1, 4, 2, 1), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(4, 1, 3, 4, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.electric().connect(conn1, 0, converter, 0);
        scene.electric().connect(conn2, 0, converter, 1);
        scene.electric().connect(converter, 2, gauge, 0);
        scene.electric().connect(gauge, 1, conn2, 0);
        scene.electric().tickForever();
        scene.idle(5);
        scene.electric().addSource(conn1, 0, 2);
        scene.electric().addSource(conn2, 0, 0);
        scene.idle(5);

        scene.overlay().showText(80)
                .text("The Redstone Converter is a device that converts redstone signals into resistance changes")
                .pointAt(util.vector().of(2.5, 1.2, 2.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.world().modifyBlockEntityNBT(util.select().position(2, 1, 0),
                AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 2));
        scene.world().modifyBlock(util.grid().at(2, 1, 1), state -> state.setValue(BlockStateProperties.POWER, 2), false);
        scene.world().toggleRedstonePower(util.select().position(converter));
        scene.world().modifyBlockEntity(converter, RedstoneConverterBlockEntity.class, conv -> conv.updateResistance(2 / 15f));
        scene.idle(40);
        scene.world().modifyBlockEntityNBT(util.select().position(2, 1, 0),
                AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 5));
        scene.world().modifyBlock(util.grid().at(2, 1, 1), state -> state.setValue(BlockStateProperties.POWER, 5), false);
        scene.world().modifyBlockEntity(converter, RedstoneConverterBlockEntity.class, conv -> conv.updateResistance(5 / 15f));
        scene.idle(40);
        scene.world().modifyBlockEntityNBT(util.select().position(2, 1, 0),
                AnalogLeverBlockEntity.class, nbt -> nbt.putInt("State", 12));
        scene.world().modifyBlock(util.grid().at(2, 1, 1), state -> state.setValue(BlockStateProperties.POWER, 12), false);
        scene.world().modifyBlockEntity(converter, RedstoneConverterBlockEntity.class, conv -> conv.updateResistance(12 / 15f));
        scene.idle(40);

        scene.overlay().showText(60)
                .text("Its internal structure resembles that of a potentiometer")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);

        scene.overlay().showText(80)
                .text("Resistance between non-inverting and tap pads will go down with the applied signal...")
                .pointAt(util.vector().of(2.8, 1.1, 2.4))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("...while resistance between inverting and tap pads will go up")
                .pointAt(util.vector().of(2.8, 1.1, 2.6))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(70)
                .text("When attached to a block it'll act similar to a Redstone Comparator")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(80)
                .text("For some blocks, the Redstone Converter can provide more precision than a Redstone Comparator")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void transformerSizes(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("transformer_sizes", "Transformers");
        scene.setNextUpEnabled(true);
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);

        var smallTr = util.grid().at(2, 1, 1);
        scene.world().showSection(util.select().position(smallTr), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showControls(util.vector().blockSurface(smallTr, Direction.NORTH), Pointing.RIGHT, 30)
                .withItem(AllItems.WRENCH.asStack());
        scene.idle(20);
        scene.world().setBlock(smallTr, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState().setValue(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X), false);
        scene.idle(20);

        var mediumTr = util.grid().at(2, 1, 3);
        scene.world().showSection(util.select().fromTo(mediumTr, mediumTr.west().above()), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showControls(util.vector().blockSurface(mediumTr.above(), Direction.NORTH), Pointing.RIGHT, 30)
                .withItem(AllItems.WRENCH.asStack());
        scene.idle(20);
        scene.world().setBlock(mediumTr, ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .setValue(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TransformerMediumBlock.PART, 1), false);
        scene.world().setBlock(mediumTr.west(), ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .setValue(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TransformerMediumBlock.PART, 0), false);
        scene.world().setBlock(mediumTr.above(), ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .setValue(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TransformerMediumBlock.PART, 3), false);
        scene.world().setBlock(mediumTr.above().west(), ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .setValue(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TransformerMediumBlock.PART, 2), false);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Transformers come in different sizes, each of them offering different power capabilities and winding capacities")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void transformerWinding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("transformer_winding", "Winding a transformer");
        scene.setNextUpEnabled(true);
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);

        var tr = util.grid().at(2, 1, 2);
        scene.world().showSection(util.select().position(tr), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("To wind a transformer, first select the starting terminal for your winding")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        var stack = new ItemStack(ModdedItems.WIRE.get(), 1);
        scene.overlay().showControls(util.vector().of(2.8, 1.9, 2.0), Pointing.RIGHT, 30).withItem(stack);
        scene.idle(30);

        var side = util.vector().blockSurface(tr, Direction.NORTH);
        scene.overlay().showText(80)
                .text("Next, click on the transformer body and pick the number of turn you want to add")
                .attachKeyFrame()
                .pointAt(side)
                .placeNearTarget();
        scene.idle(50);
        scene.overlay().showControls(side, Pointing.UP, 30).withItem(stack);
        scene.idle(40);

        scene.overlay().showText(80)
                .text("Lastly, select the end terminal for your winding")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showControls(util.vector().of(2.2, 1.9, 2.0), Pointing.LEFT, 30).withItem(stack);
        scene.idle(20);
        scene.world().setBlock(tr, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                .setValue(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TransformerSmallBlock.COILS, 1), false);
        scene.idle(10);

        scene.effects().indicateSuccess(tr);
        scene.idle(10);

        scene.overlay().showText(60)
                .text("Repeat for the secondary winding")
                .placeNearTarget();
        scene.idle(50);

        scene.world().setBlock(tr, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                .setValue(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .setValue(TransformerSmallBlock.COILS, 2), false);
        scene.effects().indicateSuccess(tr);
        scene.idle(30);

        scene.overlay().showText(80)
                .text("Your transformer will now transform voltage with the ratio you wound")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void transformerNether(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("transformer_nether", "Interdimensional transformers");
        scene.setNextUpEnabled(true);
        scene.configureBasePlate(0, 0, 7);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(1, 1, 4, 5, 4, 4), Direction.UP);
        scene.idle(10);

        scene.overlay().showText(70)
                .text("Assembling a transformer next to a nether portal...")
                .attachKeyFrame()
                .pointAt(util.vector().of(3.5, 2.5, 4))
                .placeNearTarget();
        scene.idle(60);

        scene.world().showSection(util.select().position(3, 1, 3), Direction.DOWN);
        scene.idle(15);
        scene.world().showSection(util.select().position(3, 2, 3), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showControls(util.vector().of(3, 2.5, 3.5), Pointing.DOWN, 30)
                .withItem(AllItems.WRENCH.asStack())
                .rightClick();
        scene.idle(20);

        var state = ModdedBlocks.NETHER_TRANSFORMER.getDefaultState()
                .setValue(NetherTransformerBlock.HORIZONTAL_AXIS, Direction.Axis.Z);
        scene.world().setBlock(util.grid().at(3, 1, 3), state.setValue(NetherTransformerBlock.PART, 0), false);
        scene.world().setBlock(util.grid().at(3, 2, 3), state.setValue(NetherTransformerBlock.PART, 2), false);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("...will create a linked transformer on the other end.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.rotateCameraY(360);
        scene.world().hideSection(util.select().layers(0, 1), Direction.UP);
        scene.idle(15);

        var netherFloor = scene.world().showIndependentSection(util.select().layer(7), Direction.UP);
        var netherFlora = scene.world().showIndependentSection(util.select().fromTo(0, 8, 0, 3, 8, 3), Direction.UP);
        scene.world().moveSection(netherFloor, util.vector().of(0, -7, 0), 0);
        scene.world().moveSection(netherFlora, util.vector().of(0, -7, 0), 0);
        scene.idle(60);

        scene.markAsFinished();
    }
}
