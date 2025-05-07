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
package org.patryk3211.powergrid.electricity.light.fixture;

import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.patryk3211.powergrid.electricity.light.bulb.ILightBulb;

public class LightFixtureRenderer extends SafeBlockEntityRenderer<LightFixtureBlockEntity> {
    public LightFixtureRenderer(BlockEntityRendererFactory.Context context) {
        super();
    }

    @Override
    protected void renderSafe(LightFixtureBlockEntity be, float partialTicks, MatrixStack matrices, VertexConsumerProvider consumer, int light, int overlay) {
        var properties = be.getLightBulbProperties();
        if(properties == null)
            return;
        var blockState = be.getCachedState();
        var bulbState = be.getState();
//        if(blockState.get(LightFixtureBlock.POWER) == 0)
//            bulbState = ILightBulb.State.OFF;
        var vb = consumer.getBuffer(RenderLayer.getCutout());

        var model = properties.getModelProvider().get().apply(bulbState);
        var buffer = CachedBufferer.partial(model, blockState);

        var facing = blockState.get(LightFixtureBlock.FACING);
        rotateToFacing(buffer, facing)
                .translate(((LightFixtureBlock) blockState.getBlock()).modelOffset)
                .light(light)
                .renderInto(matrices, vb);
    }

    public SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Direction facing) {
        return switch (facing) {
            case UP -> buffer;
            case DOWN -> buffer.rotateCentered(Direction.EAST, (float) Math.PI);
            default -> {
                buffer.rotateCentered(Direction.EAST, (float) Math.PI * 0.5f);
                yield buffer.rotateCentered(Direction.SOUTH, (float) ((facing.asRotation()) / 180f * Math.PI));
            }
        };
    }
}
