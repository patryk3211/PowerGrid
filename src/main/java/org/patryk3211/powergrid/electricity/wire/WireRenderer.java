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
import org.joml.Matrix4f;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class WireRenderer extends EntityRenderer<WireEntity> {
    private static final double SEGMENT_SIZE = 0.5;

    public static class RenderParameters {
        private final double a, b, c;
        private final Vec3d normal;
        private final float dx;
        private final float L;

        // Catenary parameter calculation implemented according to:
        // https://math.stackexchange.com/questions/3557767/how-to-construct-a-catenary-of-a-specified-length-through-two-specified-points
        public RenderParameters(Vec3d t1, Vec3d t2, double horizontalCoefficient, double verticalCoefficient, Vec3d entityPos) {
            var direction = new Vec3d(t2.x - t1.x, 0, t2.z - t1.z);
            double dy = t2.y - t1.y;
            dx = (float) direction.length();
            normal = direction.normalize();

            // Calculate total curve length using "material parameters"
            L = (float) Math.sqrt(dx * dx * horizontalCoefficient + dy * dy * verticalCoefficient);
            double r = Math.sqrt(L * L - dy * dy) / dx;

            double A;
            if(r < 3) A = Math.sqrt(6 * (r - 1));
            else A = Math.log(2 * r) + Math.log(Math.log(2 * r));

            // Solve using Newton's iteration
            for(int i = 0; i < 5; ++i) {
                var top = Math.sinh(A) - r * A;
                var bot = Math.cosh(A) - r;
                A = A - top / bot;
            }

            a = dx / (2 * A);
            double z = dy / L;
            b = -a * 0.5 * Math.log((1 + z) / (1 - z));
            double y1 = t1.y - entityPos.y;
            c = y1 - a * Math.cosh((-(dx / 2) - b) / a);
        }

        float apply(float x) {
            return (float) (a * Math.cosh((x - b) / a) + c);
        }

        void runForSegments(ISegmentConsumer consumer) {
            int segmentCount = (int) Math.round(L / SEGMENT_SIZE);

            float prevX = -dx / 2;
            float prevY = apply(prevX);
            for(int i = 1; i <= segmentCount; ++i) {
                float x = (((float) i / segmentCount) - 0.5f) * dx;
                float y = apply(x);

                consumer.apply(
                        (float) normal.x * prevX, prevY, (float) normal.z * prevX,
                        (float) normal.x * x, y, (float) normal.z * x
                );

                prevX = x;
                prevY = y;
            }
        }

        public interface ISegmentConsumer {
            void apply(float x1, float y1, float z1, float x2, float y2, float z2);
        }
    }

    public WireRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(WireEntity entity) {
        return null;
    }

    @Override
    public void render(WireEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if(entity.renderParams == null) {
            // TODO: Do something better about invalid entities (probably discard them, idk).
            var buffer = vertexConsumers.getBuffer(ModdedRenderLayers.getWireLayer());
            quad(matrices.peek().getPositionMatrix(), buffer, light,
                    -1, 0, 0, 1, 0, 0,
                    -1, 1, 0, 1, 1, 0);
            return;
        }

        var buffer = vertexConsumers.getBuffer(ModdedRenderLayers.getWireLayer());
        assert entity.renderParams instanceof RenderParameters;
        ((RenderParameters) entity.renderParams).runForSegments((x1, y1, z1, x2, y2, z2) ->
            renderSegment(matrices, buffer,
                    x1, y1, z1,
                    x2, y2, z2,
                    0.1, light));
    }

    public static void renderSegment(MatrixStack ms, VertexConsumer buffer,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
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
                .color(0xFFC26B4C)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2)
                .color(0xFFC26B4C)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3)
                .color(0xFFC26B4C)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2)
                .color(0xFFC26B4C)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4)
                .color(0xFFC26B4C)
                .light(light)
                .next();
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3)
                .color(0xFFC26B4C)
                .light(light)
                .next();
    }
}
