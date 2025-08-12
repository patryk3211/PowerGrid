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
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class HvSwitchRenderer extends KineticBlockEntityRenderer<HvSwitchBlockEntity> {
    public HvSwitchRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HvSwitchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if(VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        var state = be.getBlockState();
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        var facing = state.getValue(HvSwitchBlock.HORIZONTAL_FACING);
        var rod = CachedBuffers.partialFacing(ModdedPartialModels.HV_SWITCH_ROD, state, facing);
        float angle = (1.0f - be.rod.getValue(partialTicks)) * (float) Math.PI * 0.5f;
        rod
                .rotateCentered(angle, facing.getClockWise())
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(HvSwitchBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, state, state.getValue(HvSwitchBlock.HORIZONTAL_FACING).getClockWise());
    }
}
