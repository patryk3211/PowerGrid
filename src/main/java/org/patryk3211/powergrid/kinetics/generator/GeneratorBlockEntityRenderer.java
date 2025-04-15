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
package org.patryk3211.powergrid.kinetics.generator;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class GeneratorBlockEntityRenderer extends KineticBlockEntityRenderer<GeneratorBlockEntity> {
    public GeneratorBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(GeneratorBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
//        if (!Backend.canUseInstancing(be.getWorld())) {
            Direction facing = be.getCachedState().get(Properties.HORIZONTAL_FACING);
            BlockPos pos = be.getPos();
            float time = AnimationTickHolder.getRenderTime(be.getWorld());

            SuperByteBuffer shaft = CachedBufferer.partialFacing(AllPartialModels.SHAFT_HALF, be.getCachedState(), facing);
            float offset = getRotationOffsetForPosition(be, pos, facing.getAxis());
            float angle = time * be.getSpeed() * 3.0F / 10.0F % 360.0F;
            if (be.getSpeed() != 0.0F && be.hasSource()) {
                BlockPos source = be.source.subtract(be.getPos());
                Direction sourceFacing = Direction.getFacing((float)source.getX(), (float)source.getY(), (float)source.getZ());
                if (sourceFacing.getAxis() == facing.getAxis()) {
                    angle *= sourceFacing == facing ? 1.0F : -1.0F;
                } else if (sourceFacing.getDirection() == facing.getDirection()) {
                    angle *= -1.0F;
                }
            }

            angle += offset;
            angle = angle / 180.0F * (float)Math.PI;
            kineticRotationTransform(shaft, be, facing.getAxis(), angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderLayer.getSolid()));
//        }
    }
}
