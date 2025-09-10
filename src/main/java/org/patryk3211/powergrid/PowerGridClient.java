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

import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.collections.ModdedRenderLayers;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.info.TerminalHandler;
import org.patryk3211.powergrid.electricity.transformer.TransformerWindingScreen;
import org.patryk3211.powergrid.electricity.wire.ClientWireInteractions;
import org.patryk3211.powergrid.electricity.zapper.ElectroZapperRenderHandler;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItemRenderer;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerItemRenderer;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingPreview;
import org.patryk3211.powergrid.ponder.PowerGridPonderPlugin;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;
import org.patryk3211.powergrid.utility.PlacementOverlay;

public class PowerGridClient {
	public static final ElectroZapperRenderHandler ELECTRO_ZAPPER_RENDER_HANDLER = new ElectroZapperRenderHandler();

	public static void initClient() {
		ModdedPartialModels.register();
		ModdedRenderLayers.register();

		registerOverlays();
		PlacementOverlay.init();

		registerArchitecturyEvents();

		PonderIndex.addPlugin(new PowerGridPonderPlugin());
	}

	public static void registerArchitecturyEvents() {
		ClientTickEvent.CLIENT_LEVEL_PRE.register(GlobalElectricNetworks::tick);
		ClientTickEvent.CLIENT_LEVEL_POST.register(TerminalHandler::tick);
		ClientTickEvent.CLIENT_POST.register(PowerGridClient::clientTick);
	}

	private static void clientTick(Minecraft client) {
		if(client.level == null || client.player == null)
			return;

		ELECTRO_ZAPPER_RENDER_HANDLER.tick();
		CustomValueSettingsScreen.clientTick();
		WindingPreview.tick();
		TransformerWindingScreen.clientTick();
		ClientWireInteractions.clientTick();
		ThermometerItemRenderer.clientTick();
		MultimeterItemRenderer.clientTick(client.level, client.player);
	}

	public static void registerOverlays() {
		ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
			Window window = Minecraft.getInstance().getWindow();
			PlacementOverlay.renderOverlay(Minecraft.getInstance().gui, graphics);
        });
	}
}