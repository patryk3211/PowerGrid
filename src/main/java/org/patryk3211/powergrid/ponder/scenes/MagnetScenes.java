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
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.collections.ModdedItems;

public class MagnetScenes {
    public static void magnet(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("thunder_magnet", "Natural magnets");
        scene.configureBasePlate(0, 0, 5);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        scene.idle(10);

        var stack = new ItemStack(Items.IRON_INGOT);
        var item1 = scene.world().createItemEntity(util.vector().of(3.2f, 2.0f, 2.0f), Vec3d.ZERO, stack);
        var item2 = scene.world().createItemEntity(util.vector().of(1.5f, 2.0f, 4.3f), Vec3d.ZERO, stack);
        var item3 = scene.world().createItemEntity(util.vector().of(2.9f, 2.0f, 3.1f), Vec3d.ZERO, stack);
        var item4 = scene.world().createItemEntity(util.vector().of(1.2f, 2.0f, 1.1f), Vec3d.ZERO, stack);

        scene.overlay().showText(80)
                .text("When lightning strikes near an iron ingot it has a chance to magnetize it")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(30);

        scene.world().createEntity(world -> {
            var entity = EntityType.LIGHTNING_BOLT.create(world);
            if(entity != null) {
                entity.refreshPositionAfterTeleport(util.vector().of(2.5, 1.0, 2.5));
                entity.setCosmetic(false);
            }
            return entity;
        });

        var magnetStack = new ItemStack(ModdedItems.MAGNET);
        scene.world().modifyEntity(item1, entity -> {
            ((ItemEntity) entity).setStack(magnetStack);
            entity.setVelocity(0, 0.2, 0);
        });
        scene.idle(30);

        scene.overlay().showControls(new InputWindowElement(util.vector().of(3.2f, 1.3f, 2.0f), Pointing.RIGHT)
                .withItem(magnetStack), 50);
        scene.idle(60);

        scene.markAsFinished();
    }

    public static void lightningAttractor(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("lightning_attractor", "Artificial lightning");
        scene.configureBasePlate(0, 0, 7);

        scene.showBasePlate();
        scene.idle(5);

        scene.world().showSection(util.select().position(3, 1, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 2, 3, 5, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(1, 3, 3, 5, 3, 3), Direction.DOWN);
        scene.idle(10);

        scene.overlay().showText(80)
                .text("You can build a lightning attractor using lightning rods and wool placed on a Mechanical Bearing")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.overlay().showText(80)
                .text("When you spin it fast enough during a thunderstorm it will make lightning strikes more frequent")
                .attachKeyFrame()
                .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }
}
