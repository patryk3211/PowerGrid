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
package org.patryk3211.powergrid.electricity.portablebattery;

import com.simibubi.create.content.equipment.armor.BacktankArmorLayer;
import com.simibubi.create.content.equipment.armor.BacktankBlock;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;

/**
 * @see BacktankArmorLayer
 * @param <T>
 * @param <M>
 */
public class BatteryArmorLayer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {
    public BatteryArmorLayer(FeatureRendererContext<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider buffer, int light, LivingEntity entity, float yaw, float pitch,
                       float pt, float p_225628_8_, float p_225628_9_, float p_225628_10_) {
        if(entity.getPose() == EntityPose.SLEEPING)
            return;

        var item = PortableBatteryItem.getWornBy(entity);
        if(item == null)
            return;

        M entityModel = getContextModel();
        if (!(entityModel instanceof BipedEntityModel<?> model))
            return;

        RenderLayer renderType = TexturedRenderLayers.getEntityCutout();
        BlockState renderedState = item.getBlock().getDefaultState()
                .with(BacktankBlock.HORIZONTAL_FACING, Direction.NORTH);
        SuperByteBuffer backtank = CachedBuffers.block(renderedState);

        ms.push();

        model.body.rotate(ms);
        ms.translate(-1 / 2f, 10 / 16f, 1f);
        ms.scale(1, -1, -1);

        backtank.light(light)
                .renderInto(ms, buffer.getBuffer(renderType));
//        backtank.forEntityRender()
//                .light(light)
//                .renderInto(ms, buffer.getBuffer(renderType));

        ms.pop();
    }

    public static void registerOn(EntityRenderer<?> entityRenderer, LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helper) {
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer))
            return;
        if (!(livingRenderer.getModel() instanceof BipedEntityModel))
            return;
        BatteryArmorLayer<?, ?> layer = new BatteryArmorLayer<>(livingRenderer);
        helper.register(layer);
    }
}
