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
package org.patryk3211.powergrid.electricity.portablebattery.forge;

import com.simibubi.create.content.equipment.armor.BacktankArmorLayer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;

public class BatteryArmorLayerImpl {
    @SuppressWarnings("rawtypes,unchecked")
    public static void registerOn(EntityRenderer<?> entityRenderer) {
        if (!(entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer))
            return;
        if (!(livingRenderer.getModel() instanceof BipedEntityModel))
            return;
        BacktankArmorLayer<?, ?> layer = new BacktankArmorLayer<>(livingRenderer);
        livingRenderer.addFeature((BacktankArmorLayer) layer);
    }

    public static void registerOnAll(EntityRenderDispatcher dispatcher) {
        for (EntityRenderer<? extends PlayerEntity> renderer : dispatcher.getSkinMap().values())
            registerOn(renderer);
        for (EntityRenderer<?> renderer : dispatcher.renderers.values())
            registerOn(renderer);
    }
}
