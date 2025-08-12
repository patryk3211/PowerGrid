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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class GaugeRenderer extends SafeBlockEntityRenderer<GaugeBlockEntity> {
    public GaugeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(GaugeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
//        if (Backend.canUseInstancing(be.getWorld())) return;
        var gaugeState = be.getBlockState();

        var headBuffer = CachedBuffers.partial(getHeadModel(gaugeState, be), gaugeState);
        var dialBuffer = CachedBuffers.partial(getDialModel(gaugeState), gaugeState);

        float progress = Mth.lerp(partialTicks, be.prevDialState, be.dialState);

        for (Direction facing : Iterate.directions) {
            if (!((IGaugeBlock) gaugeState.getBlock()).shouldRenderHeadOnFace(be.getLevel(), be.getBlockPos(), gaugeState, facing))
                continue;

            float dialPivot = 5.75f / 16;
            VertexConsumer vb = buffer.getBuffer(RenderType.solid());
            rotateBufferTowards(dialBuffer, facing).translate(0, dialPivot, dialPivot)
                    .rotate(Direction.EAST.getAxis(), (float) (Math.PI / 2 * -progress))
                    .translate(0, -dialPivot, -dialPivot)
                    .light(light)
                    .renderInto(ms, vb);
            rotateBufferTowards(headBuffer, facing).light(light)
                    .renderInto(ms, vb);
        }
    }

    protected SuperByteBuffer rotateBufferTowards(SuperByteBuffer buffer, Direction target) {
        return buffer.rotateCentered((float) ((-target.toYRot() - 90) / 180 * Math.PI), Direction.UP);
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
