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

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class RotorRenderer extends SafeBlockEntityRenderer<RotorBlockEntity> {
    public RotorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(RotorBlockEntity rotor, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        if(VisualizationManager.supportsVisualization(rotor.getLevel()) && rotor.getType() != ModdedBlockEntities.GENERATOR_LARGE_INDUCTION_ROTOR.get())
            return;

        var state = rotor.getBlockState();
        Direction.Axis axis = ((AbstractRotorBlock) state.getBlock()).getAssemblyRotationAxis(state);

        var rotorModel = getModelForState(state);
        var rotorAngle = getRotorAngle(rotor, partialTicks);

        rotorModel.light(light);
        rotorModel.rotateCentered(rotorAngle, Direction.get(Direction.AxisDirection.POSITIVE, axis));
        rotorModel.renderInto(matrixStack, buffer.getBuffer(RenderType.cutoutMipped()));
    }

    public static float getRotorAngle(SmartBlockEntity rotor, float partialTicks) {
        var behaviour = rotor.getBehaviour(RotorBehaviour.TYPE);
        var rotorAngle = behaviour.getAngle() + behaviour.getAngularVelocity() * 0.3f * partialTicks;
        rotorAngle = rotorAngle / 180f * (float) Math.PI;
        return rotorAngle;
    }

    protected SuperByteBuffer getModelForState(BlockState state) {
        return CachedBuffers.block(state);
    }
}
