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
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.patryk3211.powergrid.chemistry.vat.ChemicalVatModel;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedParticles;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.patryk3211.powergrid.electricity.ClientElectricNetwork;
import org.patryk3211.powergrid.electricity.info.TerminalHandler;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.network.ClientBoundPackets;
import org.patryk3211.powergrid.ponder.PonderIndex;
import org.patryk3211.powergrid.utility.PlacementOverlay;

public class PowerGridClient implements ClientModInitializer, ModelLoadingPlugin {
	@Override
	public void onInitializeClient() {
		ModelLoadingPlugin.register(this);

		ModdedPartialModels.register();
		ModdedRenderLayers.register();
		ParticleManagerRegistrationCallback.EVENT.register(ModdedParticles::registerFactories);

		registerOverlays();

		ClientElectricNetwork.init();
		TerminalHandler.init();
		ClientBoundPackets.init();
		WirePreview.init();
		PlacementOverlay.init();

		PonderIndex.register();
	}

	public void registerOverlays() {
		HudRenderCallback.EVENT.register((graphics, partialTicks) -> {
			Window window = MinecraftClient.getInstance().getWindow();
			PlacementOverlay.renderOverlay(MinecraftClient.getInstance().inGameHud, graphics);
        });
	}

	@Override
	public void onInitializeModelLoader(Context context) {
		context.resolveModel().register(innerContext -> {
			final var id = innerContext.id();
			if(id != null) {
				if(id.equals(ChemicalVatModel.MODEL_ID)) {
					return new ChemicalVatModel();
				}
			}
			return null;
		});
	}
}