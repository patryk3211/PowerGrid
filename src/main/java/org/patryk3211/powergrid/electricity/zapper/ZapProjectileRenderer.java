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
package org.patryk3211.powergrid.electricity.zapper;

import com.jozufozu.flywheel.util.transform.TransformStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.patryk3211.powergrid.PowerGrid;

@Environment(EnvType.CLIENT)
public class ZapProjectileRenderer extends EntityRenderer<ZapProjectileEntity> {
    public static final Identifier TEXTURE = PowerGrid.texture("entity/zap_projectile");

    public ZapProjectileRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(ZapProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        var buffer = consumers.getBuffer(RenderLayer.getEntitySolid(getTexture(entity)));

        var normalMatrix = matrices.peek().getNormalMatrix();

        var stack = TransformStack.cast(matrices);
        stack.pushPose();
        stack.translate(0, 0.125f, 0);

        stack.rotateY(-yaw);
        stack.rotateX(-entity.getPitch(tickDelta));

        final float UNIT = 1 / 16f;
        final float HALF_UNIT = UNIT / 2f;

        for(int i = 0; i < 4; ++i) {
            stack.rotateZ(90);

            var positionMatrix = matrices.peek().getPositionMatrix();
            light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
            vertex(positionMatrix, normalMatrix, buffer, -HALF_UNIT, -HALF_UNIT, -HALF_UNIT * 5, 0, 0, 0, 0, 1, light);
            vertex(positionMatrix, normalMatrix, buffer, -HALF_UNIT, -HALF_UNIT, HALF_UNIT * 5, UNIT * 5, 0, 0, 0, 1, light);
            vertex(positionMatrix, normalMatrix, buffer, HALF_UNIT, HALF_UNIT, HALF_UNIT * 5, UNIT * 5, UNIT, 0, 0, 1, light);
            vertex(positionMatrix, normalMatrix, buffer, HALF_UNIT, HALF_UNIT, -HALF_UNIT * 5, 0, UNIT, 0, 0, 1, light);
        }

        stack.popPose();
    }

    @Override
    public Identifier getTexture(ZapProjectileEntity entity) {
        return TEXTURE;
    }

    public void vertex(Matrix4f positionMatrix, Matrix3f normalMatrix, VertexConsumer vertexConsumer, float x, float y, float z, float u, float v, float normalX, float normalZ, float normalY, int light) {
        vertexConsumer
                .vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .next();
    }
}
