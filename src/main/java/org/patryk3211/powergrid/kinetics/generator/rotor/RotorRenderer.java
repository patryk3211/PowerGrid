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

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

public class RotorRenderer extends SafeBlockEntityRenderer<RotorBlockEntity> {
    public RotorRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    protected void renderSafe(RotorBlockEntity rotor, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider buffer, int light, int overlay) {
        if(Backend.canUseInstancing(rotor.getWorld()))
            return;

        var state = rotor.getCachedState();
        var axis = state.get(Properties.AXIS);

        var rotorModel = getModelForState(state);
        var rotorAngle = getRotorAngle(rotor, partialTicks);

        rotorModel.light(light);
        rotorModel.rotateCentered(Direction.get(Direction.AxisDirection.POSITIVE, axis), rotorAngle);
        rotorModel.renderInto(matrixStack, buffer.getBuffer(RenderLayer.getSolid()));
    }

    public static float getRotorAngle(SmartBlockEntity rotor, float partialTicks) {
        var behaviour = rotor.getBehaviour(RotorBehaviour.TYPE);
        var rotorAngle = behaviour.getAngle() + behaviour.getAngularVelocity() * 0.3f * partialTicks;
        rotorAngle = rotorAngle / 180f * (float) Math.PI;
        return rotorAngle;
    }

    protected SuperByteBuffer getModelForState(BlockState state) {
        return CachedBufferer.block(state);
    }
}
