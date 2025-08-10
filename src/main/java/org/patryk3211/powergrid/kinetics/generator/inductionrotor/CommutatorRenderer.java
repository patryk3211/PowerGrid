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

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorBlockEntity;
import org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer;

import static net.minecraft.state.property.Properties.HORIZONTAL_AXIS;

public class CommutatorRenderer extends RotorRenderer {
    public CommutatorRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RotorBlockEntity rotor, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider buffer, int light, int overlay) {
        super.renderSafe(rotor, partialTicks, matrixStack, buffer, light, overlay);

        var state = rotor.getCachedState();
        var axis = state.get(HORIZONTAL_AXIS);
        var facing = Direction.from(axis, Direction.AxisDirection.POSITIVE);

        var rotorAngle = getRotorAngle(rotor, partialTicks);
        var brushAngle = rotorAngle * 2;

        var sin = Math.sin(brushAngle);
        var brushOffset = sin * sin * 1 / 16f;

        var brush = CachedBuffers.partial(ModdedPartialModels.COMMUTATOR_BRUSH, state);
        brush.light(light)
                .center()
                .rotateToFace(facing)
                .uncenter()
                .translate(-brushOffset, 0, 0)
                .renderInto(matrixStack, buffer.getBuffer(RenderLayer.getSolid()));
        brush
                .center()
                .rotateToFace(facing.getOpposite())
                .uncenter()
                .translate(-brushOffset, 0, 0)
                .renderInto(matrixStack, buffer.getBuffer(RenderLayer.getSolid()));
    }

    @Override
    protected SuperByteBuffer getModelForState(BlockState state) {
        return CachedBuffers.partialFacing(ModdedPartialModels.COMMUTATOR_SHAFT, state, Direction.from(state.get(HORIZONTAL_AXIS), Direction.AxisDirection.POSITIVE));
    }
}
