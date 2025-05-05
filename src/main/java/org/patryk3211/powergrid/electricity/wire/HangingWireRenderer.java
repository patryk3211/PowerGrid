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

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import org.patryk3211.powergrid.PowerGrid;

@Environment(EnvType.CLIENT)
public class HangingWireRenderer extends EntityRenderer<HangingWireEntity> {
    public static final Identifier TEXTURE = new Identifier(PowerGrid.MOD_ID, "textures/special/wire.png");

    private static final double SEGMENT_SIZE = 0.5;

    public static class CurveParameters {
        private final double a, b, c;
        private final Vec3d normal;
        private final float dx;
        private final float L;
        private final Vec3d cross1, cross2;
        private final float thickness;

        // Catenary parameter calculation implemented according to:
        // https://math.stackexchange.com/questions/3557767/how-to-construct-a-catenary-of-a-specified-length-through-two-specified-points
        public CurveParameters(Vec3d t1, Vec3d t2, double horizontalCoefficient, double verticalCoefficient, double thickness) {
            var direction = new Vec3d(t2.x - t1.x, 0, t2.z - t1.z);
            double dy = t2.y - t1.y;
            dx = (float) direction.length();
            normal = direction.normalize();
            this.thickness = (float) thickness;

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
            c = 0.5 * (dy - L / Math.tanh(A));

            // Calculate cross parameters
            direction = new Vec3d(t2.x - t1.x, t2.y - t1.y, t2.z - t1.z);
            Vec3d v1 = new Vec3d(1 - direction.x, 1 - direction.y, 1 - direction.z);
            cross1 = v1.crossProduct(direction).normalize().multiply(thickness * 0.5);
            cross2 = cross1.crossProduct(direction).normalize().multiply(thickness * 0.5);
        }

        public float apply(float x) {
            return (float) (a * Math.cosh((x - b) / a) + c);
        }

        public void runForSegments(ISegmentConsumer consumer) {
            int segmentCount = Math.max((int) Math.round(L / SEGMENT_SIZE), 5);

            float prevX = -dx / 2;
            float prevY = apply(prevX);
            float offset = 0;
            for(int i = 1; i <= segmentCount; ++i) {
                float x = (((float) i / segmentCount) - 0.5f) * dx;
                float y = apply(x);

                float dx = x - prevX;
                float dy = y - prevY;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                consumer.apply(
                        (float) normal.x * prevX, prevY, (float) normal.z * prevX,
                        (float) normal.x * x, y, (float) normal.z * x,
                        offset, length
                );

                offset += length;
                prevX = x;
                prevY = y;
            }
        }

        public Vec3d getRandomPoint(Random random) {
            float x = random.nextFloat() * dx - dx / 2;
            float y = apply(x);
            return new Vec3d(normal.x * x, y, normal.z * x);
        }

        /**
         * Get length of the span (-dx/2, dx) in which the curve is defined.
         * @return Curve span
         */
        public float getCurveSpan() {
            return dx;
        }

        public Vec3d getNormal() {
            return normal;
        }

        /**
         * Find closest x coordinate of the curve in relation to the given point.
         * @param x1 First point coordinate
         * @param y1 Second point coordinate
         * @return First coordinate of closest curve point (x, f(x))
         */
        public double findClosestPoint(double x1, double y1) {
            // f(x) = square distance between cosh(x) and a point (in 2D space)
            // f(x) = (x - P.x)^2 + (cosh(x) - P.y)^2
            // f'(x) = 2(cosh(x) - P.y) * sinh(x) + 2(x - P.x)
            // f''(x) = 4cosh^2(x) - 2cosh(x) * P.y
            // The functions for a curve defined by a, b and c are slightly different:
            // z = (x - b) / a
            // f'(x) = 2 * sinh(z) * (a * cosh(z) + c - P.y) + 2(x - P.x)
            // f''(x) = 4 * cosh^2(z) + (2 / a) * cosh(z) * (c - P.y)
            // Find x where value of f(x) is the smallest (smallest square distance)

            // Select initial guess for Newton's iteration.
            double x = x1;

            // Solve for f'(x) = 0
            for(int i = 0; i < 5; ++i) {
                // Apply Newton's iteration where x_{n+1} = x - f(x_n)/f'(x_n) (which means that we actually need the second derivative)
                double z = (x - b) / a;
                double xCosh = Math.cosh(z);
                double xSinh = Math.sinh(z);
                double fval = 2 * ((a * xCosh + c - y1) * xSinh + x - x1);
                double fdval = 4 * xCosh * xCosh + (2 / a) * xCosh * (c - y1);
                x = x - fval / fdval;
                // TODO: Use fval to see if solution is close to zero (instead of a fixed number of iterations)
            }

            // X should correspond to a point on the curve (x, f(x)),
            // where distance to point P(x1, y1) is the smallest.
            return x;
        }

