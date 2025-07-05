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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

public class RotorRenderer extends KineticBlockEntityRenderer<RotorBlockEntity> {
    public RotorRenderer(BlockEntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RotorBlockEntity rotor, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider buffer, int light, int overlay) {
        var state = getRenderedBlockState(rotor);
        var axis = state.get(Properties.AXIS);
        var side = state.get(RotorBlock.SHAFT_DIRECTION);

        Direction facing;
        if(side != ShaftDirection.NONE) {
            facing = side.with(axis);
            assert facing != null;

            float time = AnimationTickHolder.getRenderTime(rotor.getWorld());

            var shaft = CachedBufferer.partialFacing(ModdedPartialModels.SHAFT_BIT, state, facing);
            var offset = getRotationOffsetForPosition(rotor, rotor.getPos(), axis);
            var angle = time * rotor.getSpeed() * 3.0F / 10.0F % 360.0F;
            if (rotor.getSpeed() != 0.0F && rotor.hasSource()) {
                var source = rotor.source.subtract(rotor.getPos());
                var sourceFacing = Direction.getFacing((float) source.getX(), (float) source.getY(), (float) source.getZ());
                if (sourceFacing.getAxis() == axis) {
                    angle *= sourceFacing == facing ? 1.0F : -1.0F;
                } else if (sourceFacing.getDirection() == facing.getDirection()) {
                    angle *= -1.0F;
                }
            }

            angle += offset;
            angle = angle / 180.0F * (float) Math.PI;
            kineticRotationTransform(shaft, rotor, axis, angle, light);
            shaft.renderInto(matrixStack, buffer.getBuffer(RenderLayer.getSolid()));
        } else {
            facing = Direction.from(axis, Direction.AxisDirection.POSITIVE);
        }
        var rotorModel = CachedBufferer.partialFacing(getModelForState(state), state, facing);
        var rotorAngle = getRotorAngle(rotor, partialTicks);

        rotorModel.light(light);
        rotorModel.rotateCentered(Direction.get(Direction.AxisDirection.POSITIVE, axis), rotorAngle);
        rotorModel.renderInto(matrixStack, buffer.getBuffer(RenderLayer.getSolid()));
    }

    protected float getRotorAngle(RotorBlockEntity rotor, float partialTicks) {
        var behaviour = rotor.getRotorBehaviour();
        var rotorAngle = behaviour.getAngle() + behaviour.getAngularVelocity() * 0.3f * partialTicks;
        rotorAngle = rotorAngle / 180f * (float) Math.PI;
        return rotorAngle;
    }

    protected PartialModel getModelForState(BlockState state) {
        if(state.get(RotorBlock.SHAFT_DIRECTION) != ShaftDirection.NONE) {
            return ModdedPartialModels.ROTOR_SHAFT;
        } else {
            return ModdedPartialModels.ROTOR_FULL;
        }
    }
}
