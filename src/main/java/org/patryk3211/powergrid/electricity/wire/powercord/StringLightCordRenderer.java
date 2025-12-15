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
package org.patryk3211.powergrid.electricity.wire.powercord;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

import static org.patryk3211.powergrid.electricity.wire.HangingWireRenderer.quad;

public class StringLightCordRenderer extends CordRenderer<StringLightCordEntity> {
    public static final ResourceLocation TEXTURE = PowerGrid.texture("entity/bulb");

    public StringLightCordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(StringLightCordEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        entity.beginRender();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    protected void segmentRenderHook(StringLightCordEntity entity, PoseStack matrices, MultiBufferSource vertexConsumers,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     float offset, float length, boolean first, boolean last, int light) {
        if(last)
            return;
        var power = Mth.lerp(AnimationTickHolder.getPartialTicks(), entity.prevPower, entity.power);
        int color = entity.nextColor();

        int r = (int) (((color >> 16) & 0xFF) * 0.80f + 64);
        int g = (int) (((color >> 8) & 0xFF) * 0.85f + 48);
        int b = (int) ((color & 0xFF) * 0.70f);

        int bulbColor = ((int) (r * (power * 0.75f + 0.25f)) << 16)
                | ((int) (g * (power * 0.75f + 0.25f)) << 8)
                | ((int) (b * (power * 0.75f + 0.25f)));

        bulb(matrices, vertexConsumers.getBuffer(RenderType.entityCutout(TEXTURE)), x2, y2 - 0.65f / 16f, z2, light, bulbColor | 0xFF000000);

        if(power > 0.01f) {
            int glowColor = ((int) (r * (power * 0.75f)) << 16)
                    | ((int) (g * (power * 0.75f)) << 8)
                    | ((int) (b * (power * 0.75f)));
            glow(matrices, vertexConsumers.getBuffer(ModdedRenderLayers.getAdditiveColor()), x2, y2 - 0.65f / 16f, z2, glowColor | 0xFF000000);
        }
    }

    private static void bulb(PoseStack ms, VertexConsumer buffer, float x, float y, float z, int light, int color) {
        final float SIZE = 2 / 16f;
        final float HALF_SIZE = SIZE * 0.5f;

        quad(ms.last(), buffer, light, color,
                x + HALF_SIZE, y       , z + HALF_SIZE,
                x - HALF_SIZE, y       , z + HALF_SIZE,
                x + HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z + HALF_SIZE,
                0, 0, 1,
                2 / 16f, 0, 2 / 16f, 0);
        quad(ms.last(), buffer, light, color,
                x + HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x + HALF_SIZE, y       , z - HALF_SIZE,
                x - HALF_SIZE, y       , z - HALF_SIZE,
                0, 0, -1,
                -2 / 16f, 2 / 16f, -2 / 16f, 2 / 16f);
        quad(ms.last(), buffer, light, color,
                x - HALF_SIZE, y       , z + HALF_SIZE,
                x - HALF_SIZE, y       , z - HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z - HALF_SIZE,
                -1, 0, 0,
                -2 / 16f, 2 / 16f, 2 / 16f, 0);
        quad(ms.last(), buffer, light, color,
                x + HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x + HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x + HALF_SIZE, y       , z + HALF_SIZE,
                x + HALF_SIZE, y       , z - HALF_SIZE,
                1, 0, 0,
                2 / 16f, 0, -2 / 16f, 2 / 16f);
        quad(ms.last(), buffer, light, color,
                x - HALF_SIZE, y, z + HALF_SIZE,
                x + HALF_SIZE, y, z + HALF_SIZE,
                x - HALF_SIZE, y, z - HALF_SIZE,
                x + HALF_SIZE, y, z - HALF_SIZE,
                0, 1, 0,
                2 / 16f, 0, 2 / 16f, 0);
        quad(ms.last(), buffer, light, color,
                x + HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x + HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z - HALF_SIZE,
                0, -1, 0,
                2 / 16f, 2 / 16f, -2 / 16f, 2 / 16f);
    }

    private static void glow(PoseStack ms, VertexConsumer buffer, float x, float y, float z, int color) {
        final float SMALL_OFFSET = 0.125f / 16f;
        final float SIZE = 2.25f / 16f;
        final float HALF_SIZE = SIZE * 0.5f;

        quad(ms.last(), buffer, color,
                x + HALF_SIZE, y + SMALL_OFFSET, z + HALF_SIZE,
                x - HALF_SIZE, y + SMALL_OFFSET, z + HALF_SIZE,
                x + HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z + HALF_SIZE);
        quad(ms.last(), buffer, color,
                x + HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x + HALF_SIZE, y + SMALL_OFFSET, z - HALF_SIZE,
                x - HALF_SIZE, y + SMALL_OFFSET, z - HALF_SIZE);
        quad(ms.last(), buffer, color,
                x - HALF_SIZE, y + SMALL_OFFSET, z + HALF_SIZE,
                x - HALF_SIZE, y + SMALL_OFFSET, z - HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z - HALF_SIZE);
        quad(ms.last(), buffer, color,
                x + HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x + HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x + HALF_SIZE, y + SMALL_OFFSET, z + HALF_SIZE,
                x + HALF_SIZE, y + SMALL_OFFSET, z - HALF_SIZE);
        quad(ms.last(), buffer, color,
                x - HALF_SIZE, y + SMALL_OFFSET, z + HALF_SIZE,
                x + HALF_SIZE, y + SMALL_OFFSET, z + HALF_SIZE,
                x - HALF_SIZE, y + SMALL_OFFSET, z - HALF_SIZE,
                x + HALF_SIZE, y + SMALL_OFFSET, z - HALF_SIZE);
        quad(ms.last(), buffer, color,
                x + HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z + HALF_SIZE,
                x + HALF_SIZE, y - SIZE, z - HALF_SIZE,
                x - HALF_SIZE, y - SIZE, z - HALF_SIZE);
    }
}
