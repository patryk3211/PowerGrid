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

import com.simibubi.create.foundation.ponder.ElementLink;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.element.EntityElement;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.ponder.instruction.EmitParticlesInstruction;
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;

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
}
