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
package org.patryk3211.powergrid.electricity.base;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

// TODO: Might have to reimplement wires as entities.
public class ElectricRenderer<T extends SmartBlockEntity> extends SafeBlockEntityRenderer<T> {
    public ElectricRenderer(BlockEntityRendererFactory.Context context) {

    }

    @Override
    protected void renderSafe(T be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        ms.push();
        try {
            var behaviour = be.getBehaviour(ElectricBehaviour.TYPE);
            if (behaviour == null)
                return;

            var buffer = bufferSource.getBuffer(ModdedRenderLayers.getWireLayer());

            var pos = be.getPos();
            ms.translate(-pos.getX(), -pos.getY(), -pos.getZ());
            var matrix = ms.peek().getPositionMatrix();

            final float thickness = 0.1f;

            var connections = behaviour.getConnections();
            for (var terminalConnections : connections) {
                for (var connection : terminalConnections) {
                    if (connection.renderParameters == null)
                        continue;

                    var params = connection.renderParameters;
                    var p1 = params.pos1();
                    var p2 = params.pos2();

                    renderSegment(ms, buffer, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, 0.1, light);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ms.pop();
    }

    public static void renderSegment(MatrixStack ms, VertexConsumer buffer,
                                     double x1, double y1, double z1, double x2, double y2, double z2,
                                     double thickness, int light) {
        var direction = new Vec3d(x2 - x1, y2 - y1, z2 - z1);
        var v1 = new Vec3d(1 - direction.x, 1 - direction.y, 1 - direction.z);

        var cross1 = v1.crossProduct(direction).normalize().multiply(thickness * 0.5);
        var cross2 = cross1.crossProduct(direction).normalize().multiply(thickness * 0.5);

        var matrix = ms.peek().getPositionMatrix();
        quad(matrix, buffer, light,
                x1 + cross1.x, y1 + cross1.y, z1 + cross1.z,
                x1 - cross1.x, y1 - cross1.y, z1 - cross1.z,
                x2 + cross1.x, y2 + cross1.y, z2 + cross1.z,
                x2 - cross1.x, y2 - cross1.y, z2 - cross1.z);
        quad(matrix, buffer, light,
                x1 + cross2.x, y1 + cross2.y, z1 + cross2.z,
                x1 - cross2.x, y1 - cross2.y, z1 - cross2.z,
                x2 + cross2.x, y2 + cross2.y, z2 + cross2.z,
                x2 - cross2.x, y2 - cross2.y, z2 - cross2.z);
    }

    public static void quad(Matrix4f matrix, VertexConsumer buffer, int light,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1)
                .color(0xFFFF0000)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2)
                .color(0xFFFF0000)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3)
                .color(0xFFFF0000)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2)
                .color(0xFFFF0000)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4)
                .color(0xFFFF0000)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3)
                .color(0xFFFF0000)
                .light(light)
                .next();
    }
}
