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
package org.patryk3211.powergrid.electricity.gauge;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class GaugeRenderer extends SafeBlockEntityRenderer<GaugeBlockEntity> {
    public GaugeRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    protected void renderSafe(GaugeBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
//        if (Backend.canUseInstancing(be.getWorld())) return;
        var gaugeState = be.getCachedState();

        SuperByteBuffer headBuffer = CachedBufferer.partial(getHeadModel(gaugeState, be), gaugeState);
        SuperByteBuffer dialBuffer = CachedBufferer.partial(getDialModel(gaugeState), gaugeState);

        float progress = MathHelper.lerp(partialTicks, be.prevDialState, be.dialState);

        for (Direction facing : Iterate.directions) {
            if (!((IGaugeBlock) gaugeState.getBlock()).shouldRenderHeadOnFace(be.getWorld(), be.getPos(), gaugeState, facing))
                continue;

            float dialPivot = 5.75f / 16;
            VertexConsumer vb = buffer.getBuffer(RenderLayer.getSolid());
            rotateBufferTowards(dialBuffer, facing).translate(0, dialPivot, dialPivot)
                    .rotate(Direction.EAST, (float) (Math.PI / 2 * -progress))
                    .translate(0, -dialPivot, -dialPivot)
                    .light(light)
                    .renderInto(ms, vb);
            rotateBufferTowards(headBuffer, facing).light(light)
                    .renderInto(ms, vb);
        }
    }

    protected SuperByteBuffer rotateBufferTowards(SuperByteBuffer buffer, Direction target) {
        return buffer.rotateCentered(Direction.UP, (float) ((-target.asRotation() - 90) / 180 * Math.PI));
    }

    public static PartialModel getHeadModel(BlockState state, GaugeBlockEntity entity) {
        var block = state.getBlock();
        if(block instanceof GaugeBlock<?> gaugeBlock) {
            return switch(gaugeBlock.material) {
                case ANDESITE -> {
                    if(entity instanceof CurrentGaugeBlockEntity)
                        yield ModdedPartialModels.ANDESITE_CURRENT_HEAD;
                    else
                        yield ModdedPartialModels.ANDESITE_VOLTAGE_HEAD;
                }
                case BRASS -> {
                    if(entity instanceof CurrentGaugeBlockEntity)
                        yield ModdedPartialModels.BRASS_CURRENT_HEAD;
                    else
                        yield ModdedPartialModels.BRASS_VOLTAGE_HEAD;
                }
            };
        }
        throw new IllegalArgumentException("Unknown block type");
    }

    public static PartialModel getDialModel(BlockState state) {
        var block = state.getBlock();
        if(block instanceof GaugeBlock<?> gaugeBlock) {
            return switch(gaugeBlock.material) {
                case ANDESITE -> AllPartialModels.GAUGE_DIAL;
                case BRASS -> ModdedPartialModels.BRASS_GAUGE_DIAL;
            };
        }
        throw new IllegalArgumentException("Unknown block type");
    }
}
