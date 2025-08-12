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
import net.minecraft.client.MinecraftClient;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.patryk3211.powergrid.electricity.wire.WirePreview;

public class WirePreviewImpl {
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            var matrixStack = event.getPoseStack();
            matrixStack.push();

            var cameraPos = event.getCamera().getPos();
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            var buffer = DefaultSuperRenderTypeBuffer.getInstance();
            var player = MinecraftClient.getInstance().player;

            var world = MinecraftClient.getInstance().world;
            var target = MinecraftClient.getInstance().crosshairTarget;
            if (player != null && target != null) {
                WirePreview.render(buffer, matrixStack, world, player, target);
            }

            buffer.draw();
            matrixStack.pop();
        }
    }
}
