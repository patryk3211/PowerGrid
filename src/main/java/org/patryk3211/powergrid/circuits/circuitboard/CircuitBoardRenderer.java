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
package org.patryk3211.powergrid.circuits.circuitboard;

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;

@Environment(EnvType.CLIENT)
public class CircuitBoardRenderer extends SafeBlockEntityRenderer<CircuitBoardBlockEntity> {
    public CircuitBoardRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    protected void renderSafe(CircuitBoardBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        var components = be.getComponents(IRenderedComponent.class);
        if(components.isEmpty())
            return;

        for(var placed : components) {
            var rendered = (IRenderedComponent) placed.component;
            ms.push();
            ms.translate(placed.x / 16f, 2 / 16f, placed.y / 16f);
            rendered.render(be, placed, partialTicks, ms, bufferSource, light, overlay);
            ms.pop();
        }
    }
}
