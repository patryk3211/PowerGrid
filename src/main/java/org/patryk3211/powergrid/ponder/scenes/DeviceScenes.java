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
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.patryk3211.powergrid.electricity.battery.BatteryBlockEntity;
import org.patryk3211.powergrid.electricity.battery.MultiBlockBatteryEntity;
import org.patryk3211.powergrid.electricity.crt.CRTBlock;
import org.patryk3211.powergrid.electricity.electromagnet.ElectromagnetBlockEntity;
import org.patryk3211.powergrid.electricity.electromagnet.MagnetizingBehaviour;
import org.patryk3211.powergrid.electricity.heater.HeaterBlockEntity;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlockEntity;
import org.patryk3211.powergrid.electricity.modulardisplay.ModularDisplayBlockEntity;
import org.patryk3211.powergrid.electricity.modulardisplay.modules.AlphabetLetterModule;
import org.patryk3211.powergrid.electricity.modulardisplay.modules.ZeroToNineNumberModule;
import org.patryk3211.powergrid.electricity.solarpanel.SolarPanelBlock;
import org.patryk3211.powergrid.kinetics.plotter.PlotterBlockEntity;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlockEntity;
import org.patryk3211.powergrid.ponder.base.PowerGridSceneBuilder;

import java.util.Optional;
import java.util.Random;
import java.util.function.UnaryOperator;

;

public class DeviceScenes {
    public static void heatingCoilBasic(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("heating_coil_basic", "Warming up the atmosphere");
        scene.setNextUpEnabled(true);
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(4, 1, 1, 5, 1, 3), Direction.UP);
        scene.world().showSection(util.select().position(0, 1, 2), Direction.UP);
        scene.world().showSection(util.select().position(5, 1, 2), Direction.UP);
        scene.idle(5);

        var heatingCoil = util.grid().at(3, 1, 2);
        var voltageSource = util.grid().at(6, 2, 2);
        var deviceConnector = util.grid().at(3, 2, 2);
        scene.world().showSection(util.select().position(4, 2, 1), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 2, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(heatingCoil), Direction.DOWN);
        scene.world().showSection(util.select().position(deviceConnector), Direction.DOWN);
        scene.idle(10);

        scene.electric().connect(util.grid().at(4, 2, 1), 0, deviceConnector, 0);
        scene.electric().connect(util.grid().at(4, 2, 3), 0, deviceConnector, 1);
        scene.electric().connectInvisible(util.grid().at(4, 2, 1), 0, voltageSource, 0);
        scene.electric().connectInvisible(util.grid().at(4, 2, 3), 0, voltageSource, 1);
        scene.idle(5);

        scene.overlay().showText(60)
                .text("The heating coil can be used to heat up the passing Air Flow if enough power is applied to it")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(heatingCoil))
                .placeNearTarget();
        scene.world().modifyBlockEntity(heatingCoil, HeaterBlockEntity.class, c -> {
            var temp = c.getBehaviour(ThermalBehaviour.TYPE);
            temp.setTemperature(380);
        });
        scene.electric().setSource(voltageSource, 32);
        scene.electric().tickFor(10);
        scene.idle(100);

        scene.overlay().showText(60)
                .text("By applying a bigger voltage the Air Flow can be used for bulk blasting")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(heatingCoil))
                .placeNearTarget();

        scene.electric().setSource(voltageSource, 38);
        scene.electric().tickFor(10);
        scene.world().modifyBlockEntity(heatingCoil, HeaterBlockEntity.class, c -> {
            var temp = c.getBehaviour(ThermalBehaviour.TYPE);
            temp.setTemperature(510);
        });
        scene.idle(80);

        scene.markAsFinished();
    }

    public static void heatingCoilSpeed(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("heating_coil_speed", "Electrified bulk processing");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(0, 1, 4, 3, 1, 5), Direction.UP);
        scene.world().showSection(util.select().fromTo(1, 1, 0, 3, 1, 0), Direction.UP);
        scene.world().showSection(util.select().position(2, 0, 5), Direction.UP);
        scene.idle(5);

        var heatingCoil = util.grid().at(1, 1, 3);
        var voltageSource = util.grid().at(2, 1, 6);
        var deviceConnector = util.grid().at(1, 2, 3);
        scene.world().showSection(util.select().position(0, 2, 4), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 2, 4), Direction.DOWN);
        scene.world().showSection(util.select().position(heatingCoil), Direction.DOWN);
        scene.world().showSection(util.select().position(deviceConnector), Direction.DOWN);
        scene.idle(10);

        scene.electric().connect(util.grid().at(0, 2, 4), 0, deviceConnector, 0);
        scene.electric().connect(util.grid().at(2, 2, 4), 0, deviceConnector, 1);
        scene.electric().connectInvisible(util.grid().at(0, 2, 4), 0, voltageSource, 0);
        scene.electric().connectInvisible(util.grid().at(2, 2, 4), 0, voltageSource, 1);
        scene.electric().setSource(voltageSource, 32);
        scene.world().modifyBlockEntity(heatingCoil, HeaterBlockEntity.class, c -> {
            var temp = c.getBehaviour(ThermalBehaviour.TYPE);
            temp.setTemperature(380);
        });
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.world().setBlock(util.grid().at(3, 1, 3), Blocks.FIRE.defaultBlockState(), false);
        scene.world().showSection(util.select().position(3, 1, 3), Direction.WEST);

        scene.overlay().showText(60)
                .text("The heating coil allows for faster bulk processing")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(heatingCoil))
                .placeNearTarget();
        scene.idle(20);

        var stack = new ItemStack(Items.BEEF);
        var cooked = new ItemStack(Items.COOKED_BEEF);

        var heaterEntity = scene.world().createItemEntity(util.vector().centerOf(1, 2, 1), util.vector().of(0, 0.1, 0), stack);
        var fireEntity = scene.world().createItemEntity(util.vector().centerOf(3, 2, 1), util.vector().of(0, 0.1, 0), stack);
        scene.idle(10);
        scene.world().modifyEntity(heaterEntity, e -> e.setDeltaMovement(0, 0, -0.2f));
        scene.world().modifyEntity(fireEntity, e -> e.setDeltaMovement(0, 0, -0.2f));

        var item1Vec = util.vector().blockSurface(util.grid().at(1, 1, 0), Direction.SOUTH).add(0, 0, 0.1);
        var item2Vec = util.vector().blockSurface(util.grid().at(3, 1, 0), Direction.SOUTH).add(0, 0, 0.1);

        scene.effects().emitParticles(item1Vec.add(0, 0.2f, 0), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 60);
        scene.effects().emitParticles(item2Vec.add(0, 0.2f, 0), scene.effects().simpleParticleEmitter(ParticleTypes.LARGE_SMOKE, Vec3.ZERO), 1, 100);

        scene.idle(60);
        scene.world().modifyEntity(heaterEntity, e -> ((ItemEntity) e).setItem(cooked));
        scene.overlay().showControls(item1Vec, Pointing.DOWN, 20).withItem(cooked);
        scene.idle(40);
        scene.world().modifyEntity(fireEntity, e -> ((ItemEntity) e).setItem(cooked));
        scene.overlay().showControls(item2Vec, Pointing.DOWN, 20).withItem(cooked);

        scene.idle(20);
        scene.markAsFinished();
    }

    public static void light(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("light", "Lighting up the world with electricity");
        scene.configureBasePlate(0, 0, 5);

        var light = util.grid().at(2, 2, 2);
        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 1, 2), Direction.NORTH);
        scene.idle(5);

        scene.world().showSection(util.select().position(0, 2, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(4, 2, 2), Direction.DOWN);
        scene.idle(5);

        scene.world().showSection(util.select().position(light), Direction.DOWN);
        scene.idle(5);

        scene.electric().connect(util.grid().at(0, 2, 2), 0, light, 0);
        scene.electric().connect(util.grid().at(4, 2, 2), 0, light, 1);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("When light bulbs are inserted into fixtures and powered on they turn electricity into light")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(light).subtract(0, 0.5, 0))
                .placeNearTarget();
        scene.idle(40);
        scene.world().modifyBlock(light, state -> state.setValue(LightFixtureBlock.POWER, 2), false);
        scene.idle(70);

        scene.overlay().showText(80)
                .text("If the voltage is not high enough, the light bulb will be dimmer")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(light).subtract(0, 0.5, 0))
                .placeNearTarget();
        scene.idle(40);
        scene.world().modifyBlock(light, state -> state.setValue(LightFixtureBlock.POWER, 1), false);
        scene.idle(50);

        scene.overlay().showText(80)
                .text("You can change the color of the light bulb by right-clicking it with a dye")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().of(2.5, 2.5, 2.5), Pointing.DOWN, 40)
                .withItem(new ItemStack(Items.RED_DYE))
                .rightClick();
        scene.idle(20);
        scene.world().modifyBlockEntity(light, LightFixtureBlockEntity.class, be -> be.setColor(DyeColor.RED));
        scene.idle(20);

        scene.markAsFinished();
    }

    public static void growthLamp(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("growth_lamp", "Accelerating crop growth");
        scene.configureBasePlate(0, 0, 5);

        var light = util.grid().at(2, 2, 2);
        scene.showBasePlate();
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 2), Direction.UP);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(2, 1, 2, 2, 3, 4), Direction.NORTH);
        scene.idle(15);

        scene.overlay().showText(80)
                .text("The growth lamp is a special type of light bulb which accelerates crop growth in a certain area when powered on")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(light, Direction.WEST))
                .placeNearTarget();
        scene.idle(40);
        scene.world().modifyBlock(light, state -> state.setValue(LightFixtureBlock.POWER, 2), false);
