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

import com.simibubi.create.foundation.ponder.PonderStoryBoardEntry;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.electricswitch.SurfaceSwitchBlock;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.ponder.base.ElectricInstructions;

public class RelayScenes {
    public static PonderStoryBoardEntry.PonderStoryBoard switchSceneFor(BlockEntry<? extends SurfaceSwitchBlock> block, String suffix) {
        return (scene, util) -> switchScene(scene, util, block.get(), suffix);
    }

    public static void switchScene(SceneBuilder scene, SceneBuildingUtil util, SurfaceSwitchBlock block, String suffix) {
        var electric = ElectricInstructions.of(scene);
        scene.title("switch_" + suffix, "Manually switching electricity");
        scene.configureBasePlate(0, 0, 5);

        var source = util.grid.at(2, 1, 3);
        var target = util.grid.at(1, 2, 2);
        var bulb = util.grid.at(3, 2, 2);
        scene.world.setBlock(target, block.getDefaultState()
                .with(Properties.FACING, Direction.DOWN)
                .with(CustomProperties.ALONG_FIRST_AXIS, false)
                .with(Properties.OPEN, true), false);

        scene.showBasePlate();
        scene.idle(10);

        scene.world.showSection(util.select.fromTo(0, 1, 2, 4, 1, 2), Direction.DOWN);
        scene.world.showSection(util.select.position(0, 2, 2), Direction.DOWN);
        scene.world.showSection(util.select.position(4, 2, 2), Direction.DOWN);
        scene.idle(10);

        scene.world.showSection(util.select.position(1, 2, 2), Direction.DOWN);
        scene.world.showSection(util.select.position(3, 2, 2), Direction.DOWN);
        scene.idle(10);

        electric.connectInvisible(util.grid.at(0, 2, 2), 0, source, 0);
        electric.connectInvisible(util.grid.at(4, 2, 2), 0, source, 1);
        electric.connect(util.grid.at(0, 2, 2), 0, target, 1);
        electric.connect(util.grid.at(4, 2, 2), 0, bulb, 0);
        electric.connect(target, 0, bulb, 1);
        scene.idle(10);

        scene.overlay.showText(80)
                .text("Switches and buttons allow you to manually toggle electricity")
                .pointAt(util.vector.centerOf(target))
                .placeNearTarget()
                .attachKeyFrame();
        scene.idle(50);

        scene.world.modifyBlock(target, state -> state.with(Properties.OPEN, false), false);
        scene.world.modifyBlock(bulb, state -> state.with(LightFixtureBlock.POWER, 2), false);
        scene.effects.indicateSuccess(target);
        if(block.isButton()) {
            scene.idle(10);
            scene.world.modifyBlock(target, state -> state.with(Properties.OPEN, true), false);
            scene.world.modifyBlock(bulb, state -> state.with(LightFixtureBlock.POWER, 0), false);
            scene.idle(30);
        } else {
            scene.idle(40);
        }

        scene.markAsFinished();
        electric.unload();
    }
}
