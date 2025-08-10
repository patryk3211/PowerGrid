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

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class SparkGapRenderer extends SafeBlockEntityRenderer<SparkGapBlockEntity> {
    public SparkGapRenderer(BlockEntityRendererFactory.Context context) { }

    @Override
    protected void renderSafe(SparkGapBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        var state = be.getCachedState();
        var buffer = CachedBuffers.partial(ModdedPartialModels.SPARK_GAP_ARM, state);
        var facing = Direction.from(state.get(SparkGapBlock.HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE);
        float offset = be.setting.getValue() / 18f * (3 / 16f);

        var consumer = bufferSource.getBuffer(RenderLayer.getSolid());
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
                .rotateY(180)
                .uncenter()
                .translate(0, 0, -offset)
                .renderInto(ms, consumer);
    }
}
