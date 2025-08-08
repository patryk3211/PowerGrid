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

import io.github.fabricators_of_create.porting_lib.event.client.ParticleManagerRegistrationCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardModel;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.ClientElectricNetwork;
import org.patryk3211.powergrid.electricity.info.TerminalHandler;
import org.patryk3211.powergrid.electricity.portablebattery.BatteryArmorLayer;
import org.patryk3211.powergrid.electricity.transformer.TransformerWindingScreen;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperRenderHandler;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingPreview;
import org.patryk3211.powergrid.ponder.PonderIndex;
import org.patryk3211.powergrid.ponder.PonderTags;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;
import org.patryk3211.powergrid.utility.PlacementOverlay;

public class PowerGridClient implements ClientModInitializer, ModelLoadingPlugin {
	public static final ElectroZapperRenderHandler ELECTRO_ZAPPER_RENDER_HANDLER = new ElectroZapperRenderHandler();

	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(this);

		ModdedKeys.register();

		ModdedPartialModels.register();
		ModdedRenderLayers.register();
		ParticleManagerRegistrationCallback.EVENT.register(ModdedParticles::registerFactories);
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(PowerGridClient::addEntityRendererLayers);

		ELECTRO_ZAPPER_RENDER_HANDLER.registerListeners();

		registerOverlays();

		ClientElectricNetwork.init();
		TerminalHandler.init();
		ModdedPackets.getChannel().initClientListener();
		WirePreview.init();
		PlacementOverlay.init();
		ClientTickEvents.END_CLIENT_TICK.register(this::clientTick);

		PonderTags.register();
		PonderIndex.register();
	}

	private void clientTick(MinecraftClient client) {
		if(client.world == null || client.player == null)
			return;

		ELECTRO_ZAPPER_RENDER_HANDLER.tick();
		CustomValueSettingsScreen.clientTick();
		WindingPreview.tick();
		TransformerWindingScreen.clientTick();
	}

	public void registerOverlays() {
		HudRenderCallback.EVENT.register((graphics, partialTicks) -> {
			Window window = MinecraftClient.getInstance().getWindow();
			PlacementOverlay.renderOverlay(MinecraftClient.getInstance().inGameHud, graphics);
        });
	}

	@Override
	public void onInitializeModelLoader(Context context) {
		var componentModels = ComponentModels.collectIds();
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
											   LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper registrationHelper, EntityRendererFactory.Context context) {
		BatteryArmorLayer.registerOn(entityRenderer, registrationHelper);
	}
}