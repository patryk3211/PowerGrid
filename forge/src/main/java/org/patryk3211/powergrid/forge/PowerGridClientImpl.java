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
package org.patryk3211.powergrid.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.circuits.components.forge.CircuitBoardModel;
import org.patryk3211.powergrid.circuits.components.forge.CircuitBoardModelLoader;
import org.patryk3211.powergrid.collections.forge.ModdedKeysImpl;
import org.patryk3211.powergrid.collections.forge.ModdedParticlesImpl;
import org.patryk3211.powergrid.electricity.portablebattery.forge.BatteryArmorLayerImpl;
import org.patryk3211.powergrid.electricity.wire.forge.WirePreviewImpl;

public class PowerGridClientImpl {
    public static void init() {
        PowerGridClient.initClient();

        // Mod specific bus
        PowerGridImpl.bus.register(PowerGridClientImpl.class);

        // Main forge event bus
        NeoForge.EVENT_BUS.register(ForgeClientEvents.class);
        NeoForge.EVENT_BUS.register(WirePreviewImpl.class);
        PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.registerListeners(NeoForge.EVENT_BUS);
    }

    @SubscribeEvent
    public static void particleManagerRegistration(RegisterParticleProvidersEvent event) {
        ModdedParticlesImpl.registerFactories(event);
    }

    @SubscribeEvent
    public static void modelRequestLoad(ModelEvent.RegisterAdditional event) {
        var componentModels = ComponentModels.collectIds();
        componentModels.forEach(event::register);
        event.register(new ModelResourceLocation(CircuitBoardModel.BASE_MODEL, "standalone"));
    }

    @SubscribeEvent
    public static void modelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(PowerGrid.asResource("circuit_board"), new CircuitBoardModelLoader());
    }

    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        BatteryArmorLayerImpl.registerOnAll(dispatcher);
    }

    @SubscribeEvent
    public static void keyRegistration(RegisterKeyMappingsEvent event) {
        ModdedKeysImpl.register(event);
    }
}