//        scene.idle(70);

        var crops = new BlockPos[] {
                util.grid().at(1, 1, 1),
                util.grid().at(2, 1, 1),
                util.grid().at(1, 1, 2),
                util.grid().at(3, 1, 2)
        };

        var random = new Random();
        UnaryOperator<BlockState> growCrop = state -> state.setValue(BlockStateProperties.AGE_7, Math.min(state.getValue(BlockStateProperties.AGE_7) + 1, 7));
        for(int i = 0; i < 15; ++i) {
            scene.world().modifyBlock(crops[random.nextInt(crops.length)], growCrop, false);
            scene.idle(10);
        }

        scene.markAsFinished();
    }

    public static void motor(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("electric_motor", "Turning electricity into rotation");
        scene.configureBasePlate(1, 0, 5);

        var source = util.grid().at(4, 1, 4);
        var motor = util.grid().at(4, 1, 2);
        var gauge = util.grid().at(5, 2, 2);
        scene.electric().connectInvisible(source, 0, util.grid().at(5, 1, 1), 0);
        scene.electric().connectInvisible(source, 1, util.grid().at(5, 1, 3), 0);

        scene.showBasePlate();
        scene.world().showSection(util.select().position(gauge.below()), Direction.UP);
        scene.idle(5);

        scene.world().showSection(util.select().position(gauge), Direction.DOWN);
        scene.world().showSection(util.select().position(5, 1, 1), Direction.DOWN);
        scene.world().showSection(util.select().position(5, 1, 3), Direction.DOWN);
        scene.idle(5);

        scene.world().showSection(util.select().position(0, 0, 3), Direction.EAST);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 1, 1, 2), Direction.EAST);
        scene.world().showSection(util.select().fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
        scene.idle(5);

        scene.world().showSection(util.select().position(motor), Direction.DOWN);
        scene.idle(5);

        scene.electric().connect(util.grid().at(5, 1, 1), 0, gauge, 0);
        scene.electric().connect(util.grid().at(5, 1, 1), 0, motor, 1);
        scene.electric().connect(util.grid().at(5, 1, 3), 0, gauge, 1);
        scene.electric().connect(util.grid().at(5, 1, 3), 0, motor, 0);
        scene.electric().setSource(source, 50);
        scene.electric().tickFor(10);

        scene.world().setKineticSpeed(util.select().fromTo(0, 1, 2, 4, 1, 2), 64);
        scene.world().setKineticSpeed(util.select().position(0, 0, 3), -32);
        scene.effects().rotationSpeedIndicator(motor.west());
        scene.idle(15);

        scene.overlay().showText(80)
                .text("The electric motor lets you convert electricity into rotation")
                .attachKeyFrame()
                .pointAt(util.vector().topOf(motor))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("The speed of the motor depends on the voltage you provide")
                .attachKeyFrame()
                .pointAt(util.vector().blockSurface(gauge, Direction.WEST))
                .placeNearTarget();
        scene.idle(40);
        scene.electric().setSource(source, 100);
        scene.electric().tickFor(10);
        scene.world().multiplyKineticSpeed(util.select().everywhere(), 2.0f);
        scene.effects().rotationSpeedIndicator(motor.west());
        scene.idle(50);

        scene.markAsFinished();
    }

    public static void constantSpeedMotor(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("constant_speed_motor", "Regulated Motor");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);
        var section = scene.world().showIndependentSection(util.select().fromTo(1, 1, 3, 2, 2, 3), Direction.DOWN);
        scene.world().moveSection(section, util.vector().of(0, 0, -1), 0);
        scene.idle(10);

        scene.world().setKineticSpeed(util.select().fromTo(1, 1, 2, 2, 2, 3), 32);
        scene.world().setKineticSpeed(util.select().fromTo(1, 1, 3, 2, 1, 3), 8);

        scene.overlay().showText(80)
                .text("A Constant Speed Motor combines the functionality of an Electric Motor and a Speed Controller")
                .pointAt(util.vector().of(1.5, 1.5, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().hideIndependentSection(section, Direction.UP);
        scene.idle(20);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 2, 1, 2), Direction.DOWN);
        scene.idle(20);
        scene.effects().indicateSuccess(util.grid().at(2, 1, 2));
        scene.idle(20);

        scene.overlay().showText(90)
                .text("Its speed is constant while the provided voltage determines the stress capacity and rotation direction")
                .pointAt(util.vector().topOf(2, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(100);

        scene.markAsFinished();
    }

    public static void basinHeater(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("basin_heater", "High power heating");
        scene.configureBasePlate(0, 0, 5);

        var target = util.grid().at(2, 1, 2);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().position(target.above()), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Basin Heater can provide Heat to Items processed in a Basin")
                .pointAt(util.vector().blockSurface(target, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().hideSection(util.select().position(target.above()), Direction.UP);
        scene.idle(20);

        scene.world().modifyBlock(target, state -> state.setValue(BasinHeaterBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED), false);
        scene.idle(40);

        scene.overlay().showText(80)
                .text("When given enough power the heater can give the highest level of heat")
                .pointAt(util.vector().topOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.world().modifyBlock(target, state -> state.setValue(BasinHeaterBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING), false);
        scene.idle(50);

        scene.markAsFinished();
    }

    public static void servo(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("servo", "Precise movements");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().layer(1), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().layer(2), Direction.DOWN);
        scene.idle(10);

        var plank = scene.world().showIndependentSection(util.select().layer(3), Direction.DOWN);
        scene.world().moveSection(plank, util.vector().of(0, 0, 0), 0);

        scene.overlay().showText(80)
                .text("A Servo can be used to precisely control mechanical movements using an electric signal")
                .pointAt(util.vector().topOf(4, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        var bearing = util.grid().at(2, 2, 2);
        var rotationDuration = 37 * 2;

        scene.world().setKineticSpeed(util.select().layer(1), 16);
        scene.world().setKineticSpeed(util.select().position(2, 1, 2), -16);
        scene.effects().rotationSpeedIndicator(util.grid().at(4, 1, 2));
        scene.world().rotateBearing(bearing, 360, rotationDuration);
        scene.world().rotateSection(plank, 0, 360, 0, rotationDuration);
        scene.idle(rotationDuration);

        rotationDuration = 69;
        scene.world().setKineticSpeed(util.select().layer(1), -16);
        scene.world().setKineticSpeed(util.select().position(2, 1, 2), 16);
        scene.effects().rotationSpeedIndicator(util.grid().at(4, 1, 2));
        scene.world().rotateBearing(bearing, -315, rotationDuration);
        scene.world().rotateSection(plank, 0, -315, 0, rotationDuration);
        scene.idle(rotationDuration);

        scene.world().setKineticSpeed(util.select().layer(1), 0);

        scene.overlay().showText(80)
                .text("The control pin accepts a voltage between -5 and 5 volts, which maps to -360 and 360 degrees of rotation")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void bell(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("bell", "Alarm bell");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(2, 2, 2), Direction.SOUTH);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Alarm Bell is an electric device which makes sound when you power it.")
                .pointAt(util.vector().centerOf(2, 2, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void electromagnet(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("electromagnet", "Processing Items with the Electromagnet");
        scene.configureBasePlate(0, 0, 5);
        scene.world().showSection(util.select().layer(0), Direction.UP);
        scene.idle(5);

        var depot = scene.world().showIndependentSection(util.select().position(2, 1, 1), Direction.DOWN);
        scene.world().moveSection(depot, util.vector().of(0, 0, 1), 0);
        scene.idle(10);

        var magnet = util.select().position(2, 3, 2);
        var magnetPos = util.grid().at(2, 3, 2);
        var depotPos = util.grid().at(2, 1, 1);

        scene.world().modifyBlockEntity(magnetPos, ElectromagnetBlockEntity.class, be -> {
                var behavior = be.getBehaviour(ThermalBehaviour.TYPE);
                if(behavior != null)
                    behavior.behaviourFlags(0);
        });
        scene.electric().addSource(magnetPos, 0, 200);
        scene.electric().addSource(magnetPos, 1, 0);
        scene.electric().tickFor(5);

        scene.world().showSection(magnet, Direction.DOWN);
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(2, 1, 3, 2, 1, 5), Direction.NORTH);
        scene.idle(3);
        scene.world().showSection(util.select().position(2, 2, 3), Direction.SOUTH);
        scene.idle(3);
        scene.world().showSection(util.select().position(2, 3, 3), Direction.NORTH);

        scene.effects().indicateSuccess(magnetPos);
        scene.idle(10);

        var pressSide = util.vector().blockSurface(magnetPos, Direction.WEST);
        scene.overlay().showText(60)
                .pointAt(pressSide)
                .placeNearTarget()
                .attachKeyFrame()
                .text("The Electromagnet can process items provided beneath it");
        scene.idle(70);
        scene.overlay().showText(60)
                .pointAt(pressSide.subtract(0, 2, 0))
                .placeNearTarget()
                .text("The Input items can be dropped or placed on a Depot under the Electromagnet");
        scene.idle(50);
        var alloy = new ItemStack(AllItems.ANDESITE_ALLOY);
        scene.world().createItemOnBeltLike(depotPos, Direction.NORTH, alloy);
        var depotCenter = util.vector().centerOf(depotPos.south());
        scene.overlay().showControls(depotCenter, Pointing.UP, 30).withItem(alloy);
        scene.idle(10);
        var type = ElectromagnetBlockEntity.class;
        scene.world().modifyBlockEntity(magnetPos, type, pte -> pte.getMagnetizingBehaviour()
                .start(MagnetizingBehaviour.Mode.BELT, util.vector().of(2.5f, 1.8125f, 2.5f)));
        int processingTime = 50;
        scene.idle(processingTime);
        scene.world().removeItemsFromBelt(depotPos);
        var magnetStack = ModdedItems.MAGNET.asStack();
        scene.world().createItemOnBeltLike(depotPos, Direction.UP, magnetStack);
        scene.idle(10);
        scene.overlay().showControls(depotCenter, Pointing.UP, 50).withItem(magnetStack);
        scene.idle(60);

        scene.world().hideIndependentSection(depot, Direction.NORTH);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 3, 0, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(4, 1, 2, 0, 2, 2), Direction.SOUTH);
        scene.idle(20);
        var beltPos = util.grid().at(0, 1, 2);
        scene.overlay().showText(40)
                .pointAt(util.vector().blockSurface(beltPos, Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                .text("When items are provided on a belt...");
        scene.idle(30);

        var ingot = scene.world().createItemOnBelt(beltPos, Direction.SOUTH, alloy);
        scene.idle(15);
        var ingot2 = scene.world().createItemOnBelt(beltPos, Direction.SOUTH, alloy);
        scene.idle(15);
        scene.world().stallBeltItem(ingot, true);
        scene.world().modifyBlockEntity(magnetPos, type, pte -> pte.getMagnetizingBehaviour()
                .start(MagnetizingBehaviour.Mode.BELT, util.vector().of(2.5f, 1.8125f, 2.5f)));

        scene.overlay().showText(50)
                .pointAt(pressSide)
                .placeNearTarget()
                .attachKeyFrame()
                .text("The Electromagnet will hold and process them automatically");

        scene.idle(processingTime);
        scene.world().removeItemsFromBelt(magnetPos.below(2));
        ingot = scene.world().createItemOnBelt(magnetPos.below(2), Direction.UP, magnetStack);
        scene.world().stallBeltItem(ingot, true);
        scene.idle(15);
        scene.world().stallBeltItem(ingot, false);
        scene.idle(15);
        scene.world().stallBeltItem(ingot2, true);
        scene.world().modifyBlockEntity(magnetPos, type, pte -> pte.getMagnetizingBehaviour()
                .start(MagnetizingBehaviour.Mode.BELT, util.vector().of(2.5f, 1.8125f, 2.5f)));
        scene.idle(processingTime);
        scene.world().removeItemsFromBelt(magnetPos.below(2));
        ingot2 = scene.world().createItemOnBelt(magnetPos.below(2), Direction.UP, magnetStack);
        scene.world().stallBeltItem(ingot2, true);
        scene.idle(15);
        scene.world().stallBeltItem(ingot2, false);

        scene.markAsFinished();
    }

    public static void electricFan(SceneBuilder builder, SceneBuildingUtil util){
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("electric_fan", "Making Wind");
        scene.configureBasePlate(0, 0, 5);
        var fan = util.grid().at(3, 1, 2);
        var common = util.grid().at(4, 1, 3);
        var input = util.grid().at(4, 1, 1);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().fromTo(input, common), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(fan), Direction.DOWN);
        scene.idle(5);

        scene.electric().addSource(common, 0, 0);
        scene.electric().connect(common, 0, fan, 0);
        var wire = scene.electric().connect(input, 0, fan, 1);

        scene.overlay().showText(80)
                .text("The Electric Fan Moves Air Using Electricity")
                .pointAt(util.vector().of(3.5, 1.5, 2.5))
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(80);
        scene.rotateCameraY(60);

        scene.overlay().showText(80)
                .text("Direction and Speed Are Dependent on the Voltage Applied")
                .pointAt(util.vector().of(3.5, 1.5, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(10);
        scene.electric().addSource(input, 0, 40);
        scene.electric().tickFor(10);
        scene.idle(70);
        scene.electric().removeWire(wire);
        scene.electric().tickFor(10);

        var rheo = util.grid().at(1, 1, 2);
        var thermo = util.grid().at(1, 1, 1);
        scene.idle(20);
        scene.world().setBlock(rheo, ModdedBlocks.RHEOSTAT.getDefaultState(), true);
        scene.world().setBlock(thermo, ModdedBlocks.THERMOMETER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.SOUTH), true);
        scene.world().showSection(util.select().position(rheo), Direction.DOWN);
        scene.world().showSection(util.select().position(thermo), Direction.DOWN);
        scene.electric().addSource(rheo, 1, 35);
        scene.electric().addSource(rheo, 0, 0);
        scene.electric().tickFor(30);
        scene.addKeyframe();
        scene.idle(50);
        scene.overlay().showText(80)
                .text("Fans Can Blow Air Over Hot Components to Cool Them Down")
                .attachKeyFrame();
        scene.idle(30);
        scene.electric().connect(input, 0, fan, 1);
        scene.idle(60);
        scene.markAsFinished();

    }

    public static void encasedFan(SceneBuilder builder, SceneBuildingUtil util){
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("encased_fan", "Cooling Down");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        var fan1 = util.grid().at(3, 1, 3);
        var fan2 = util.grid().at(1, 1, 3);
        var rheo = util.grid().at(3, 1, 1);
        var thermo = util.grid().at(2, 1, 1);
        var circut = util.grid().at(1, 1, 1);
        var cog1 = util.grid().at(1, 1, 4);
        var cog2 = util.grid().at(3, 1, 4);

        scene.world().showSection(util.select().position(fan1), Direction.DOWN);
        scene.world().showSection(util.select().position(fan2), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().fromTo(cog1, cog2), Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(util.select().position(rheo), Direction.DOWN);
        scene.world().showSection(util.select().position(circut), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(thermo), Direction.EAST);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("Fans Can Be Used to Cool Off Hot Components")
                .pointAt(util.vector().of(3, 1, 2))
                .placeNearTarget()
                .attachKeyFrame();

        scene.electric().addSource(rheo, 0, 35);
        scene.electric().addSource(rheo, 1, 0);
        scene.electric().tickFor(20);
        scene.idle(40);
        scene.world().setKineticSpeed(util.select().fromTo(1, 1, 4, 3, 1, 3), -128);
        scene.world().setKineticSpeed(util.select().position(2, 1, 4), 128);
        scene.electric().tickFor(20);
        scene.idle(40);
        scene.overlay().showText(80)
                .text("Also Works on Circuit Boards")
                .pointAt(util.vector().of(1.25, 1.25, 1.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);
        scene.markAsFinished();
    }

    public static void ceilingTile(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("ceiling_tile", "A stylish ceiling");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        scene.idle(10);
        scene.world().showSection(util.select().position(2, 2, 2), Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(60)
                .text("The Ceiling Tile is a decorative block.")
                .pointAt(util.vector().of(2.5, 2.25, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showText(60)
                .text("You can add certain blocks to it to expand its functionality")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);

        scene.overlay().showControls(util.vector().of(2.5, 2.5, 2.5), Pointing.DOWN, 30)
                .withItem(ModdedBlocks.FACTORY_LIGHT.asStack())
                .rightClick();
        scene.idle(20);

        scene.world().setBlock(util.grid().at(2, 2, 2), ModdedBlocks.CEILING_TILE_LAMP.getDefaultState(), false);
        scene.idle(20);
        scene.effects().indicateSuccess(util.grid().at(2, 2, 2));
        scene.idle(30);

        scene.world().hideSection(util.select().position(2, 2, 2), Direction.UP);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The Ceiling Tile supports a variety of attachments")
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(20);
        scene.world().showSection(util.select().position(1, 2, 1), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(3, 2, 1), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(1, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(3, 2, 3), Direction.DOWN);
        scene.idle(30);

        scene.markAsFinished();
    }

    private static class DisChargeBattery extends TickingInstruction {
        private final BlockPos battery;
        private final int time;
        private final float targetEnergyLevel;

        private float energyPerTick;

        public DisChargeBattery(int time, float targetEnergyLevel, BlockPos battery) {
            super(false, time);
            this.time = time;
            this.targetEnergyLevel = targetEnergyLevel;
            this.battery = battery;
        }

        protected Optional<BatteryBlockEntity> getBattery(PonderScene scene) {
            var be = scene.getWorld().getBlockEntity(battery);
            if(be instanceof MultiBlockBatteryEntity mbe)
                return Optional.ofNullable(mbe.getControllerBE());
            if(be instanceof BatteryBlockEntity battery)
                return Optional.of(battery);
            return Optional.empty();
        }

        @Override
        protected void firstTick(PonderScene scene) {
            super.firstTick(scene);
            getBattery(scene).ifPresent(battery -> {
                var target = targetEnergyLevel * battery.getCapacity();
                energyPerTick = (float) ((target - battery.getEnergy()) / time);
            });
        }

        @Override
        public void tick(PonderScene scene) {
            super.tick(scene);
            if(!isComplete()) {
                getBattery(scene).ifPresent(battery ->
                    battery.setEnergy(battery.getEnergy() + energyPerTick));
            }
        }
    }

    public static void battery(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("battery", "Storing electricity");
        scene.configureBasePlate(0, 0, 5);

        var battery = util.grid().at(2, 1, 3);
        var meter = util.grid().at(2, 1, 1);
        var connector = util.grid().at(2, 2, 2);

        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(battery), Direction.DOWN);
        scene.world().showSection(util.select().position(battery.above()), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(meter), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(connector), Direction.SOUTH);
        scene.idle(10);
        scene.electric().connect(connector, 0, meter, 1);
        scene.electric().connect(connector, 1, meter, 0);
        scene.electric().tickFor(10);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Battery allows you to store electricity for later use")
                .pointAt(util.vector().topOf(battery.above()))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("As the battery discharges, its voltage and resistance will begin to change")
                .placeNearTarget()
                .attachKeyFrame();

        scene.idle(40);
        scene.electric().tickFor(30);
        scene.addInstruction(new DisChargeBattery(30, 0.0f, battery));
        scene.idle(50);

        scene.markAsFinished();
    }

    public static void plotter(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("plotter", "Plotting voltage");
        scene.configureBasePlate(1, 0, 5);

        var plotter = util.grid().at(3, 1, 2);
        var bulb = util.grid().at(3, 2, 4);
        var connector1 = util.grid().at(1, 2, 2);
        var connector2 = util.grid().at(5, 2, 2);

        scene.electric().addSource(connector2, 0, 0);
        var source = scene.electric().addSource(connector1, 0, 0);

        scene.world().setKineticSpeed(util.select().position(plotter), 0);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(plotter), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Plotter is a simple electromechanical device that measures voltage over time")
                .pointAt(util.vector().topOf(plotter))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().fromTo(0, 1, 3, 6, 1, 3), Direction.DOWN);
        scene.world().showSection(util.select().position(0, 0, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(6, 0, 2), Direction.DOWN);
        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().position(plotter), 64);

        scene.world().showSection(util.select().fromTo(connector1.below(), connector1), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(connector2.below(), connector2), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(bulb.below(), bulb), Direction.DOWN);
        scene.idle(10);

        scene.electric().connect(connector1, 0, plotter, 1);
        scene.electric().connect(connector2, 0, plotter, 0);
        scene.electric().connect(connector1, 0, bulb, 0);
        scene.electric().connect(connector2, 0, bulb, 1);
        scene.idle(20);

        scene.electric().setSource(source, 90);
        scene.electric().tickFor(10);
        scene.addKeyframe();
        scene.idle(40);

        scene.electric().setSource(source, 122);
        scene.electric().tickFor(20);
        scene.idle(40);

        scene.electric().setSource(source, -122);
        scene.electric().tickFor(20);
        scene.idle(40);

        scene.effects().indicateSuccess(plotter);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("The graph color can be changed by right clicking the plotter with a dye")
                .pointAt(util.vector().of(3.5, 1.9, 2.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);
        scene.overlay().showControls(util.vector().of(3.5, 1.9, 2.5), Pointing.UP, 30)
                .withItem(new ItemStack(Items.RED_DYE))
                .rightClick();
        scene.idle(20);
        scene.world().modifyBlockEntity(plotter, PlotterBlockEntity.class, be -> be.setColor(DyeColor.RED));
        scene.idle(40);

        scene.markAsFinished();
    }

    public static void crt(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("crt", "Cathode Ray Tube");
        scene.configureBasePlate(0, 0, 3);

        scene.scaleSceneView(3.0f);

        var crt = util.grid().at(1, 1, 1);

        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().position(crt), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Cathode Ray Tube is a complex device that needs a bit of setup to work")
                .pointAt(util.vector().of(1.5, 1.75, 1.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.rotateCameraY(-90);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("First, you need to provide 12 volts between the Heater and Cathode pins")
                .pointAt(CRTBlock.TERMINALS[1].getOrigin().add(1, 1, 1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(70);
        scene.electric().addSource(crt, 0, 0);
        scene.electric().addSource(crt, 1, 12);
        scene.electric().tickForever();
        scene.idle(20);

        scene.overlay().showText(70)
                .text("Next, connect about 1000 volts between the Anode and Cathode")
                .pointAt(CRTBlock.TERMINALS[3].getOrigin().add(1, 1, 1))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);
        scene.electric().addSource(crt, 3, 1000);
        scene.idle(20);

        scene.rotateCameraY(90);
        scene.idle(20);
        scene.effects().indicateSuccess(crt);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("You can change the position of the dot using the deflection coils")
                .pointAt(util.vector().of(1.25, 1.75, 1.5))
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(70);

        scene.electric().addSource(crt, 6, 0);
        var x = scene.electric().addSource(crt, 4, 0);
        var y = scene.electric().addSource(crt, 5, 0);

        scene.electric().setSource(x, t -> (float) Math.cos(t * 0.33) * 15f * Math.min(t * 0.1f, 1));
        scene.electric().setSource(y, t -> (float) Math.sin(t * 0.33) * 15f * Math.min(t * 0.1f, 1));
        scene.idle(80);

        scene.overlay().showText(80)
                .text("You can apply a negative voltage to the Grid to change brightness of the trace")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(50);
        scene.electric().addSource(crt, 2, -3.5f);
        scene.idle(120);

        scene.markAsFinished();
    }

    public static void punchCardReader(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("punch_card_reader", "Punch Cards");
        scene.configureBasePlate(0, 0, 5);

        var reader = util.grid().at(2, 1, 2);
        var bulb = util.grid().at(2, 2, 4);
        var conn1 = util.grid().at(0, 2, 3);
        var conn2 = util.grid().at(4, 2, 3);

        scene.world().setKineticSpeed(util.select().position(reader), 0);

        scene.world().showSection(util.select().fromTo(0, 0, 0, 4, 0, 4), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(reader), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 3, 4, 2, 4), Direction.DOWN);
        scene.idle(10);

        scene.electric().connect(conn1, 0, bulb, 0);
        scene.electric().connect(bulb, 1, reader, 1);
        scene.electric().connect(reader, 0, conn2, 0);
        scene.electric().addSource(conn1, 0, 125);
        scene.electric().addSource(conn2, 0, 0);
        scene.electric().tickForever();
        scene.idle(10);

        scene.overlay().showText(80)
                .text("The Punch Card Reader is a device that can be used to playback a sequence of signals.")
                .pointAt(util.vector().blockSurface(reader, Direction.NORTH))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.rotateCameraY(-90);
        scene.overlay().showText(80)
                .text("It will connect the common terminal to an output pin when a hole is read from the card.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);
        scene.rotateCameraY(90);
        scene.idle(20);

        scene.world().showSection(util.select().position(5, 0, 3), Direction.DOWN);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 5, 1, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().setKineticSpeed(util.select().position(reader), -128);

        scene.overlay().showText(80)
                .text("To work, the device needs a kinetic input. Rotation speed controls the read speed.")
                .pointAt(util.vector().of(3.5, 1.5, 2.5))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("Punch Cards can be inserted manually or automatically.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(60);

        var card = ModdedItems.PUNCH_CARD.asStack();
        card.getOrCreateTag().putByteArray("Data", new byte[] { 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0 });
        scene.overlay()
                .showControls(util.vector().topOf(reader), Pointing.DOWN, 30)
                .withItem(card).rightClick();

        scene.idle(20);
        scene.world().modifyBlockEntity(reader, PunchCardReaderBlockEntity.class,
                be -> be.insertCard(card, Direction.UP));

        scene.idle(100);
        scene.markAsFinished();
    }

    public static void solarPanel(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("solar_panel_conditions", "Solar Panel Basics");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        var panel1 = util.grid().at(1, 1, 2);
        var panel2 = util.grid().at(3, 1, 2);
        var panel3 =  util.grid().at(2, 2, 2);
        var bearing = util.grid().at(0, 1, 2);
        var leave1 = util.grid().at(3, 2, 2);
        var glass = util.grid().at(3, 1, 2);
        var water =  util.grid().at(2, 1, 2);
        var leave2 = util.grid().at(1, 1, 2);

        scene.idle(10);
        var rotatedPanel = scene.world().showIndependentSection(util.select().position(panel1), Direction.DOWN);
        scene.world().showSection(util.select().position(panel2), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(80)
                .text("The Solar Panel needs to point at the sun to make power.")
                .pointAt(util.vector().centerOf(panel2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(90);

        scene.idle(10);
        scene.overlay().showText(80)
                .text("The closer to directly pointing at the sun you get, the more power the panel will make.")
                .pointAt(util.vector().centerOf(panel1))
                .placeNearTarget();
        scene.idle(90);

        scene.world().showSection(util.select().position(bearing), Direction.DOWN);

        scene.overlay().showText(100)
                .text("For pointing with more accuracy you can use the Solar Panel Bearing refer to that ponder for more information.")
                .pointAt(util.vector().centerOf(bearing))
                .placeNearTarget();
        scene.idle(15);
        scene.world().rotateBearing(bearing, 360, 35 * 2);
        scene.world().rotateSection(rotatedPanel, 360, 0, 0, 35 * 2);
        scene.idle(95);
        scene.world().hideSection(util.select().position(bearing), Direction.UP);

        scene.overlay().showText(70)
                .text("The Solar Panel needs direct sunlight.")
                .pointAt(util.vector().centerOf(panel2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(util.select().position(leave1), Direction.DOWN);
        scene.overlay().showText(80)
                .text("If there are any obstacles in the way it will make substantially less power.")
                .pointAt(util.vector().centerOf(leave1))
                .placeNearTarget();
        scene.idle(90);
        scene.world().hideSection(util.select().position(leave1), Direction.UP);
        scene.world().hideSection(util.select().position(glass), Direction.UP);
        scene.world().hideSection(util.select().position(leave2), Direction.UP);
        scene.world().hideIndependentSection(rotatedPanel, Direction.UP);
        scene.idle(15);

        scene.world().replaceBlocks(util.select().position(glass), Blocks.GLASS.defaultBlockState(), false);
        scene.world().replaceBlocks(util.select().position(water), Blocks.WATER.defaultBlockState(), false);
        scene.world().replaceBlocks(util.select().position(leave2), Blocks.OAK_LEAVES.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(3, 1, 2, 1, 1, 2), Direction.DOWN);

        scene.idle(10);
        scene.overlay().showText(100)
                .text("There are some blocks that let light through like glass, water, leaves and a few other blocks.")
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(30)
                .text("25%%")
                .pointAt(util.vector().topOf(leave2))
                .placeNearTarget();
        scene.idle(30);

        scene.overlay().showText(30)
                .text("50%%")
                .pointAt(util.vector().topOf(water))
                .placeNearTarget();
        scene.idle(30);

        scene.overlay().showText(30)
                .text("75%%")
                .pointAt(util.vector().topOf(glass))
                .placeNearTarget();
        scene.idle(30);

        scene.world().hideSection(util.select().fromTo(3, 1, 2, 1, 1, 2), Direction.UP);
        scene.idle(15);
        scene.world().showSection(util.select().fromTo(1,1,1,3,1,3), Direction.DOWN);
        scene.world().replaceBlocks(util.select().fromTo(1,1,1,3,1,3), Blocks.SAND.defaultBlockState(), false);
        scene.world().showSection(util.select().position(3, 2, 1), Direction.DOWN);
        scene.world().replaceBlocks(util.select().position(3, 2, 1), Blocks.DEAD_BUSH.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(3,2,3,3,3,3), Direction.DOWN);
        scene.world().replaceBlocks(util.select().fromTo(3,2,3,3,3,3), Blocks.CACTUS.defaultBlockState(), false);

        scene.idle(20);

        scene.overlay().showText(80)
                .text("Biome placement matters, the hotter the Solar Panel gets the less efficient it is.")
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(90)
                .text("Placing the Solar Panel in the desert will get you noticeably less overall power than...")
                .pointAt(util.vector().centerOf(panel3));
        scene.idle(10);
        scene.world().showSection(util.select().position(panel3), Direction.DOWN);
        scene.world().setBlock(panel3, ModdedBlocks.SOLAR_PANEL.getDefaultState()
                .setValue(SolarPanelBlock.FACING, Direction.DOWN), false);
        scene.idle(90);

        scene.world().replaceBlocks(util.select().fromTo(1,1,1,3,1,3), Blocks.GRASS_BLOCK.defaultBlockState(), false);
        scene.world().setBlocks(util.select().position(3,2,3), Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), false);
        scene.world().setBlocks(util.select().position(3,3,3), Blocks.TALL_GRASS.defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), false);
        scene.world().replaceBlocks(util.select().position(3, 2, 1), Blocks.GRASS.defaultBlockState(), false);

        scene.overlay().showText(60)
                .text("Placing one in a plains biome.")
                .pointAt(util.vector().centerOf(panel3));
        scene.idle(70);

        scene.world().hideSection(util.select().fromTo(1, 1, 1, 3, 1, 3), Direction.UP);
        scene.world().hideSection(util.select().position(3, 2, 1), Direction.UP);
        scene.world().hideSection(util.select().position(3, 2, 3), Direction.UP);
        scene.world().hideSection(util.select().position(3, 3, 3), Direction.UP);
        scene.world().hideSection(util.select().position(2, 2, 2), Direction.UP);
        scene.idle(20);

        scene.world().replaceBlocks(util.select().fromTo(1, 1, 1, 3, 1, 3),
                ModdedBlocks.SOLAR_PANEL.getDefaultState().setValue(SolarPanelBlock.FACING, Direction.DOWN),
                false);
        // 2x2
        scene.electric().connectPanels(util.grid().at(1, 1, 1), util.grid().at(2, 1, 1));
        scene.electric().connectPanels(util.grid().at(1, 1, 1), util.grid().at(1, 1, 2));
        scene.electric().connectPanels(util.grid().at(1, 1, 1), util.grid().at(2, 1, 2));
        // 1x2
        scene.electric().connectPanels(util.grid().at(3, 1, 1), util.grid().at(3, 1, 2));
        // 3x1
        scene.electric().connectPanels(util.grid().at(1, 1, 3), util.grid().at(2, 1, 3));
        scene.electric().connectPanels(util.grid().at(1, 1, 3), util.grid().at(3, 1, 3));
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 1, 3), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("Solar panels placed next to each other can connect to form a multiblock.")
                .placeNearTarget()
                .pointAt(util.vector().of(2, 1.5, 2))
                .attachKeyFrame();
        scene.idle(90);

        var wrench = AllItems.WRENCH.asStack();
        scene.overlay().showControls(util.vector().of(2.5, 1.5, 2.9), Pointing.DOWN, 30)
                .rightClick()
                .withItem(wrench);
        scene.idle(10);
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(10);
        scene.electric().mergePanels(util.grid().at(1, 1, 1), util.grid().at(1, 1, 3));
        scene.idle(30);

        scene.overlay().showControls(util.vector().of(2.9, 1.5, 2.5), Pointing.DOWN, 30)
                .rightClick()
                .withItem(wrench);
        scene.idle(10);
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(10);
        scene.electric().mergePanels(util.grid().at(1, 1, 1), util.grid().at(3, 1, 1));
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(30);

        scene.overlay().showControls(util.vector().of(2.1, 1.5, 2.5), Pointing.DOWN, 30)
                .rightClick()
                .withItem(wrench);
        scene.idle(10);
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(10);
        scene.electric().splitPanels(util.grid().at(1, 1, 1), 1, Direction.EAST);
        scene.world().showSection(util.select().position(4, 1, 4), Direction.DOWN);
        scene.idle(30);

        scene.overlay().showText(80)
                .text("Next scene will show you how to start making power with Solar Panels.");
        scene.idle(80);
        scene.setNextUpEnabled(true);
        scene.markAsFinished();
    }

    public static void solarPanel2(SceneBuilder builder, SceneBuildingUtil util){
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("solar_panel_power_creation", "Solar Panel Power Generation");
        scene.configureBasePlate(0, 0, 5);

        var resistor = util.grid().at(2, 1, 1);
        var deviceConnector = util.grid().at(2, 1, 2);
        var powerGauge = util.grid().at(4, 1, 2);

        scene.showBasePlate();
        scene.idle(20);
        scene.world().replaceBlocks(util.select().position(deviceConnector), ModdedBlocks.SOLAR_PANEL.getDefaultState()
                .setValue(SolarPanelBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(util.select().position(deviceConnector), Direction.DOWN);
        scene.scaleSceneView(2f);

        scene.overlay().showText(100)
                .text("A Solar Panels output is based on an IV curve, Google \"Solar Panel IV curve\" for an image of a general IV curve.")
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(70)
                .text("There are a few terms you need to know.")
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(80)
                .text("Voc is the max voltage of the Solar Panel with no load on it.")
                .attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(100)
                .text("Isc is the short current of the Solar Panel aka the current you would get placing a wire between the positive and negative.")
                .attachKeyFrame();
        scene.idle(110);

        scene.overlay().showText(150)
                .text("Vmp and Imp are the points along the IV curve where the Solar Panel makes the most power, these points shift around depending on the environmental conditions of the Solar Panel.")
                .attachKeyFrame();
        scene.idle(160);

        scene.scaleSceneView(1f);
        scene.world().hideSection(util.select().position(deviceConnector), Direction.UP);
        scene.idle(15);
        scene.world().restoreBlocks(util.select().position(deviceConnector));
        scene.world().showSection(util.select().fromTo(0, 1, 0, 4, 1, 4), Direction.DOWN);
        scene.idle(15);
        scene.electric().addSource(deviceConnector, 0, 24);
        scene.electric().connect(deviceConnector, 1, powerGauge, 1);
        scene.electric().connectInvisible(deviceConnector, 0, powerGauge, 2);
        scene.electric().connect(powerGauge, 0, resistor, 0, 30);
        scene.electric().connect(resistor, 1, deviceConnector, 0);

        scene.overlay().showText(90)
                .text("A simple way to use a Solar Panel is by impedance matching it to your load.")
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(140)
                .pointAt(util.select().position(resistor).getCenter())
                .text("For example if a Solar Panels Vmp is 21.2V and Imp is 7.8A, Ohm's law dictates that the optimal load resistance would be 2.71 Ohms.")
                .attachKeyFrame();
        scene.idle(130);
        scene.effects().indicateSuccess(resistor);
        scene.electric().tickForever();
        scene.idle(20);

        scene.overlay().showText(90)
                .text("The further you get from that resistance the less overall power the panel will make.")
                .attachKeyFrame();
        scene.idle(80);

        scene.markAsFinished();
    }

    public static void solarPanelBearing(SceneBuilder builder, SceneBuildingUtil util){
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("solar_panel_bearing", "Solar Panel Bearing Basics");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        var bearing = util.grid().at(5, 2, 3);
        var panelNBlock = util.grid().at(4, 2, 3);
        var smallCog = util.grid().at(6, 1, 2);
        var largeCog = util.grid().at(6, 2, 3);
        var log = util.grid().at(5, 1, 3);

        scene.world().showSection(util.select().position(log), Direction.DOWN);
        scene.world().showSection(util.select().position(bearing), Direction.DOWN);
        ElementLink<WorldSectionElement> panelNBlockContraption =
                scene.world().showIndependentSection(util.select().position(panelNBlock), Direction.DOWN);
        scene.world().showSection(util.select().position(smallCog), Direction.DOWN);
        scene.world().showSection(util.select().position(largeCog), Direction.DOWN);
        ElementLink<WorldSectionElement> panelNBlockContraption2 = null;

        scene.overlay().showOutlineWithText(util.select().position(bearing.west()), 80)
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().blockSurface(panelNBlock, Direction.UP))
                .placeNearTarget()
                .attachKeyFrame()
                .text("The Solar Panel Bearing attaches to Solar Panels placed in front of it.");
        scene.idle(90);

        scene.world().rotateBearing(bearing, 360, 37 * 2);
        scene.world().rotateSection(panelNBlockContraption, 360, 0, 0, 37 * 2);
        scene.world().setKineticSpeed(util.select().position(largeCog), 16);
        scene.world().setKineticSpeed(util.select().position(smallCog), -32);
        Vec3 blockSurface = util.vector().blockSurface(bearing, Direction.UP);
        scene.overlay().showControls(blockSurface, Pointing.DOWN, 60).rightClick();

        scene.overlay().showText(70)
                .text("By clicking you can assemble the contraption.")
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(74);
        scene.world().setKineticSpeed(util.select().position(largeCog), 0);
        scene.world().setKineticSpeed(util.select().position(smallCog), 0);
        scene.idle(11);

        scene.world().hideIndependentSection(panelNBlockContraption, Direction.UP);
        scene.idle(15);
        panelNBlockContraption = scene.world().showIndependentSection(util.select().fromTo(4, 2,2, 4, 2, 4), Direction.DOWN);

        scene.overlay().showText(90)
                .text("You do not need to glue Solar Panels together as long as they are touching.")
                .pointAt(util.select().position(panelNBlock).getCenter())
                .attachKeyFrame();
        scene.idle(30);

        scene.world().rotateBearing(bearing, 360, 37 * 2);
        scene.world().rotateSection(panelNBlockContraption, 360, 0, 0, 37 * 2);
        scene.world().setKineticSpeed(util.select().position(largeCog), 16);
        scene.world().setKineticSpeed(util.select().position(smallCog), -32);
        scene.idle(74);
        scene.world().setKineticSpeed(util.select().position(largeCog), 0);
        scene.world().setKineticSpeed(util.select().position(smallCog), 0);
        scene.idle(11);
        scene.world().hideIndependentSection(panelNBlockContraption, Direction.UP);
        scene.idle(15);
        scene.world().replaceBlocks(util.select().position(panelNBlock), ModdedBlocks.CONDUCTIVE_CASING.getDefaultState(), false);
        panelNBlockContraption = scene.world().showIndependentSection(util.select().fromTo(4, 2, 3, 2, 2, 3), Direction.DOWN);
        panelNBlockContraption2 = scene.world().showIndependentSection(util.select().fromTo(4, 2, 2, 2, 2, 2)
                .add(util.select().fromTo(4 ,2, 4, 2, 2 ,4)), Direction.DOWN);

        scene.overlay().showText(140)
                .text("If you do not connect the Solar Panels directly to the face then you need to glue every block that isn't a Solar Panel and glue at least one Solar Panel.")
                .pointAt(util.select().position(panelNBlock.west()).getCenter())
                .attachKeyFrame();
        scene.idle(120);

        scene.overlay().showOutline(PonderPalette.GREEN, "glue", util.select().fromTo(4, 2, 3, 2, 2, 3), 70);
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(3, 2, 3)).add(0, .4f, 0), Pointing.DOWN, 60)
                .withItem(AllItems.SUPER_GLUE.asStack());
        scene.idle(50);

        scene.overlay().showText(70)
                .text("Gluing like this will only turn the center.")
                .pointAt(util.select().position(panelNBlock.west()).getCenter())
                .attachKeyFrame();
        scene.idle(50);

        scene.world().rotateBearing(bearing, 360, 37 * 2);
        scene.world().rotateSection(panelNBlockContraption, 360, 0, 0, 37 * 2);
        scene.world().setKineticSpeed(util.select().position(largeCog), 16);
        scene.world().setKineticSpeed(util.select().position(smallCog), -32);
        scene.idle(74);
        scene.world().setKineticSpeed(util.select().position(largeCog), 0);
        scene.world().setKineticSpeed(util.select().position(smallCog), 0);

        scene.overlay().showOutline(PonderPalette.GREEN, "glue", util.select().fromTo(4, 2, 3, 2, 2, 3)
                .add(util.select().fromTo(2, 2, 2, 2, 2, 4)
                .add(util.select().fromTo(4, 2, 3, 2, 2, 3))), 60);
        scene.overlay().showControls(util.vector().centerOf(util.grid().at(3, 2, 3)).add(0, .4f, 0), Pointing.DOWN, 60)
                .withItem(AllItems.SUPER_GLUE.asStack());

        scene.overlay().showText(70)
                .text("Gluing like this will rotate the center and the Solar Panels.")
                .pointAt(util.select().position(panelNBlock.west()).getCenter())
                .attachKeyFrame();
        scene.idle(60);

        scene.world().rotateBearing(bearing, 360, 37 * 2);
        scene.world().rotateSection(panelNBlockContraption, 360, 0, 0, 37 * 2);
        scene.world().rotateSection(panelNBlockContraption2, 360, 0, 0, 37 * 2);
        scene.world().setKineticSpeed(util.select().position(largeCog), 16);
        scene.world().setKineticSpeed(util.select().position(smallCog), -32);
        scene.idle(74);
        scene.world().setKineticSpeed(util.select().position(largeCog), 0);
        scene.world().setKineticSpeed(util.select().position(smallCog), 0);
        scene.idle(20);

        scene.overlay().showText(70)
                .text("When assembled the output of all connected Solar Panels is combined on the terminals on the bearing.")
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(80)
                .text("On the side of the block you can change how the power from the Solar Panels is output.")
                .attachKeyFrame();
        blockSurface = util.vector().blockSurface(bearing, Direction.NORTH);
        scene.overlay().showControls(blockSurface, Pointing.RIGHT, 90);
        scene.overlay().showFilterSlotInput(blockSurface, Direction.NORTH, 90);
        scene.idle(95);

        scene.overlay().showText(70)
                .text("The output is in (Strings in parallel * Panels in String).")
                .attachKeyFrame();
        scene.idle(80);

        scene.world().rotateSection(panelNBlockContraption, -35, 0, 0, 10);
        scene.world().rotateSection(panelNBlockContraption2, -35, 0, 0, 10);

        scene.overlay().showText(110)
                .text("For example this contraption has 6 Solar Panels attached it would give you 4 options (1x6, 2x3, 3x2, 6x1).")
                .pointAt(util.select().position(panelNBlock.west()).getCenter())
                .attachKeyFrame();
        scene.idle(120);

        scene.overlay().showText(120)
                .text("The first number shows how many strings you have in parallel, and the second number is how many Solar Panels you have in a string.")
                .attachKeyFrame();
        scene.idle(130);

        scene.overlay().showText(130)
                .text("In simplified terms the further the slider is to the left you get more voltage and less current and further to the right is more current less voltage.")
                .attachKeyFrame();
        scene.idle(140);

        scene.overlay().showText(130)
                .text("The max amount of panels in a string is 25, if your Solar Panel amount isn't divisible by 1-9 with a quotient of less than 26 it will not assemble.")
                .attachKeyFrame();
        scene.idle(140);

        scene.overlay().showText(70)
                .text("The fix is to add or remove Solar Panels.")
                .attachKeyFrame();
        scene.idle(80);

        scene.markAsFinished();
    }
    public static void factoryLight(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("factory_light", "Lighting up your warehouse 101");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-2);

        var poles = util.select().fromTo(1, 1, 4, 3, 5, 4)
        .add(util.select().fromTo(1,5,4,1,6, 2)
        .add(util.select().fromTo(3,5,4,3,6, 2)));
        var light = util.select().fromTo(1, 4, 2, 3, 4, 2).add(util.select().position(2, 5, 2));
        var connector1 = util.grid().at(3,6,2);
        var connector2 = util.grid().at(1,6,2);
        var device_connector = util.grid().at(2,5,2);
        scene.idle(10);
        scene.world().showSection(poles, Direction.DOWN);
        scene.idle(20);
        scene.world().showSection(light, Direction.UP);
        scene.electric().addSource(connector1, 0, 120);
        scene.electric().addSource(connector2, 0, 0);
        scene.idle(10);
        scene.electric().connect(connector1, 0, device_connector, 1);
        scene.idle(10);
        scene.electric().connect(connector2, 0, device_connector, 0);
        scene.idle(20);
        ItemStack bulb = new ItemStack(ModdedItems.LIGHT_BULB);
        Vec3 frontVec = util.vector().blockSurface(util.grid().at(1, 4, 3), Direction.WEST);
                //.add(-.125, 0, 0);

        scene.overlay().showControls(frontVec, Pointing.DOWN, 40).rightClick()
                .withItem(bulb);
        scene.overlay().showOutlineWithText(util.select().fromTo(1, 4, 2, 3, 4, 2), 80)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(1, 4, 2), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                        .text("Right-click to add any light bulb");
        scene.idle(90);
        scene.electric().tickFor(20);

        scene.markAsFinished();
    }
    public static void factoryLightTall(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("factory_light_tall", "Using the Factory Light for tall buildings");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.setSceneOffsetY(-3);
        scene.scaleSceneView(0.8f);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0,1,0,4, 8, 4), Direction.DOWN);
        scene.electric().addSource(util.grid().at(3, 8 , 4), 0, 120);
        scene.electric().addSource(util.grid().at(1, 8 , 4), 0, 0);
        scene.idle(10);
        scene.electric().connect(util.grid().at(3, 8 , 4), 0, util.grid().at(2, 8 , 3), 1);
        scene.idle(10);
        scene.electric().connect(util.grid().at(1, 8 , 4), 0, util.grid().at(2, 8 , 3), 0);
        scene.idle(20);
        scene.overlay().showOutlineWithText(util.select().position(2,7,3), 80)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(2, 7, 3), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                .text("A Factory light can provide a light level of 15 to the top face of a block up to 16 blocks below it");
        scene.idle(10);
        scene.overlay().showText(60)
                .placeNearTarget()
                .pointAt(util.vector().topOf(util.grid().at(2, 0,3)))
                .text("15");
        scene.idle(80);

        scene.markAsFinished();
    }
    public static void factoryLightConnect(SceneBuilder builder, SceneBuildingUtil util) {
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("factory_light_connect", "Connecting Factory Lights together");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        var left = util.select().fromTo(4, 1, 0, 4, 1, 2);
        var back = util.select().fromTo(2, 1, 4, 0, 1, 4);
        var both = util.select().fromTo(0, 1, 0, 2, 1, 2);
        scene.idle(10);
        scene.world().showSection(left, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutlineWithText(left, 60)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(4, 1, 2), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                .text("Factory Lights can be connected to adjacent ones like this");
        scene.idle(70);
        scene.world().showSection(back, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutlineWithText(back, 40)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(0, 1, 4), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                .text("Or like this");
        scene.idle(50);
        scene.world().showSection(both, Direction.DOWN);
        scene.idle(10);
        scene.overlay().showOutlineWithText(both, 40)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().blockSurface(util.grid().at(0, 1, 1), Direction.WEST))
                .placeNearTarget()
                .attachKeyFrame()
                .text("But not this");
        scene.idle(50);

        scene.markAsFinished();
    }

    public static void modularDisplay(SceneBuilder builder, SceneBuildingUtil util){
        var scene = new PowerGridSceneBuilder(builder);
        scene.title("modular_display", "Modular Display");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(1.75f);

        var pos = util.grid().at(3, 1, 4);
        var neg = util.grid().at(1, 1, 4);
        var reset = util.grid().at(0, 1, 3);
        var block = util.grid().at(2, 1, 2);


        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().position(pos), Direction.DOWN);
        scene.world().showSection(util.select().position(neg), Direction.DOWN);
        scene.world().showSection(util.select().position(block), Direction.DOWN);
        scene.world().showSection(util.select().position(reset), Direction.DOWN);
        scene.idle(30);

        scene.overlay().showText(70)
                .text("You can place modules in by clicking a display module on a empty slot")
                .pointAt(util.vector().of(2.25, 1.75, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(40);

        scene.world().modifyBlockEntity(block, ModularDisplayBlockEntity.class, be -> {
            be.modules[0] = new ZeroToNineNumberModule(0, false, DyeColor.WHITE);
        });
        scene.effects().indicateSuccess(block);
        scene.idle(40);

        scene.overlay().showText(60)
                .text("You can remove them by shift clicking")
                .pointAt(util.vector().of(2.25, 1.75, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.overlay().showControls(util.vector().of(2.25, 1.75, 2), Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.idle(40);

        scene.world().modifyBlockEntity(block, ModularDisplayBlockEntity.class, be -> {
            be.modules[0] = null;
        });

        scene.idle(20);

        scene.rotateCameraY(-135);
        scene.idle(20);

        scene.overlay().showText(110)
                .text("There is a case ground in the center and a positive and reset pin for every slot")
                .pointAt(util.vector().of(2.5, 1.75, 3))
                .placeNearTarget()
                .attachKeyFrame();

        scene.world().modifyBlockEntity(block, ModularDisplayBlockEntity.class, be -> {
            be.modules[0] = new ZeroToNineNumberModule(0, false, DyeColor.WHITE);
        });
        scene.idle(30);
        scene.electric().connect(neg, 0, block, 8, DyeColor.BLACK);
        scene.idle(30);
        scene.electric().connect(pos, 0, block, 0, DyeColor.RED);
        scene.idle(30);
        scene.electric().connect(reset, 0, block, 1, DyeColor.BLACK);
        scene.idle(30);

        scene.rotateCameraY(135);
        scene.idle(10);

        scene.overlay().showText(70)
                .text("See display module ponder for instructions on how to use the modules")
                .pointAt(util.vector().of(2.25, 1.75, 2))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(80);

        scene.overlay().showText(80)
                .text("You can click and hold on a slot that has a module to change what the module displays")
                .placeNearTarget()
                .attachKeyFrame();

        scene.overlay().showControls(util.vector().of(2.25, 1.75, 2), Pointing.DOWN, 20).rightClick();
        scene.idle(20);

        scene.world().modifyBlockEntity(block, ModularDisplayBlockEntity.class, be -> {
            be.modules[0] = new AlphabetLetterModule(0, false, DyeColor.WHITE);
        });
        scene.idle(70);

        scene.overlay().showText(70)
                .text("You can dye modules by clicking the slot with dye")
                .placeNearTarget()
                .attachKeyFrame();

        scene.overlay().showControls(util.vector().of(2.25, 1.75, 2), Pointing.DOWN, 20)
                .withItem(Items.RED_DYE.getDefaultInstance()).rightClick();
        scene.idle(20);

        scene.world().modifyBlockEntity(block, ModularDisplayBlockEntity.class, be -> {
            be.modules[0] = new AlphabetLetterModule(0, false, DyeColor.RED);
        });
        scene.idle(20);
        scene.markAsFinished();
    }

}
