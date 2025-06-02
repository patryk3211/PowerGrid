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
package org.patryk3211.powergrid.electricity.fan;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import static net.minecraft.state.property.Properties.FACING;

public class ElectricFanRenderer extends SafeBlockEntityRenderer<ElectricFanBlockEntity> {
    public ElectricFanRenderer(BlockEntityRendererFactory.Context context) {

    }

    @Override
    protected void renderSafe(ElectricFanBlockEntity be, float partialTicks, MatrixStack ms, VertexConsumerProvider buffer, int light, int overlay) {
        var direction = be.getCachedState().get(FACING);
        var vb = buffer.getBuffer(RenderLayer.getCutoutMipped());

        int lightInFront = WorldRenderer.getLightmapCoordinates(be.getWorld(), be.getPos().offset(direction));

        var fan = CachedBufferer.partialFacing(AllPartialModels.ENCASED_FAN_INNER, be.getCachedState(), direction.getOpposite());

        float time = AnimationTickHolder.getRenderTime(be.getWorld());
        float speed = be.getSpeed() * 5;
        if(speed > 0)
            speed = MathHelper.clamp(speed, 80, 64 * 20);
        if(speed < 0)
            speed = MathHelper.clamp(speed, -64 * 20, -80);
        float angle = (time * speed * 3 / 10f) % 360;
        angle = angle / 180f * (float) Math.PI;


        fan
                .light(lightInFront)
                .rotateCentered(Direction.get(Direction.AxisDirection.POSITIVE, direction.getAxis()), angle)
                .renderInto(ms, vb);
    }
}
