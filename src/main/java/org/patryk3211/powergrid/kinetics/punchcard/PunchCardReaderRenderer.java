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
package org.patryk3211.powergrid.kinetics.punchcard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class PunchCardReaderRenderer extends KineticBlockEntityRenderer<PunchCardReaderBlockEntity> {
    public PunchCardReaderRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PunchCardReaderBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        if(be.currentItem().isEmpty())
            return;

        var state = be.getBlockState();
        var facing = state.getValue(PunchCardReaderBlock.HORIZONTAL_FACING);
        var card = CachedBuffers.partial(ModdedPartialModels.PUNCH_CARD, state);

        card
                .light(light)
                .center()
                .rotateToFace(facing)
                .uncenter()
                .translate(0, 0, -be.progress.getValue(partialTicks) * 6 / 16f)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected BlockState getRenderedBlockState(PunchCardReaderBlockEntity be) {
        return shaft(be.getBlockState().getValue(PunchCardReaderBlock.HORIZONTAL_FACING).getClockWise().getAxis());
    }
}
