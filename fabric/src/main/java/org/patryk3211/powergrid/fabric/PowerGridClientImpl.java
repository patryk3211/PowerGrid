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
package org.patryk3211.powergrid.fabric;

import io.github.fabricators_of_create.porting_lib.event.client.ClientWorldEvents;
import io.github.fabricators_of_create.porting_lib.event.client.ParticleManagerRegistrationCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.circuits.CircuitBoardModel;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.collections.fabric.ModdedKeysImpl;
import org.patryk3211.powergrid.collections.fabric.ModdedParticlesImpl;
import org.patryk3211.powergrid.electricity.ClientElectricNetwork;
import org.patryk3211.powergrid.electricity.portablebattery.fabric.BatteryArmorLayerImpl;
import org.patryk3211.powergrid.network.packets.EntityDataS2CPacket;

public class PowerGridClientImpl implements ClientModInitializer, ModelLoadingPlugin {
    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(this);
        ModdedKeysImpl.register();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(PowerGridClientImpl::addEntityRendererLayers);

        PowerGridClient.initClient();

        PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.registerListeners();
        ParticleManagerRegistrationCallback.EVENT.register(ModdedParticlesImpl::registerFactories);
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> EntityDataS2CPacket.clientEntityAdded(entity));

        // Register platform events
        ClientWorldEvents.UNLOAD.register(ClientElectricNetwork::unloadWorld);
    }

    @Override
    public void onInitializeModelLoader(Context context) {
        var componentModels = ComponentModels.collectRawIds();
        context.addModels(componentModels);
        context.resolveModel().register(innerContext -> {
            final var id = innerContext.id();
            if(id != null) {
                if(id.equals(CircuitBoardModel.MODEL_ID)) {
                    return new CircuitBoardModel();
                }
            }
            return null;
        });
    }

    public static void addEntityRendererLayers(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?> entityRenderer,
                                               LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper registrationHelper, EntityRendererProvider.Context context) {
        BatteryArmorLayerImpl.registerOn(entityRenderer, registrationHelper);
    }
}
