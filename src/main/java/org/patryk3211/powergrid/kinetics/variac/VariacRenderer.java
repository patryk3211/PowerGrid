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
package org.patryk3211.powergrid.kinetics.variac;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class VariacRenderer extends KineticBlockEntityRenderer<VariacBlockEntity> {
    public VariacRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(VariacBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource provider, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, provider, light, overlay);

        var state = be.getBlockState();
        var buffer = CachedBuffers.partialFacing(ModdedPartialModels.VARIAC_ARMATURE, state, state.getValue(VariacBlock.HORIZONTAL_FACING).getOpposite());

        float angle = be.arm.getValue(partialTicks) * (float) Math.PI * 1.75f;
        buffer
                .rotateCentered(angle, Direction.UP)
                .light(light)
                .renderInto(ms, provider.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(VariacBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.UP);
    }
}
