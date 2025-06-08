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
import com.simibubi.create.foundation.ponder.instruction.EmitParticlesInstruction;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.light.bulb.LightBulb;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlock;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;

import java.util.Random;
import java.util.function.UnaryOperator;

public class DeviceScenes {
    public static void heatingCoilBasic(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("heating_coil_basic", "Warming up the atmosphere");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world.showSection(util.select.fromTo(4, 1, 1, 5, 1, 3), Direction.UP);
        scene.world.showSection(util.select.position(0, 1, 2), Direction.UP);
        scene.world.showSection(util.select.position(5, 1, 2), Direction.UP);
        scene.idle(5);

        var heatingCoil = util.grid.at(3, 1, 2);
        var voltageSource = util.grid.at(6, 2, 2);
        scene.world.showSection(util.select.position(4, 2, 1), Direction.DOWN);
        scene.world.showSection(util.select.position(4, 2, 3), Direction.DOWN);
        scene.world.showSection(util.select.position(heatingCoil), Direction.DOWN);
        scene.idle(10);

        electric.connect(util.grid.at(4, 2, 1), 0, heatingCoil, 0);
        electric.connect(util.grid.at(4, 2, 3), 0, heatingCoil, 1);
        electric.connectInvisible(util.grid.at(4, 2, 1), 0, voltageSource, 0);
        electric.connectInvisible(util.grid.at(4, 2, 3), 0, voltageSource, 1);
        scene.idle(5);

        scene.overlay.showText(60)
                .text("The heating coil can be used to heat up the passing Air Flow if enough power is applied to it")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(heatingCoil))
                .placeNearTarget();

        electric.setSource(voltageSource, 32);
        electric.tickFor(10);
        scene.idle(100);

