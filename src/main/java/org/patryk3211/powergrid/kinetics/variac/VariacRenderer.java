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

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class VariacRenderer extends KineticBlockEntityRenderer<VariacBlockEntity> {
    public VariacRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(VariacBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider provider, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, provider, light, overlay);

        var state = be.getCachedState();
        var buffer = CachedBuffers.partialFacing(ModdedPartialModels.VARIAC_ARMATURE, state, state.get(VariacBlock.HORIZONTAL_FACING).getOpposite());

        float angle = be.arm.getValue(partialTicks) * (float) Math.PI * 1.75f;
        buffer
                .rotateCentered(angle, Direction.UP)
                .light(light)
                .renderInto(ms, provider.getBuffer(RenderLayer.getSolid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(VariacBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.UP);
    }
}
