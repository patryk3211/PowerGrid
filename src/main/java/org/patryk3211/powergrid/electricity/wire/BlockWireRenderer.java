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

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.patryk3211.powergrid.PowerGrid;

public class BlockWireRenderer extends EntityRenderer<BlockWireEntity> {
    public static final Identifier TEXTURE = new Identifier(PowerGrid.MOD_ID, "textures/special/wire.png");

    public BlockWireRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(BlockWireEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        var currentPos = Vec3d.ZERO;
        var buffer = vertexConsumers.getBuffer(RenderLayer.getEntitySolid(TEXTURE));
        var pos = entity.getPos();

        try {
            for (var segment : entity.segments) {
                var normal = segment.direction.getVector();
                var newPos = currentPos.add(normal.getX() * segment.length, normal.getY() * segment.length, normal.getZ() * segment.length);

                var blockPos = BlockPos.ofFloored(
                        newPos.x + pos.x,
                        newPos.y + pos.y,
                        newPos.z + pos.z
                );
                var blockLight = entity.getWorld().getLightLevel(LightType.BLOCK, blockPos);
                var skyLight = entity.getWorld().getLightLevel(LightType.SKY, blockPos);

                renderSegment(matrices, buffer, LightmapTextureManager.pack(blockLight, skyLight), 0xFFFFFFFF, currentPos, segment.direction, 1.0f / 16, segment.length, entity.getId());
                currentPos = newPos;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Identifier getTexture(BlockWireEntity entity) {
        return TEXTURE;
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

    private static void vertex(MatrixStack.Entry matrix, VertexConsumer buffer,
                               float x1, float y1, float z1,
                               float u, float v,
                               float xn, float yn, float zn,
                               int color, int light) {
        buffer.vertex(matrix.getPositionMatrix(), x1, y1, z1)
                .color(color)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(matrix.getNormalMatrix(), xn, yn, zn)
                .next();
    }

    public static void renderSegment(MatrixStack ms, VertexConsumer buffer, int light, int color,
                                     Vec3d start, Direction dir, float thickness, float length, int uvOffset) {
        if(dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
            length *= -1;
            thickness *= -1;
        }

        uvOffset = uvOffset % 16;
        float u0 = uvOffset / 16f, v0 = uvOffset / 16f, t0 = uvOffset / 16f;
        float u1 = thickness + uvOffset / 16f, v1 = thickness + uvOffset / 16f, t1 = thickness + uvOffset / 16f;

        float x2 = 0, y2 = 0, z2 = 0;
        switch(dir.getAxis()) {
            case X -> {
                thickness *= 0.995f;
                x2 = length;
                u0 = 0;
                u1 = Math.abs(length);
            }
            case Y -> {
                y2 = length;
                v0 = 0;
                v1 = Math.abs(length);
            }
            case Z -> {
                thickness *= 1.005f;
                z2 = length;
                t0 = 0;
                t1 = Math.abs(length);
            }
        }

        float x1 = (float) start.x - thickness / 2;
        float y1 = (float) start.y - thickness / 2;
        float z1 = (float) start.z - thickness / 2;

        x2 += x1 + thickness;
        y2 += y1 + thickness;
        z2 += z1 + thickness;
//        float x2 = x1 + thickness, y2 = y1 + thickness, z2 = z1 + thickness;

        if(dir.getDirection() == Direction.AxisDirection.NEGATIVE) {
            float xb = x1, yb = y1, zb = z1;
            x1 = x2; y1 = y2; z1 = z2;
            x2 = xb; y2 = yb; z2 = zb;
        }

        var matrix = ms.peek();

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
    }
}