        scene.overlay.showText(60)
                .text("By applying a bigger voltage the Air Flow can be used for bulk blasting")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(heatingCoil))
                .placeNearTarget();

        electric.setSource(voltageSource, 38);
        electric.tickFor(10);
        scene.idle(80);

        scene.markAsFinished();
        electric.unload();
    }

    public static void heatingCoilSpeed(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("heating_coil_speed", "Electrified bulk processing");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.world.showSection(util.select.fromTo(0, 1, 4, 3, 1, 5), Direction.UP);
        scene.world.showSection(util.select.fromTo(1, 1, 0, 3, 1, 0), Direction.UP);
        scene.world.showSection(util.select.position(2, 0, 5), Direction.UP);
        scene.idle(5);

        var heatingCoil = util.grid.at(1, 1, 3);
        var voltageSource = util.grid.at(2, 1, 6);
        scene.world.showSection(util.select.position(0, 2, 4), Direction.DOWN);
        scene.world.showSection(util.select.position(2, 2, 4), Direction.DOWN);
        scene.world.showSection(util.select.position(heatingCoil), Direction.DOWN);
        scene.idle(10);

        electric.connect(util.grid.at(0, 2, 4), 0, heatingCoil, 1);
        electric.connect(util.grid.at(2, 2, 4), 0, heatingCoil, 0);
        electric.connectInvisible(util.grid.at(0, 2, 4), 0, voltageSource, 0);
        electric.connectInvisible(util.grid.at(2, 2, 4), 0, voltageSource, 1);
        electric.setSource(voltageSource, 32);
        electric.tickFor(10);
        scene.idle(10);

        scene.world.setBlock(util.grid.at(3, 1, 3), Blocks.FIRE.getDefaultState(), false);
        scene.world.showSection(util.select.position(3, 1, 3), Direction.WEST);

        scene.overlay.showText(60)
                .text("The heating coil allows for faster bulk processing")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(heatingCoil))
                .placeNearTarget();
        scene.idle(20);

        var stack = new ItemStack(Items.BEEF);
        var cooked = new ItemStack(Items.COOKED_BEEF);

        var heaterEntity = scene.world.createItemEntity(util.vector.centerOf(1, 2, 1), util.vector.of(0, 0.1, 0), stack);
        var fireEntity = scene.world.createItemEntity(util.vector.centerOf(3, 2, 1), util.vector.of(0, 0.1, 0), stack);
        scene.idle(10);
        scene.world.modifyEntity(heaterEntity, e -> e.setVelocity(0, 0, -0.2f));
        scene.world.modifyEntity(fireEntity, e -> e.setVelocity(0, 0, -0.2f));

        var item1Vec = util.vector.blockSurface(util.grid.at(1, 1, 0), Direction.SOUTH).add(0, 0, 0.1);
        var item2Vec = util.vector.blockSurface(util.grid.at(3, 1, 0), Direction.SOUTH).add(0, 0, 0.1);

        scene.effects.emitParticles(item1Vec.add(0, 0.2f, 0), EmitParticlesInstruction.Emitter.simple(ParticleTypes.LARGE_SMOKE, Vec3d.ZERO), 1, 60);
        scene.effects.emitParticles(item2Vec.add(0, 0.2f, 0), EmitParticlesInstruction.Emitter.simple(ParticleTypes.LARGE_SMOKE, Vec3d.ZERO), 1, 100);

        scene.idle(60);
        scene.world.modifyEntity(heaterEntity, e -> ((ItemEntity) e).setStack(cooked));
        scene.overlay.showControls(new InputWindowElement(item1Vec, Pointing.DOWN).withItem(cooked), 20);
        scene.idle(40);
        scene.world.modifyEntity(fireEntity, e -> ((ItemEntity) e).setStack(cooked));
        scene.overlay.showControls(new InputWindowElement(item2Vec, Pointing.DOWN).withItem(cooked), 20);

        scene.idle(20);
        scene.markAsFinished();
        electric.unload();
    }

    public static void transformerSizes(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("transformer_sizes", "Transformers");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);

        var smallTr = util.grid.at(2, 1, 1);
        scene.world.showSection(util.select.position(smallTr), Direction.DOWN);
        scene.idle(20);

        scene.overlay.showControls(new InputWindowElement(util.vector.blockSurface(smallTr, Direction.NORTH), Pointing.RIGHT).withWrench(), 30);
        scene.idle(20);
        scene.world.setBlock(smallTr, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState().with(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X), false);
        scene.idle(20);

        var mediumTr = util.grid.at(2, 1, 3);
        scene.world.showSection(util.select.fromTo(mediumTr, mediumTr.west().up()), Direction.DOWN);
        scene.idle(20);

        scene.overlay.showControls(new InputWindowElement(util.vector.blockSurface(mediumTr.up(), Direction.NORTH), Pointing.RIGHT).withWrench(), 30);
        scene.idle(20);
        scene.world.setBlock(mediumTr, ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .with(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .with(TransformerMediumBlock.PART, 1), false);
        scene.world.setBlock(mediumTr.west(), ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .with(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .with(TransformerMediumBlock.PART, 0), false);
        scene.world.setBlock(mediumTr.up(), ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .with(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .with(TransformerMediumBlock.PART, 3), false);
        scene.world.setBlock(mediumTr.up().west(), ModdedBlocks.TRANSFORMER_MEDIUM.getDefaultState()
                .with(TransformerMediumBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .with(TransformerMediumBlock.PART, 2), false);
        scene.idle(20);

        scene.overlay.showText(80)
                .text("Transformers come in different sizes, each of them offering different power capabilities and winding capacities")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void transformerWinding(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("transformer_winding", "Winding a transformer");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(10);

        var tr = util.grid.at(2, 1, 2);
        scene.world.showSection(util.select.position(tr), Direction.DOWN);
        scene.idle(15);

        scene.overlay.showText(80)
                .text("To wind a transformer, first select the starting terminal for your winding")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        var stack = new ItemStack(ModdedItems.WIRE, 1);
        scene.overlay.showControls(new InputWindowElement(util.vector.of(2.8, 1.9, 2.0), Pointing.RIGHT).withItem(stack), 30);
        scene.idle(30);

        var side = util.vector.blockSurface(tr, Direction.NORTH);
        scene.overlay.showText(80)
                .text("Next, click on the transformer body and pick the number of turn you want to add")
                .attachKeyFrame()
                .pointAt(side)
                .placeNearTarget();
        scene.idle(50);
        scene.overlay.showControls(new InputWindowElement(side, Pointing.UP).withItem(stack), 30);
        scene.idle(40);

        scene.overlay.showText(80)
                .text("Lastly, select the end terminal for your winding")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay.showControls(new InputWindowElement(util.vector.of(2.2, 1.9, 2.0), Pointing.LEFT).withItem(stack), 30);
        scene.idle(20);
        scene.world.setBlock(tr, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                .with(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .with(TransformerSmallBlock.COILS, 1), false);
        scene.idle(10);

        scene.effects.indicateSuccess(tr);
        scene.idle(10);

        scene.overlay.showText(60)
                .text("Repeat for the secondary winding")
                .placeNearTarget();
        scene.idle(50);

        scene.world.setBlock(tr, ModdedBlocks.TRANSFORMER_SMALL.getDefaultState()
                .with(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                .with(TransformerSmallBlock.COILS, 2), false);
        scene.effects.indicateSuccess(tr);
        scene.idle(30);

        scene.overlay.showText(80)
                .text("Your transformer will now transform voltage with the ratio you wound")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    public static void light(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("light", "Lighting up the world with electricity");
        scene.configureBasePlate(0, 0, 5);

//        var source = util.grid.at(2, 1, 3);
//        electric.setSource(source, 60);
//        electric.connectInvisible(source, 0, util.grid.at(0, 2, 2), 0);
//        electric.connectInvisible(source, 1, util.grid.at(4, 2, 2), 0);

        var light = util.grid.at(2, 2, 2);
        scene.showBasePlate();
        scene.idle(5);

        scene.world.showSection(util.select.fromTo(0, 1, 2, 4, 1, 2), Direction.NORTH);
        scene.idle(5);

        scene.world.showSection(util.select.position(0, 2, 2), Direction.DOWN);
        scene.world.showSection(util.select.position(4, 2, 2), Direction.DOWN);
        scene.idle(5);

        scene.world.showSection(util.select.position(light), Direction.DOWN);
        scene.idle(5);

        electric.connect(util.grid.at(0, 2, 2), 0, light, 0);
        electric.connect(util.grid.at(4, 2, 2), 0, light, 1);
        scene.idle(15);

        scene.overlay.showText(80)
                .text("When light bulbs are inserted into fixtures and powered on they turn electricity into light")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(light).subtract(0, 0.5, 0))
                .placeNearTarget();
        scene.idle(40);
        scene.world.modifyBlock(light, state -> state.with(LightFixtureBlock.POWER, 2), false);
        scene.idle(70);

        scene.overlay.showText(80)
                .text("If the voltage is not high enough, the light bulb will be dimmer")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(light).subtract(0, 0.5, 0))
                .placeNearTarget();
        scene.idle(40);
        scene.world.modifyBlock(light, state -> state.with(LightFixtureBlock.POWER, 1), false);
        scene.idle(50);

        scene.markAsFinished();
        electric.unload();
    }

    public static void growthLamp(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("growth_lamp", "Accelerating crop growth");
        scene.configureBasePlate(0, 0, 5);

        var light = util.grid.at(2, 2, 2);
        scene.showBasePlate();
        scene.world.showSection(util.select.fromTo(1, 1, 1, 3, 1, 2), Direction.UP);
        scene.idle(10);

        scene.world.showSection(util.select.fromTo(2, 1, 2, 2, 3, 4), Direction.NORTH);
        scene.idle(15);

        scene.overlay.showText(80)
                .text("The growth lamp is a special type of light bulb which accelerates crop growth in a certain area when powered on")
                .attachKeyFrame()
                .pointAt(util.vector.blockSurface(light, Direction.WEST))
                .placeNearTarget();
        scene.idle(40);
        scene.world.modifyBlock(light, state -> state.with(LightFixtureBlock.POWER, 2), false);
//        scene.idle(70);

        var crops = new BlockPos[] {
                util.grid.at(1, 1, 1),
                util.grid.at(2, 1, 1),
                util.grid.at(1, 1, 2),
                util.grid.at(3, 1, 2)
        };

        var random = new Random();
        UnaryOperator<BlockState> growCrop = state -> state.with(Properties.AGE_7, Math.min(state.get(Properties.AGE_7) + 1, 7));
        for(int i = 0; i < 15; ++i) {
            scene.world.modifyBlock(crops[random.nextInt(crops.length)], growCrop, false);
            scene.idle(10);
        }

        scene.markAsFinished();
    }

    public static void motor(SceneBuilder scene, SceneBuildingUtil util) {
        var electric = ElectricInstructions.of(scene);
        scene.title("electric_motor", "Turning electricity into rotation");
        scene.configureBasePlate(1, 0, 5);

        var source = util.grid.at(4, 1, 4);
        var motor = util.grid.at(4, 1, 2);
        var gauge = util.grid.at(5, 2, 2);
        electric.connectInvisible(source, 0, util.grid.at(5, 1, 1), 0);
        electric.connectInvisible(source, 1, util.grid.at(5, 1, 3), 0);

        scene.showBasePlate();
        scene.world.showSection(util.select.position(gauge.down()), Direction.UP);
        scene.idle(5);

        scene.world.showSection(util.select.position(gauge), Direction.DOWN);
        scene.world.showSection(util.select.position(5, 1, 1), Direction.DOWN);
        scene.world.showSection(util.select.position(5, 1, 3), Direction.DOWN);
        scene.idle(5);

        scene.world.showSection(util.select.position(0, 0, 3), Direction.EAST);
        scene.world.showSection(util.select.fromTo(0, 1, 2, 1, 1, 2), Direction.EAST);
        scene.world.showSection(util.select.fromTo(2, 1, 2, 3, 1, 2), Direction.DOWN);
        scene.idle(5);

        scene.world.showSection(util.select.position(motor), Direction.DOWN);
        scene.idle(5);

        electric.connect(util.grid.at(5, 1, 1), 0, gauge, 0);
        electric.connect(util.grid.at(5, 1, 1), 0, motor, 1);
        electric.connect(util.grid.at(5, 1, 3), 0, gauge, 1);
        electric.connect(util.grid.at(5, 1, 3), 0, motor, 0);
        electric.setSource(source, 50);
        electric.tickFor(10);

        scene.world.setKineticSpeed(util.select.fromTo(0, 1, 2, 4, 1, 2), 64);
        scene.world.setKineticSpeed(util.select.position(0, 0, 3), -32);
        scene.effects.rotationSpeedIndicator(motor.west());
        scene.idle(15);

        scene.overlay.showText(80)
                .text("The electric motor lets you convert electricity into rotation")
                .attachKeyFrame()
                .pointAt(util.vector.topOf(motor))
                .placeNearTarget();
        scene.idle(90);

        scene.overlay.showText(80)
                .text("The speed of the motor depends on the voltage you provide")
                .attachKeyFrame()
                .pointAt(util.vector.blockSurface(gauge, Direction.WEST))
                .placeNearTarget();
        scene.idle(40);
        electric.setSource(source, 100);
        electric.tickFor(10);
        scene.world.multiplyKineticSpeed(util.select.everywhere(), 2.0f);
        scene.effects.rotationSpeedIndicator(motor.west());
        scene.idle(50);

        scene.markAsFinished();
        electric.unload();
    }
}
