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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.checkerframework.checker.units.qual.C;
import org.patryk3211.powergrid.PowerGridClient;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.circuits.components.forge.CircuitBoardModel;
import org.patryk3211.powergrid.collections.forge.ModdedKeysImpl;
import org.patryk3211.powergrid.collections.forge.ModdedParticlesImpl;
import org.patryk3211.powergrid.electricity.portablebattery.forge.BatteryArmorLayerImpl;
import org.patryk3211.powergrid.electricity.wire.forge.WirePreviewImpl;

@OnlyIn(Dist.CLIENT)
public class PowerGridClientImpl {
    public static CircuitBoardModel CIRCUIT_BOARD_MODEL;

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
    public static void modelRequestLoad(ModelEvent.RegisterAdditional event) {
        var componentModels = ComponentModels.collectRawIds();
        componentModels.forEach(event::register);
        event.register(CircuitBoardModel.BASE_MODEL);
    }



    @SubscribeEvent
    public static void modelAlterBaked(ModelEvent.ModifyBakingResult event) {
        CIRCUIT_BOARD_MODEL = new CircuitBoardModel(event.getModels().get(CircuitBoardModel.BASE_MODEL));
        event.getModels().put(CircuitBoardModel.MODEL_ID, CIRCUIT_BOARD_MODEL);
    }

    @SubscribeEvent
    public static void stitchAtlas(TextureStitchEvent.Post event) {
        if(event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
            CIRCUIT_BOARD_MODEL.fetchSprites(event.getAtlas());
        }
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
