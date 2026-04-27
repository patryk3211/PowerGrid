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
package org.patryk3211.powergrid.electricity.sparkgap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class SparkGapRenderer extends SafeBlockEntityRenderer<SparkGapBlockEntity> {
    public SparkGapRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    protected void renderSafe(SparkGapBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        var state = be.getBlockState();
        var buffer = CachedBuffers.partial(ModdedPartialModels.SPARK_GAP_ARM, state);
        var facing = Direction.fromAxisAndDirection(state.getValue(SparkGapBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
        float offset = be.setting.getValue() / 18f * (3 / 16f);

        var consumer = bufferSource.getBuffer(RenderType.solid());
        buffer
                .light(light)
                .center()
                .rotateToFace(facing)
                .uncenter()
                .translate(0, 0, -offset)
                .renderInto(ms, consumer);
        buffer
                .light(light)
                .center()
                .rotateToFace(facing)
                .rotateYDegrees(180)
                .uncenter()
                .translate(0, 0, -offset)
                .renderInto(ms, consumer);
    }
}