        public interface ISegmentConsumer {
            void apply(float x1, float y1, float z1, float x2, float y2, float z2, float offset, float length);
        }
    }

    public HangingWireRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(HangingWireEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(HangingWireEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if(entity.renderParams == null)
            return;

        if(entity.isOverheated())
            // Don't render since it's dead and only there to spawn particles.
            return;

        var buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
        assert entity.renderParams instanceof CurveParameters;
        CurveParameters rp = (CurveParameters) entity.renderParams;

        // To introduce some subtle variety into the wires.
        var thicknessOffset = entity.getId() / 16f;

        var pos = entity.getPos();
        var world = entity.getWorld();
        rp.runForSegments((x1, y1, z1, x2, y2, z2, offset, length) -> {
            var blockPos = BlockPos.ofFloored((x1 + x2) * 0.5 + pos.x, (y1 + y2) * 0.5 + pos.y, (z1 + z2) * 0.5 + pos.z);
            var sky = world.getLightLevel(LightType.SKY, blockPos);
            var block = world.getLightLevel(LightType.BLOCK, blockPos);
            renderSegment(matrices, buffer,
                    x1, y1, z1,
                    x2, y2, z2,
                    rp.cross1, rp.cross2, LightmapTextureManager.pack(block, sky), -1,
                    rp.thickness, thicknessOffset, length, offset);
        });
    }

    public static void renderFromPositions(MatrixStack matrices, VertexConsumer buffer, Vec3d t1, Vec3d t2, double horizontalCoefficient, double verticalCoefficient, double thickness, int light, int color) {
        float x = (float) (t1.x + t2.x) * 0.5f;
        float y = (float) t1.y;
        float z = (float) (t1.z + t2.z) * 0.5f;
        var curve = new CurveParameters(t1, t2, horizontalCoefficient, verticalCoefficient, thickness);
        curve.runForSegments((x1, y1, z1, x2, y2, z2, offset, length) ->
                renderSegment(matrices, buffer,
                        x1 + x, y1 + y, z1 + z,
                        x2 + x, y2 + y, z2 + z,
                        curve.cross1, curve.cross2, light, color,
                        curve.thickness, 0, length, offset));
    }

    public static void renderSegment(MatrixStack ms, VertexConsumer buffer,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     Vec3d cross1, Vec3d cross2, int light, int color,
                                     float thickness, float thicknessOffset, float uvLength, float lengthOffset) {
        quad(ms.peek(), buffer, light, color,
                x1 + cross1.x, y1 + cross1.y, z1 + cross1.z,
                x1 - cross1.x, y1 - cross1.y, z1 - cross1.z,
                x2 + cross1.x, y2 + cross1.y, z2 + cross1.z,
                x2 - cross1.x, y2 - cross1.y, z2 - cross1.z,
                thickness, thicknessOffset, uvLength, lengthOffset);
        quad(ms.peek(), buffer, light, color,
                x1 + cross2.x, y1 + cross2.y, z1 + cross2.z,
                x1 - cross2.x, y1 - cross2.y, z1 - cross2.z,
                x2 + cross2.x, y2 + cross2.y, z2 + cross2.z,
                x2 - cross2.x, y2 - cross2.y, z2 - cross2.z,
                thickness, thicknessOffset, uvLength, lengthOffset);
    }

    public static void quad(MatrixStack.Entry matrix, VertexConsumer buffer, int light, int color,
                            double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4,
                            float thickness, float thicknessOffset, float uvLength, float lengthOffset) {
        buffer.vertex(matrix.getPositionMatrix(), (float) x1, (float) y1, (float) z1)
                .color(color)
                .texture(lengthOffset, thicknessOffset)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrix.getNormalMatrix(), 0, 1, 0)
                .next();
        buffer.vertex(matrix.getPositionMatrix(), (float) x2, (float) y2, (float) z2)
                .color(color)
                .texture(lengthOffset, thicknessOffset + thickness)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrix.getNormalMatrix(), 0, 1, 0)
                .next();
        buffer.vertex(matrix.getPositionMatrix(), (float) x4, (float) y4, (float) z4)
                .color(color)
                .texture(lengthOffset + uvLength, thicknessOffset + thickness)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrix.getNormalMatrix(), 0, 1, 0)
                .next();
        buffer.vertex(matrix.getPositionMatrix(), (float) x3, (float) y3, (float) z3)
                .color(color)
                .texture(lengthOffset + uvLength, thicknessOffset)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrix.getNormalMatrix(), 0, 1, 0)
                .next();
    }
}
