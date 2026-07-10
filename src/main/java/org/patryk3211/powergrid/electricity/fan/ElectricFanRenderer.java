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
package org.patryk3211.powergrid.electricity.fan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class ElectricFanRenderer extends SafeBlockEntityRenderer<ElectricFanBlockEntity> {
    public ElectricFanRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    protected void renderSafe(ElectricFanBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        var direction = be.getBlockState().getValue(FACING);
        var vb = buffer.getBuffer(RenderType.cutoutMipped());

        var fan = CachedBuffers.partialFacing(ModdedPartialModels.FAN_PROPELLER, be.getBlockState(), direction.getOpposite());

        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float speed = be.getSpeed() * 5;
        if(speed > 0)
            speed = Mth.clamp(speed, 80, 64 * 20);
        if(speed < 0)
            speed = Mth.clamp(speed, -64 * 20, -80);
        float angle = (time * speed * 3 / 10f) % 360;
        angle = angle / 180f * (float) Math.PI;


        fan
                .light(light)
                .rotateCentered(angle, Direction.get(Direction.AxisDirection.POSITIVE, direction.getAxis()))
                .renderInto(ms, vb);
    }
}
