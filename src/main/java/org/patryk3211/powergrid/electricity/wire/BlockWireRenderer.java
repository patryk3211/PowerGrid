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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class BlockWireRenderer extends EntityRenderer<BlockWireEntity> {
    public BlockWireRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BlockWireEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        if(entity.isOverheated())
            return;

        var currentPos = Vec3.ZERO;
        var buffer = vertexConsumers.getBuffer(RenderType.entitySolid(getTextureLocation(entity)));
        var pos = entity.position();

        int color = entity.getColor() | 0xFF000000;

        boolean first = true;
        for(var segment : entity.segments) {
            var length = segment.length();
            if(first) {
                currentPos = Vec3.ZERO.relative(segment.direction, -1 / 16f);
                length += 1 / 16f;
                first = false;
            }
            var normal = segment.direction.getNormal();
            var newPos = currentPos.add(normal.getX() * length, normal.getY() * length, normal.getZ() * length);

            // TODO: Find a better way to calculate this
            int calcLight = 0;
            for(var dir : Direction.values()) {
                if(dir.getAxis() == segment.direction.getAxis())
                    continue;
                var n = dir.getNormal();
                var blockPos = BlockPos.containing(
                        newPos.x + pos.x + n.getX() / 16f,
                        newPos.y + pos.y + n.getY() / 16f,
                        newPos.z + pos.z + n.getZ() / 16f
                );
                var blockLight = entity.level().getBrightness(LightLayer.BLOCK, blockPos);
                var skyLight = entity.level().getBrightness(LightLayer.SKY, blockPos);
                var newLight = LightTexture.pack(blockLight, skyLight);
                if(newLight > calcLight)
                    calcLight = newLight;
            }

            renderSegment(matrices, buffer, calcLight, color, currentPos, segment.direction, entity.getWireItem().getWireThickness(), length, entity.getId());
            currentPos = newPos;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(BlockWireEntity entity) {
        return entity.getWireItem().getWireTexture();
    }

    public static void debugLine(PoseStack ms, VertexConsumer buffer, int light, int color,
                                 Vec3 v1, Vec3 v2) {
        var matrix = ms.last().pose();
        buffer.addVertex(matrix, (float) v1.x, (float) v1.y, (float) v1.z)
                .setColor(color)
                .setUv2(light & 65535, light >> 16 & 65535);
        buffer.addVertex(matrix, (float) v2.x, (float) v2.y, (float) v2.z)
                .setColor(color)
                .setUv2(light & 65535, light >> 16 & 65535);
    }

    private static void vertex(PoseStack.Pose matrix, VertexConsumer buffer,
                               double x1, double y1, double z1,
                               float u, float v,
                               float xn, float yn, float zn,
                               int color, int light) {
        buffer.addVertex(matrix.pose(), (float) x1, (float) y1, (float) z1)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(light & 65535, light >> 16 & 65535)
                .setNormal(matrix, xn, yn, zn);
    }

    public static void renderSegment(PoseStack ms, VertexConsumer buffer, int light, int color,
                                     Vec3 start, Direction dir, float thickness, float length, int uvOffset) {
        thickness *= 1.01f;
        if(dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            length *= -1;
            thickness *= -1;
        }

        uvOffset = uvOffset % 16;
        float u0 = uvOffset / 16f, v0 = uvOffset / 16f, t0 = uvOffset / 16f;
        float u1 = thickness + uvOffset / 16f, v1 = thickness + uvOffset / 16f, t1 = thickness + uvOffset / 16f;

        ms.pushPose();
        double x1 = start.x - thickness / 2;
        double y1 = start.y - thickness / 2;
        double z1 = start.z - thickness / 2;
        ms.translate(x1, y1, z1);
        x1 = y1 = z1 = 0;

        double x2 = 0, y2 = 0, z2 = 0;
        switch(dir.getAxis()) {
            case X -> {
                thickness *= 0.995;
                x1 += thickness;
                x2 = length - thickness;
                u0 = 0;
                u1 = Math.abs(length);
            }
            case Y -> {
                y1 += thickness;
                y2 = length - thickness;
                v0 = 0;
                v1 = Math.abs(length);
            }
            case Z -> {
                thickness *= 1.005;
                z1 += thickness;
                z2 = length - thickness;
                t0 = 0;
                t1 = Math.abs(length);
            }
        }

        x2 += x1 + thickness;
        y2 += y1 + thickness;
        z2 += z1 + thickness;

        if(dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            double xb = x1, yb = y1, zb = z1;
            x1 = x2; y1 = y2; z1 = z2;
            x2 = xb; y2 = yb; z2 = zb;
        }

        var matrix = ms.last();

        // Bottom face
        vertex(matrix, buffer,
                x1, y1, z1,
                t1, u0,
                0, -1, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y1, z1,
                t1, u1,
                0, -1, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y1, z2,
                t0, u1,
                0, -1, 0,
                color, light);
        vertex(matrix, buffer,
                x1, y1, z2,
                t0, u0,
                0, -1, 0,
                color, light);

        // Top face
        vertex(matrix, buffer,
                x1, y2, z1,
                t0, u1,
                0, 1, 0,
                color, light);
        vertex(matrix, buffer,
                x1, y2, z2,
                t1, u1,
                0, 1, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y2, z2,
                t1, u0,
                0, 1, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y2, z1,
                t0, u0,
                0, 1, 0,
                color, light);

        // West face
        vertex(matrix, buffer,
                x1, y1, z1,
                t0, v1,
                -1, 0, 0,
                color, light);
        vertex(matrix, buffer,
                x1, y1, z2,
                t1, v1,
                -1, 0, 0,
                color, light);
        vertex(matrix, buffer,
                x1, y2, z2,
                t1, v0,
                -1, 0, 0,
                color, light);
        vertex(matrix, buffer,
                x1, y2, z1,
                t0, v0,
                -1, 0, 0,
                color, light);

        // East face
        vertex(matrix, buffer,
                x2, y1, z1,
                t1, v0,
                1, 0, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y2, z1,
                t1, v1,
                1, 0, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y2, z2,
                t0, v1,
                1, 0, 0,
                color, light);
        vertex(matrix, buffer,
                x2, y1, z2,
                t0, v0,
                1, 0, 0,
                color, light);

        // North face
        vertex(matrix, buffer,
                x1, y1, z1,
                u0, v0,
                0, 0, -1,
                color, light);
        vertex(matrix, buffer,
                x1, y2, z1,
                u0, v1,
                0, 0, -1,
                color, light);
        vertex(matrix, buffer,
                x2, y2, z1,
                u1, v1,
                0, 0, -1,
                color, light);
        vertex(matrix, buffer,
                x2, y1, z1,
                u1, v0,
                0, 0, -1,
                color, light);

        // South face
        vertex(matrix, buffer,
                x1, y1, z2,
                u0, v0,
                0, 0, 1,
                color, light);
        vertex(matrix, buffer,
                x2, y1, z2,
                u1, v0,
                0, 0, 1,
                color, light);
        vertex(matrix, buffer,
                x2, y2, z2,
                u1, v1,
                0, 0, 1,
                color, light);
        vertex(matrix, buffer,
                x1, y2, z2,
                u0, v1,
                0, 0, 1,
                color, light);
        ms.popPose();
    }
}
