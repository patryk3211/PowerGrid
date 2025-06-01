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
import net.minecraft.world.LightType;

@Environment(EnvType.CLIENT)
public class HangingWireRenderer extends EntityRenderer<HangingWireEntity> {
    public static final double SEGMENT_SIZE = 0.5;

    public HangingWireRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(HangingWireEntity entity) {
        return entity.getWireItem().getWireTexture();
    }

    @Override
    public void render(HangingWireEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if(entity.renderParams == null)
            return;

        if(entity.isOverheated())
            // Don't render since it's dead and only there to spawn particles.
            return;

        var buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(entity)));
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
