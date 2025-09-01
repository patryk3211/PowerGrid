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
package org.patryk3211.powergrid.kinetics.generator.inductionrotor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS;

public class CommutatorRenderer extends RotorRenderer {
    public CommutatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RotorBlockEntity rotor, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(rotor, partialTicks, matrixStack, buffer, light, overlay);

        var rotorAngle = getRotorAngle(rotor, partialTicks);
        var brushAngle = rotorAngle * 2;

        var sin = Math.sin(brushAngle);
        var brushOffset = sin * sin * 1 / 16f;

        var state = rotor.getBlockState();
        if(state.getBlock() instanceof VerticalCommutatorBlock) {
            var facing = state.getValue(VerticalCommutatorBlock.HORIZONTAL_FACING);
            if(!state.getValue(VerticalCommutatorBlock.UP))
                facing = facing.getOpposite();

            var brush = CachedBuffers.partial(ModdedPartialModels.VERTICAL_COMMUTATOR_BRUSH, state);
            brush.light(light)
                    .center()
                    .rotateToFace(facing)
                    .uncenter()
                    .translate(-brushOffset, 0, 0)
                    .renderInto(matrixStack, buffer.getBuffer(RenderType.solid()));
            brush.light(light)
                    .center()
                    .rotateToFace(facing)
                    .rotateZ((float) Math.PI)
                    .uncenter()
                    .translate(-brushOffset, 0, 0)
                    .renderInto(matrixStack, buffer.getBuffer(RenderType.solid()));
        } else {
            var axis = state.getValue(CommutatorBlock.HORIZONTAL_FACING).getAxis();
            var facing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);

            var brush = CachedBuffers.partial(ModdedPartialModels.COMMUTATOR_BRUSH, state);
            brush.light(light)
                    .center()
                    .rotateToFace(facing)
                    .uncenter()
                    .translate(-brushOffset, 0, 0)
                    .renderInto(matrixStack, buffer.getBuffer(RenderType.solid()));
            brush.light(light)
                    .center()
                    .rotateToFace(facing.getOpposite())
                    .uncenter()
                    .translate(-brushOffset, 0, 0)
                    .renderInto(matrixStack, buffer.getBuffer(RenderType.solid()));
        }
    }

    @Override
    protected SuperByteBuffer getModelForState(BlockState state) {
        return CachedBuffers.partialFacing(ModdedPartialModels.COMMUTATOR_SHAFT, state, Direction.fromAxisAndDirection(state.getValue(HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE));
    }
}
