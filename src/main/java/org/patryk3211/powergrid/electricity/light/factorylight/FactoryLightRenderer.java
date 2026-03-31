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
package org.patryk3211.powergrid.electricity.light.factorylight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;

public class FactoryLightRenderer extends SafeBlockEntityRenderer<FactoryLightBlockEntity> {
    public FactoryLightRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(FactoryLightBlockEntity be, float partialTicks, PoseStack matrices, MultiBufferSource consumer, int light, int overlay) {
        var bulbState = be.getBulbState();
        if(bulbState == null || bulbState.isBurned())
            return;

        var state = be.getBlockState();
        var axis = state.getValue(FactoryLightBlock.HORIZONTAL_AXIS);
        var part = state.getValue(FactoryLightBlock.PART);
        int rotation = 0;
        PartialModel lightModel = switch(part) {
            case 0 -> ModdedPartialModels.FL_RAYS_SINGLE;
            case 1 -> ModdedPartialModels.FL_RAYS_FRONT;
            case 2 -> ModdedPartialModels.FL_RAYS_CENTER;
            case 3 -> ModdedPartialModels.FL_RAYS_BACK;
            case 4 -> {
                rotation = 90;
                yield ModdedPartialModels.FL_RAYS_FRONT;
            }
            case 5 -> {
                rotation = 90;
                yield ModdedPartialModels.FL_RAYS_CENTER;
            }
            case 6 -> {
                rotation = 90;
                yield ModdedPartialModels.FL_RAYS_BACK;
            }
            default -> throw new IllegalStateException();
        };
        int a = (int) (bulbState.getAlpha() * 255);
        if(a > 0) {
            var vba = consumer.getBuffer(ModdedRenderLayers.getAdditive());
            var lightBuffer = CachedBuffers.partial(lightModel, state);
            lightBuffer
                    .light(light)
                    .rotateYCenteredDegrees(rotation)
                    .color(a, a, a, 255)
                    .renderInto(matrices, vba);
        }
    }

}
