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
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.*;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.info.TerminalHandler;
import org.patryk3211.powergrid.electricity.transformer.TransformerWindingScreen;
import org.patryk3211.powergrid.electricity.wire.ClientWireInteractions;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.electricity.wire.WirePreview;
import org.patryk3211.powergrid.equipment.multimeter.MultimeterItemRenderer;
import org.patryk3211.powergrid.equipment.thermometer.ThermometerItemRenderer;
import org.patryk3211.powergrid.equipment.zapper.ElectroZapperRenderHandler;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingPreview;
import org.patryk3211.powergrid.network.packets.AlternatePlacementStatusC2SPacket;
import org.patryk3211.powergrid.network.packets.NegotiateSyncC2SPacket;
import org.patryk3211.powergrid.ponder.PowerGridPonderPlugin;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;
import org.patryk3211.powergrid.utility.PlacementOverlay;
import org.patryk3211.powergrid.utility.sound.SoundScapes;

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
		ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(PowerGridClient::clientJoin);
		ClientTickEvent.CLIENT_LEVEL_PRE.register(GlobalElectricNetworks::preTick);
		ClientTickEvent.CLIENT_LEVEL_POST.register(TerminalHandler::tick);
		ClientTickEvent.CLIENT_POST.register(PowerGridClient::clientTick);
		ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register(PowerGridClient::clientRespawn);
		ClientTooltipEvent.ITEM.register(WireItem::tooltip);
		ClientRawInputEvent.KEY_PRESSED.register(PowerGridClient::keyPress);
	}

	private static EventResult keyPress(Minecraft client, int keyCode, int scanCode, int action, int modifiers) {
		if(client.player == null || client.level == null)
			return EventResult.pass();
		if(!ModdedKeys.ALTERNATE_WIRE_PLACEMENT.matchesKey(keyCode, scanCode))
			return EventResult.pass();
		var stack = client.player.getMainHandItem();
		if(!IWire.isWire(client.level, stack.getItem()))
			return EventResult.pass();
		var tag = stack.getTagElement("Connection");
		if(tag == null)
			return EventResult.pass();
		// Update alternate placement status
		if(action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE) {
			ModdedPackets.sendToServer(new AlternatePlacementStatusC2SPacket(action == GLFW.GLFW_PRESS));
		}
		return EventResult.pass();
	}

	private static void clientRespawn(LocalPlayer localPlayer, LocalPlayer localPlayer1) {
		ModdedPackets.sendToServer(new NegotiateSyncC2SPacket(ModdedConfigs.common().syncWithDoubles.get()));
	}

	private static void clientJoin(LocalPlayer player) {
		ModdedPackets.sendToServer(new NegotiateSyncC2SPacket(ModdedConfigs.common().syncWithDoubles.get()));
	}

	private static void clientTick(Minecraft client) {
		if(client.level == null || client.player == null)
			return;

		SoundScapes.tick();

		ELECTRO_ZAPPER_RENDER_HANDLER.tick();
		CustomValueSettingsScreen.clientTick();
		WindingPreview.tick();
		WirePreview.tick();
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