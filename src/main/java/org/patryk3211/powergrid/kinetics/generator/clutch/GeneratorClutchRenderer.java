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

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.patryk3211.powergrid.collections.ModdedPartialModels;

import static org.patryk3211.powergrid.kinetics.generator.rotor.RotorRenderer.getRotorAngle;

public class GeneratorClutchRenderer extends KineticBlockEntityRenderer<GeneratorClutchBlockEntity> {
    public GeneratorClutchRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(GeneratorClutchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if(VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        var state = be.getBlockState();
        var facing = state.getValue(BlockStateProperties.FACING);
        var axis = facing.getAxis();

        var rotorModel = CachedBuffers.partialFacing(ModdedPartialModels.CLUTCH_SHAFT, state, facing.getOpposite());
        var rotorAngle = getRotorAngle(be, partialTicks);

        rotorModel.light(light);
        rotorModel.rotateCentered(rotorAngle, Direction.get(Direction.AxisDirection.POSITIVE, axis));
        rotorModel.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(GeneratorClutchBlockEntity be, BlockState state) {
        var facing = state.getValue(GeneratorClutchBlock.FACING);
        return CachedBuffers.partialFacing(ModdedPartialModels.SHAFT_BIT, state, facing);
    }
}
