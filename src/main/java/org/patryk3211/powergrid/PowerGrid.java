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
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import org.patryk3211.powergrid.circuits.components.Components;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.electricity.heater.HeaterFanProcessingTypes;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.equipment.thunder.LightningRodMovementBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerGrid  {
	public static final String MOD_ID = "powergrid";

	public static final Logger LOGGER = LoggerFactory.getLogger("PowerGrid");

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(MOD_ID, Registries.RECIPE_SERIALIZER);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(MOD_ID, Registries.RECIPE_TYPE);
	public static final DeferredRegister<FanProcessingType> FAN_PROCESSING_TYPES = DeferredRegister.create(MOD_ID, CreateRegistries.FAN_PROCESSING_TYPE);

	public static AbstractPowerGridRegistrate REGISTRATE;

	public static void init() {
		LOGGER.info("Power grid starting, prepare to be electrocuted");
		ElectricalNetwork.LOGGER = LOGGER;

		ModdedSoundEvents.prepare();

		REGISTRATE = createRegistrate();

		register();

		registerArchitecturyEvents();

		ModdedPackets.registerPackets();
	}

	public static void registerArchitecturyEvents() {
		TickEvent.ServerLevelTick.SERVER_LEVEL_PRE.register(GlobalElectricNetworks::tick);
		LifecycleEvent.SERVER_LEVEL_UNLOAD.register(GlobalElectricNetworks::unloadWorld);
	}

	private static void register() {
		registerRecipes();
		HeaterFanProcessingTypes.register();

		ModdedBlocks.register();
		ModdedItems.register();
		ModdedFluids.register();
		ModdedBlockEntities.register();
		ModdedEntities.register();
		ModdedConfigs.register();
		ModdedMenus.register();
		Components.register();

		ModdedParticles.register();

		finalizeRegistrate();
		RECIPE_SERIALIZERS.register();
		RECIPE_TYPES.register();
		FAN_PROCESSING_TYPES.register();
		ModdedParticles.PARTICLE_TYPES.register();

		MovementBehaviour.REGISTRY.register(Blocks.LIGHTNING_ROD, new LightningRodMovementBehaviour());
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(MOD_ID, path);
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
	}

	@ExpectPlatform
	public static void finalizeRegistrate() {
		throw new AssertionError();
	}

	private static Platform platform = null;

	public enum Platform {
		FABRIC, FORGE
	}

	public static Platform getPlatform() {
		if(platform == null) {
			var str = System.getProperty("powergrid.platform", "auto");
			platform = switch(str.toLowerCase()) {
				case "fabric" -> Platform.FABRIC;
				case "forge" -> Platform.FORGE;
				case "auto" -> {
					if(dev.architectury.platform.Platform.isForge())
						yield Platform.FORGE;
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
			case FORGE -> forge;
		};
	}
}