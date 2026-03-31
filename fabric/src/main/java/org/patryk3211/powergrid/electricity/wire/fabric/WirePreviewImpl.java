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
package org.patryk3211.powergrid.electricity.wire.fabric;

import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import org.patryk3211.powergrid.electricity.wire.WirePreview;

public class WirePreviewImpl {
    public static void init() {
        WorldRenderEvents.BEFORE_ENTITIES.register(WirePreviewImpl::render);
    }

    private static void render(WorldRenderContext context) {
        var matrixStack = context.matrixStack();
        matrixStack.pushPose();

        var partialTicks = context.tickDelta();
        var cameraPos = context.camera().getPosition();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        var buffer = DefaultSuperRenderTypeBuffer.getInstance();
        var player = Minecraft.getInstance().player;

        var world = context.world();
        var target = Minecraft.getInstance().hitResult;
        if(player != null && target != null) {
            WirePreview.render(buffer, matrixStack, world, player, target, cameraPos);
        }

        buffer.draw();
        matrixStack.popPose();
    }

}
