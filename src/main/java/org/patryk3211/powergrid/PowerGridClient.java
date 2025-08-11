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
package org.patryk3211.powergrid;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.fabricators_of_create.porting_lib.event.client.ParticleManagerRegistrationCallback;
import net.createmod.ponder.foundation.PonderIndex;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.info.TerminalHandler;
import org.patryk3211.powergrid.electricity.portablebattery.BatteryArmorLayer;
import org.patryk3211.powergrid.electricity.transformer.TransformerWindingScreen;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperRenderHandler;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingPreview;
import org.patryk3211.powergrid.ponder.PowerGridPonderPlugin;
import org.patryk3211.powergrid.ponder.PowerGridPonderScenes;
import org.patryk3211.powergrid.ponder.PowerGridPonderTags;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;
import org.patryk3211.powergrid.utility.PlacementOverlay;

public class PowerGridClient {
	public static final ElectroZapperRenderHandler ELECTRO_ZAPPER_RENDER_HANDLER = new ElectroZapperRenderHandler();

	public static void initClient() {
		ModdedKeys.register();

		ModdedPartialModels.register();
		ModdedRenderLayers.register();
		ParticleManagerRegistrationCallback.EVENT.register(ModdedParticles::registerFactories);
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(PowerGridClient::addEntityRendererLayers);

		ELECTRO_ZAPPER_RENDER_HANDLER.registerListeners();

		registerOverlays();

		TerminalHandler.init();

		WirePreview.init();
		PlacementOverlay.init();

		registerArchitecturyEvents();
		registerPlatformEvents();

		PonderIndex.addPlugin(new PowerGridPonderPlugin());
	}

	public static void registerArchitecturyEvents() {
		ClientTickEvents.START_WORLD_TICK.register(GlobalElectricNetworks::tick);
		ClientTickEvent.CLIENT_POST.register(PowerGridClient::clientTick);
	}

	@ExpectPlatform
	public static void registerPlatformEvents() {
		throw new AssertionError();
	}

	private static void clientTick(MinecraftClient client) {
		if(client.world == null || client.player == null)
			return;

		ELECTRO_ZAPPER_RENDER_HANDLER.tick();
		CustomValueSettingsScreen.clientTick();
		WindingPreview.tick();
		TransformerWindingScreen.clientTick();
	}

	public static void registerOverlays() {
		HudRenderCallback.EVENT.register((graphics, partialTicks) -> {
			Window window = MinecraftClient.getInstance().getWindow();
			PlacementOverlay.renderOverlay(MinecraftClient.getInstance().inGameHud, graphics);
        });
	}

	public static void addEntityRendererLayers(EntityType<? extends LivingEntity> entityType, LivingEntityRenderer<?, ?> entityRenderer,
											   LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper registrationHelper, EntityRendererFactory.Context context) {
		BatteryArmorLayer.registerOn(entityRenderer, registrationHelper);
	}
}