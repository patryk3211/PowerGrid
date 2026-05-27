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

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import dev.architectury.event.events.common.*;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import org.patryk3211.powergrid.advancements.PowerGridTriggers;
import org.patryk3211.powergrid.circuits.components.Components;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.compat.sable.SableUtils;
import org.patryk3211.powergrid.compat.simulated.SimulatedBridge;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.deviceconnector.DeviceConnectorBlockEntity;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.electricity.fan.ElectricFanBlockEntity;
import org.patryk3211.powergrid.electricity.heater.HeaterFanProcessingTypes;
import org.patryk3211.powergrid.electricity.light.string.StringLightCordRecipe;
import org.patryk3211.powergrid.electricity.redstoneconverter.RedstoneConverterRegistry;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.solver.NativeMNA;
import org.patryk3211.powergrid.electricity.wire.EntityWireInteraction;
import org.patryk3211.powergrid.electricity.wire.WireItem;
import org.patryk3211.powergrid.equipment.BoostRecipe;
import org.patryk3211.powergrid.equipment.thunder.LightningRodMovementBehaviour;
import org.patryk3211.powergrid.kinetics.punchcard.PunchCardReaderBlockEntity;
import org.patryk3211.powergrid.network.packets.NegotiateSyncC2SPacket;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.proxy.SubstituteBlockEntityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerGrid {
	public static final String MOD_ID = "powergrid";

	public static final Logger LOGGER = LoggerFactory.getLogger(PowerGrid.class);

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(MOD_ID, Registries.RECIPE_SERIALIZER);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(MOD_ID, Registries.RECIPE_TYPE);
	public static final DeferredRegister<FanProcessingType> FAN_PROCESSING_TYPES = DeferredRegister.create(MOD_ID, CreateRegistries.FAN_PROCESSING_TYPE);

	public static AbstractPowerGridRegistrate REGISTRATE;

	public static void init() {
		LOGGER.info("Power grid starting, prepare to be electrocuted");
		ElectricalNetwork.LOGGER = LOGGER;

		NativeMNA.tryLoad();
		if(dev.architectury.platform.Platform.isModLoaded("sable")) {
			SableUtils.makeFullProxy();
		} else {
			SableUtils.makeDummyProxy();
		}

		ModdedSoundEvents.prepare();

		REGISTRATE = createRegistrate();
		finalizeRegistrate();

		register();

		registerArchitecturyEvents();

		if(dev.architectury.platform.Platform.isModLoaded("simulated")) {
			SimulatedBridge.init();
		}

		ModdedPackets.register();
		ModPackets.PACKETS.registerC2SListener();
		ModPackets.PACKETS.registerS2CListener();
	}

	public static void registerArchitecturyEvents() {
		TickEvent.ServerLevelTick.SERVER_LEVEL_PRE.register(GlobalElectricNetworks::preTick);
		TickEvent.ServerLevelTick.SERVER_LEVEL_POST.register(GlobalElectricNetworks::postTick);
		TickEvent.SERVER_POST.register(EntityWireInteraction::postTick);
		LifecycleEvent.SERVER_LEVEL_UNLOAD.register(GlobalElectricNetworks::unloadWorld);
        LifecycleEvent.SETUP.register(PowerGrid::setup);
		CommandRegistrationEvent.EVENT.register(ModdedCommands::register);
		PlayerEvent.PLAYER_JOIN.register(PowerGrid::playerJoin);
		PlayerEvent.PLAYER_QUIT.register(PowerGrid::playerQuit);
		InteractionEvent.RIGHT_CLICK_BLOCK.register(WireItem::useOn);
		InteractionEvent.RIGHT_CLICK_ITEM.register(WireItem::use);
	}

	private static void setup() {
		RedstoneConverterRegistry.init();
	}

	private static void playerQuit(ServerPlayer player) {
		NegotiateSyncC2SPacket.SYNC_TYPES.remove(player);
	}

	private static void playerJoin(ServerPlayer player) {
		if(player.hasPermissions(3) && !ModdedConfigs.server().isUpToDate()) {
			player.sendSystemMessage(Lang.translateDirect("message.outdated_configs"));
			player.sendSystemMessage(Component.empty()
					.append(Lang.translateDirect("message.reset_configs")
							.withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE)
									.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/powergrid reset_configs")))
					).append(Component.literal(" "))
					.append(Lang.translateDirect("message.disable_config_message")
							.withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE)
									.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/powergrid ignore_configs")))
					)
			);
		}
	}

	private static void register() {
		registerRecipes();
		HeaterFanProcessingTypes.register();

		SubstituteBlockEntityProvider.INSTANCE.registerDefault(DeviceConnectorBlockEntity.class, DeviceConnectorBlockEntity::new);
		SubstituteBlockEntityProvider.INSTANCE.registerDefault(PunchCardReaderBlockEntity.class, PunchCardReaderBlockEntity::new);
		SubstituteBlockEntityProvider.INSTANCE.registerDefault(ElectricFanBlockEntity.class, ElectricFanBlockEntity::new);
		SubstituteBlockEntityProvider.INSTANCE.lock();

		ModdedDisplaySources.register();
		ModdedBlocks.register();
		ModdedItems.register();
		ModdedFluids.register();
		ModdedBlockEntities.register();
		ModdedEntities.register();
		ModdedConfigs.register();
		ModdedMenus.register();
		Components.register();

		ModdedParticles.register();

		ModdedDataComponents.REGISTER.register();
		RECIPE_SERIALIZERS.register();
		RECIPE_TYPES.register();
		FAN_PROCESSING_TYPES.register();
		ModdedParticles.PARTICLE_TYPES.register();

		MovementBehaviour.REGISTRY.register(Blocks.LIGHTNING_ROD, new LightningRodMovementBehaviour());
	}

	public static void onRegister(Registry<?> registry) {
		if(registry == BuiltInRegistries.TRIGGER_TYPES) {
			ModdedAdvancements.register();
			PowerGridTriggers.register();
		}
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	public static ResourceLocation texture(String path) {
		return asResource("textures/" + path + ".png");
	}

	@ExpectPlatform
	public static AbstractPowerGridRegistrate createRegistrate() {
		throw new AssertionError();
	}

	public static void registerRecipes() {
		var magnetizing = MagnetizingRecipe.TYPE_INFO;
		RECIPE_SERIALIZERS.register(magnetizing.getId(), magnetizing::getSerializer);
		RECIPE_TYPES.register(magnetizing.getId(), magnetizing::getType);

		RECIPE_SERIALIZERS.register("crafting_special_string_light_cord", () -> StringLightCordRecipe.SERIALIZER);

		RECIPE_SERIALIZERS.register("boost_recipe", () -> BoostRecipe.SERIALIZER);
	}

	@ExpectPlatform
	public static void finalizeRegistrate() {
		throw new AssertionError();
	}

	private static Platform platform = null;

	public enum Platform {
		FABRIC, NEOFORGE
	}

	public static Platform getPlatform() {
		if(platform == null) {
			var str = System.getProperty("powergrid.platform", "auto");
			platform = switch(str.toLowerCase()) {
				case "fabric" -> Platform.FABRIC;
				case "forge" -> Platform.NEOFORGE;
				case "auto" -> {
					if(dev.architectury.platform.Platform.isNeoForge())
						yield Platform.NEOFORGE;
					if(dev.architectury.platform.Platform.isFabric())
						yield Platform.FABRIC;
					throw new IllegalStateException("Cannot detect current platform");
				}
				default -> throw new IllegalArgumentException("Unknown output platform specified, allowed values: [fabric, forge]");
			};
		}
		return platform;
	}

	public static <T> T forPlatform(T fabric, T forge) {
		return switch(getPlatform()) {
			case FABRIC -> fabric;
			case NEOFORGE -> forge;
		};
	}
}