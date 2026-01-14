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
package org.patryk3211.powergrid.electricity.electricswitch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class HvBreakerRenderer extends KineticBlockEntityRenderer<HvBreakerBlockEntity> {
    public HvBreakerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HvBreakerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        var state = be.getBlockState();
        var facing = state.getValue(HvBreakerBlock.HORIZONTAL_FACING);

        var signalOpen = CachedBuffers.partial(ModdedPartialModels.HV_BREAKER_SIGNAL1, state);
        signalOpen
                .light(light)
                .center()
                .rotateToFace(facing)
                .uncenter()
                .translate(Mth.lerp(partialTicks, be.prevState ? 1 : 0, be.state ? 1 : 0) * 2 / 16f, 0, 0)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));

        var signalCharge = CachedBuffers.partial(ModdedPartialModels.HV_BREAKER_SIGNAL2, state);
        signalCharge
                .light(light)
                .center()
                .rotateToFace(facing)
                .uncenter()
                .translate((1 - be.charge.getValue(partialTicks)) * 2 / 16f, 0, 0)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(HvBreakerBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, state, state.getValue(HvSwitchBlock.HORIZONTAL_FACING).getClockWise());
    }
}
