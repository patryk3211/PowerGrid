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
package org.patryk3211.powergrid.kinetics.generator.clutch;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

import static org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer.getRotorAngle;

public class GeneratorClutchRenderer extends KineticBlockEntityRenderer<GeneratorClutchBlockEntity> {
    public GeneratorClutchRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(GeneratorClutchBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
        if(Backend.canUseInstancing(be.getWorld()))
            return;

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        var state = be.getCachedState();
        var facing = state.get(Properties.FACING);
        var axis = facing.getAxis();

        var rotorModel = CachedBufferer.partialFacing(ModdedPartialModels.CLUTCH_SHAFT, state, facing.getOpposite());
        var rotorAngle = getRotorAngle(be, partialTicks);

        rotorModel.light(light);
        rotorModel.rotateCentered(Direction.get(Direction.AxisDirection.POSITIVE, axis), rotorAngle);
        rotorModel.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(GeneratorClutchBlockEntity be, BlockState state) {
        var facing = state.get(GeneratorClutchBlock.FACING);
        return CachedBufferer.partialFacing(ModdedPartialModels.SHAFT_BIT, state, facing);
    }
}
