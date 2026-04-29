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
package org.patryk3211.powergrid.electricity.crt;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class CRTRenderer extends SafeBlockEntityRenderer<CRTBlockEntity> {
    private static final float CRT_SPAN = 7.5f / 16f;

    public static float zDepth() {
        return ModdedConfigs.client().crtZDepth.getF();
    }

    public CRTRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(CRTBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        var state = be.getBlockState();
        var color = be.getColor().getTextureDiffuseColors();
        var facing = state.getValue(CRTBlock.HORIZONTAL_FACING);

        var bg = CachedBuffers.partialFacing(ModdedPartialModels.CRT_BACKGROUND, state, facing.getOpposite());
        bg.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        final int R = (int) color[0] * 256,
                G = (int) color[1] * 256,
                B = (int) color[2] * 256;
        var consumer = buffer.getBuffer(ModdedRenderLayers.getAdditiveCrt());

        ms.pushPose();
        ms.rotateAround(new Quaternionf().rotateY(0.5f * (float) Math.PI * (2 - facing.get2DDataValue())), 0.5f, 0.5f, 0.5f);
        ms.translate(0.5f, 0.375f, 1 / 32f + zDepth() * 0.5f);
        ms.scale(CRT_SPAN * 0.5f, CRT_SPAN * 0.5f, 1);
        var m4 = ms.last().pose();
        float x1 = 0, y1 = 0, b1 = 0;
        final float size = ModdedConfigs.client().crtDotSize.getF();
        // Can't use sampleCount() since the entity may have been allocated with a different count.
        for(int i = 0; i < be.brightness.length - 1; ++i) {
            int i1 = (i + be.head + 1) % be.brightness.length;
            var x2 = be.xPoints[i1];
            var y2 = be.yPoints[i1];
            var b2 = be.brightness[i1] * (float) (1 - Math.exp(-i * ModdedConfigs.client().crtTracePersistence.getF()));

            var nx = x2 - x1;
            var ny = y2 - y1;

            var snx = Math.signum(nx);
            var sny = Math.signum(ny);

            if(i == 0 || (Math.abs(nx) < 1e-3f && Math.abs(ny) < 1e-3f)) {
                putPoint(consumer, m4, x2 - size, y2 - size, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 - size, y2 + size, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 + size, y2 + size, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 + size, y2 - size, R * b2, G * b2, B * b2, i);

                putPoint(consumer, m4, x2 - size, y2 - size, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 + size, y2 + size, R * b2, G * b2, B * b2, i);
            } else if(Math.abs(nx) < 1e-3f) {
                putPoint(consumer, m4, x2 - size, y1 + size * sny, R * b1, G * b1, B * b1, i - 1);
                putPoint(consumer, m4, x2 + size, y1 + size * sny, R * b1, G * b1, B * b1, i - 1);
                putPoint(consumer, m4, x2 + size, y2 + size * sny, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 - size, y2 + size * sny, R * b2, G * b2, B * b2, i);

                putPoint(consumer, m4, x2 + size, y2 + size * sny, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 - size, y1 + size * sny, R * b1, G * b1, B * b1, i - 1);
            } else if(Math.abs(ny) < 1e-3f) {
                putPoint(consumer, m4, x1 + size * snx, y2 - size, R * b1, G * b1, B * b1, i - 1);
                putPoint(consumer, m4, x1 + size * snx, y2 + size, R * b1, G * b1, B * b1, i - 1);
                putPoint(consumer, m4, x2 + size * snx, y2 + size, R * b2, G * b2, B * b2, i);
                putPoint(consumer, m4, x2 + size * snx, y2 - size, R * b2, G * b2, B * b2, i);

                putPoint(consumer, m4, x1 + size * snx, y2 - size, R * b1, G * b1, B * b1, i - 1);
                putPoint(consumer, m4, x2 + size * snx, y2 + size, R * b2, G * b2, B * b2, i);
            } else {
                var mainX1 = x1 + size * snx;
                var mainY1 = y1 + size * sny;
                var mainX2 = x2 - size * snx;
                var mainY2 = y2 - size * sny;

                var leftX1 = mainX1 - size * 2 * snx;
                var topY2 = mainY2 + size * 2 * sny;
                var bottomY1 = mainY1 - size * 2 * sny;
                var rightX2 = mainX2 + size * 2 * snx;

                var xInside = (mainX2 - mainX1) * snx < 0;
                var yInside = (mainY2 - mainY1) * sny < 0;

                if(xInside && yInside) {
                    // Only 4 triangles are needed
                    putPoint(consumer, m4, leftX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                    putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                    putPoint(consumer, m4, mainX2, topY2, R * b2, G * b2, B * b2, i);

                    putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                    putPoint(consumer, m4, mainX2, topY2, R * b2, G * b2, B * b2, i);
                    putPoint(consumer, m4, rightX2, topY2, R * b2, G * b2, B * b2, i);

                    putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                    putPoint(consumer, m4, rightX2, mainY2, R * b2, G * b2, B * b2, i);
                    putPoint(consumer, m4, rightX2, topY2, R * b2, G * b2, B * b2, i);

                    putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                    putPoint(consumer, m4, mainX1, bottomY1, R * b1, G * b1, B * b1, i - 1);
                    putPoint(consumer, m4, rightX2, mainY2, R * b2, G * b2, B * b2, i);
                } else {
                    // 3 quads must be drawn (1 dot at end and 2 connecting quads)
                    putPoint(consumer, m4, x2 - size, y2 - size, R * b2, G * b2, B * b2, i);
                    putPoint(consumer, m4, x2 - size, y2 + size, R * b2, G * b2, B * b2, i);
                    putPoint(consumer, m4, x2 + size, y2 + size, R * b2, G * b2, B * b2, i);
                    putPoint(consumer, m4, x2 + size, y2 - size, R * b2, G * b2, B * b2, i);

                    putPoint(consumer, m4, x2 - size, y2 - size, R * b2, G * b2, B * b2, i);
                    putPoint(consumer, m4, x2 + size, y2 + size, R * b2, G * b2, B * b2, i);

                    if (yInside) {
                        putPoint(consumer, m4, mainX2, topY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                        putPoint(consumer, m4, leftX1, mainY1, R * b1, G * b1, B * b1, i - 1);

                        putPoint(consumer, m4, mainX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                        putPoint(consumer, m4, mainX2, topY2, R * b2, G * b2, B * b2, i);
                    } else {
                        putPoint(consumer, m4, mainX2, topY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, leftX1, mainY1, R * b1, G * b1, B * b1, i - 1);

                        putPoint(consumer, m4, mainX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                        putPoint(consumer, m4, leftX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                    }
                    if (xInside) {
                        putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                        putPoint(consumer, m4, mainX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, rightX2, mainY2, R * b2, G * b2, B * b2, i);

                        putPoint(consumer, m4, rightX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                        putPoint(consumer, m4, mainX1, bottomY1, R * b1, G * b1, B * b1, i - 1);
                    } else {
                        putPoint(consumer, m4, mainX1, mainY1, R * b1, G * b1, B * b1, i - 1);
                        putPoint(consumer, m4, mainX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX1, bottomY1, R * b1, G * b1, B * b1, i - 1);

                        putPoint(consumer, m4, mainX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, rightX2, mainY2, R * b2, G * b2, B * b2, i);
                        putPoint(consumer, m4, mainX1, bottomY1, R * b1, G * b1, B * b1, i - 1);
                    }
                }
            }

            x1 = x2;
            y1 = y2;
            b1 = b2;
        }
        ms.popPose();
    }

    private static void putPoint(VertexConsumer consumer, Matrix4f m4, float x, float y, float r, float g, float b, int z) {
        consumer.vertex(m4, x, y, -z * zDepth())
                .color((int) r, (int) g, (int) b, 255)
                .endVertex();
    }
}
