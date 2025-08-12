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

import net.minecraft.client.MinecraftClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.collections.ModdedParticles;
import org.patryk3211.powergrid.collections.forge.ModdedKeysImpl;
import org.patryk3211.powergrid.collections.forge.ModdedParticlesImpl;
import org.patryk3211.powergrid.electricity.portablebattery.forge.BatteryArmorLayerImpl;
import org.patryk3211.powergrid.electricity.wire.forge.WirePreviewImpl;

@OnlyIn(Dist.CLIENT)
public class PowerGridClientImpl {
    public static void init() {
        PowerGridClient.initClient();

        // Mod specific bus
        PowerGridImpl.bus.register(PowerGridClientImpl.class);

        // Main forge event bus
        MinecraftForge.EVENT_BUS.register(ForgeClientEvents.class);
        MinecraftForge.EVENT_BUS.register(WirePreviewImpl.class);
        PowerGridClient.ELECTRO_ZAPPER_RENDER_HANDLER.registerListeners(MinecraftForge.EVENT_BUS);
    }

    @SubscribeEvent
    public static void particleManagerRegistration(RegisterParticleProvidersEvent event) {
        ModdedParticlesImpl.registerFactories(event);
    }

    @SubscribeEvent
    public static void addEntityLayers(EntityRenderersEvent.AddLayers event) {
        var dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        BatteryArmorLayerImpl.registerOnAll(dispatcher);
    }

    @SubscribeEvent
    public static void keyRegistration(RegisterKeyMappingsEvent event) {
        ModdedKeysImpl.register(event);
    }
}
