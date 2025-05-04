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
package org.patryk3211.powergrid.electricity.wire;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class BlockWireRenderer extends EntityRenderer<BlockWireEntity> {
    public BlockWireRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BlockWireEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        var currentPos = Vec3d.ZERO;
        var buffer = vertexConsumers.getBuffer(ModdedRenderLayers.getDebugLines());

        for(var segment : entity.segments) {
            var normal = segment.direction.getVector();
            var newPos = currentPos.add(normal.getX() * segment.length, normal.getY() * segment.length, normal.getZ() * segment.length);
            debugLine(matrices, buffer, light, 0xFFFFFFFF, currentPos, newPos);
            currentPos = newPos;
        }
    }

    @Override
    public Identifier getTexture(BlockWireEntity entity) {
        return null;
    }

    public static void debugLine(MatrixStack ms, VertexConsumer buffer, int light, int color,
                                 Vec3d v1, Vec3d v2) {
        var matrix = ms.peek().getPositionMatrix();
        buffer.vertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z)
                .color(color)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z)
                .color(color)
                .light(light)
                .next();
    }
}
