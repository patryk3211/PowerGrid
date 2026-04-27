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
package org.patryk3211.powergrid.electricity.light.fixture;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class LightFixtureRenderer extends SafeBlockEntityRenderer<LightFixtureBlockEntity> {
    public LightFixtureRenderer(BlockEntityRendererProvider.Context context) {
        super();
    }

    @Override
    protected void renderSafe(LightFixtureBlockEntity be, float partialTicks, PoseStack matrices, MultiBufferSource consumer, int light, int overlay) {
        var bulbState = be.getBulbState();
        if(bulbState == null)
            return;

        var state = be.getBlockState();
        var facing = state.getValue(LightFixtureBlock.FACING);

        var vb = consumer.getBuffer(RenderType.cutout());
        var model = bulbState.getModel();
        if (model == null)
            return;
        var buffer = CachedBuffers.partial(model, state);
        rotateToFacing(buffer, facing)
                .translate(((LightFixtureBlock) state.getBlock()).modelOffset)
                .light(light)
                .renderInto(matrices, vb);

        var color = bulbState.getColor();
        int r = 255, g = 255, b = 255;
        if(color != null) {
            vb = consumer.getBuffer(RenderType.translucent());
            var bulbBuffer = CachedBuffers.partial(bulbState.getDyedBulb(), state);
            var texDif = color.getTextureDiffuseColors();
            r = (int) (texDif[0] * 255);
            g = (int) (texDif[1] * 255);
            b = (int) (texDif[2] * 255);
            rotateToFacing(bulbBuffer, facing)
                    .color(r, g, b, 255)
                    .translate(((LightFixtureBlock) state.getBlock()).modelOffset)
                    .light(light)
                    .renderInto(matrices, vb);
        }

        if(bulbState.isBurned())
            return;

        float a = bulbState.getAlpha();
        if(a > 0) {
            var vba = consumer.getBuffer(ModdedRenderLayers.getAdditive());
            var lightModel = bulbState.getLightModel();
            var lightBuffer = CachedBuffers.partial(lightModel, state);
            rotateToFacing(lightBuffer, facing)
                    .translate(((LightFixtureBlock) state.getBlock()).modelOffset)
                    .light(light)
                    .color((int) (a * r), (int) (a * g), (int) (a * b), 255)
                    .renderInto(matrices, vba);
        }
    }

    public SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        return switch (facing) {
            case UP -> buffer;
            case DOWN -> buffer.rotateCentered((float) Math.PI, Direction.EAST);
            default -> {
                buffer.rotateCentered((float) Math.PI * 0.5f, Direction.EAST);
                yield buffer.rotateCentered((float) ((facing.toYRot()) / 180f * Math.PI), Direction.SOUTH);
            }
        };
    }
}
