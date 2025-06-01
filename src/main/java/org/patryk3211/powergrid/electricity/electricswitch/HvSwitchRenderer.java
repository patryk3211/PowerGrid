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

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class HvSwitchRenderer extends KineticBlockEntityRenderer<HvSwitchBlockEntity> {
    public HvSwitchRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HvSwitchBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
        if(Backend.canUseInstancing(be.getWorld()))
            return;

        var state = be.getCachedState();
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        var facing = state.get(HvSwitchBlock.HORIZONTAL_FACING);
        var rod = CachedBufferer.partialFacing(ModdedPartialModels.HV_SWITCH_ROD, state, facing);
        float angle = (1.0f - be.rod.getValue(partialTicks)) * (float) Math.PI * 0.5f;
        rod
                .rotateCentered(facing.rotateYClockwise(), angle)
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(HvSwitchBlockEntity be, BlockState state) {
        return CachedBufferer.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, state, state.get(HvSwitchBlock.HORIZONTAL_FACING).rotateYClockwise());
    }
}
