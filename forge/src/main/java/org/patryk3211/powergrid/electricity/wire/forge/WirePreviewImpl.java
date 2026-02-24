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
package org.patryk3211.powergrid.electricity.wire.forge;

import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItemRenderer;

public class WirePreviewImpl {
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            var matrixStack = event.getPoseStack();
            matrixStack.pushPose();

            var cameraPos = event.getCamera().getPosition();
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            var buffer = DefaultSuperRenderTypeBuffer.getInstance();
            var player = Minecraft.getInstance().player;

            var world = Minecraft.getInstance().level;
            var target = Minecraft.getInstance().hitResult;
            if (player != null && target != null) {
                WirePreview.render(buffer, matrixStack, world, player, target);
                MultimeterItemRenderer.render(buffer, matrixStack, world, player);
            }

            buffer.draw();
            matrixStack.popPose();
        }
    }
}
