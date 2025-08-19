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
package org.patryk3211.powergrid.equipment.thermometer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class ThermometerRenderer extends SafeBlockEntityRenderer<ThermometerBlockEntity> {
    public ThermometerRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    protected void renderSafe(ThermometerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        var state = be.getBlockState();
        var facing = state.getValue(ThermometerBlock.FACING);

        var buffer = CachedBuffers.partial(ModdedPartialModels.THERMOMETER_NEEDLE, state);
        var progress = Mth.lerp(partialTicks, be.prevDialState, be.dialState);
        prepareDial(buffer, facing, progress, light)
                .renderInto(ms, bufferSource.getBuffer(RenderType.solid()));

        var buffer2 = CachedBuffers.partial(ModdedPartialModels.THERMOMETER_NEEDLE_RED, state);
        prepareDial(buffer2, facing, be.maxState, light)
                .renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
    }

    private SuperByteBuffer prepareDial(SuperByteBuffer buffer, Direction facing, float progress, int light) {
        float dialPivotY = 5.75f / 16, dialPivotX = 10 / 16f;
        return rotateBuffer(buffer, facing).translate(dialPivotX, dialPivotY, 0)
                .rotateZ((float) (Math.PI / 2 * -progress))
                .translate(-dialPivotX, -dialPivotY, 0)
                .light(light);
    }

    private static SuperByteBuffer rotateBuffer(SuperByteBuffer buffer, Direction facing) {
        if(facing.getAxis() == Direction.Axis.Y) {
            return buffer.rotateXCenteredDegrees(facing == Direction.UP ? 90 : -90);
        } else {
            return buffer.rotateYCenteredDegrees(-facing.toYRot() - 180);
        }
    }
}
