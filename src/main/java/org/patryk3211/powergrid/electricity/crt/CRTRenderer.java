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

import static org.patryk3211.powergrid.electricity.crt.CRTBlockEntity.SAMPLE_COUNT;

public class CRTRenderer extends SafeBlockEntityRenderer<CRTBlockEntity> {
    private static final float CRT_SPAN = 7.5f / 16f;

    public CRTRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(CRTBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        var state = be.getBlockState();
        var facing = state.getValue(CRTBlock.HORIZONTAL_FACING);

        var bg = CachedBuffers.partialFacing(ModdedPartialModels.CRT_BACKGROUND, state, facing.getOpposite());
        bg.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));

        final int R = ModdedConfigs.client().crtRed.get(),
                G = ModdedConfigs.client().crtGreen.get(),
                B = ModdedConfigs.client().crtBlue.get();
        var consumer = buffer.getBuffer(ModdedRenderLayers.getAdditiveColor());

        ms.pushPose();
        ms.rotateAround(new Quaternionf().rotateY(0.5f * (float) Math.PI * (2 - facing.get2DDataValue())), 0.5f, 0.5f, 0.5f);
        ms.translate(0.5f, 0.375f, 1 / 32f);
        ms.scale(CRT_SPAN * 0.5f, CRT_SPAN * 0.5f, 1);
        var m4 = ms.last().pose();
        float x1 = 0, y1 = 0, b1 = 0;
        for(int i = 0; i < SAMPLE_COUNT; ++i) {
            int i1 = (i + be.head + 1) % SAMPLE_COUNT;
            var x2 = be.xPoints[i1];
            var y2 = be.yPoints[i1];
            var b2 = be.brightness[i1] * (float) (1 - Math.exp(-i * 0.25f));
            if(i == 0) {
                x1 = x2;
                y1 = y2;
                b1 = b2;
                continue;
            }

            var cx =  y2 - y1;
            var cy =  x1 - x2;
            var len = Math.sqrt(cx*cx + cy*cy);
            float size = 1 / 32f;
            if(len != 0) {
                cx /= len;
                cy /= len;
            } else {
                x1 -= size;
                x2 += size;
                cx = 0;
                cy = 1;
            }
            cx *= size; cy *= size;

            putPoint(consumer, m4, x1 - cx, y1 - cy, R * b1, G * b1, B * b1, i);
            putPoint(consumer, m4, x1 + cx, y1 + cy, R * b1, G * b1, B * b1, i);

            putPoint(consumer, m4, x2 + cx, y2 + cy, R * b2, G * b2, B * b2, i);
            putPoint(consumer, m4, x2 - cx, y2 - cy, R * b2, G * b2, B * b2, i);

            x1 = x2;
            y1 = y2;
            b1 = b2;
        }
        ms.popPose();
    }

    private static void putPoint(VertexConsumer consumer, Matrix4f m4, float x, float y, float r, float g, float b, int z) {
        consumer.vertex(m4, x, y, z * 0.0001f)
                .color((int) r, (int) g, (int) b, 255)
                .endVertex();
    }
}
