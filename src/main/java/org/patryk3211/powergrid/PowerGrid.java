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

import com.simibubi.create.AllMovementBehaviours;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.ModInitializer;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.Components;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.electricity.heater.HeaterFanProcessingTypes;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.equipment.thunder.LightningRodMovementBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerGrid implements ModInitializer {
	public static final String MOD_ID = "powergrid";

	public static final Logger LOGGER = LoggerFactory.getLogger("PowerGrid");

//	public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
//			.icon(() -> new ItemStack(ModdedItems.WIRE))
//			.displayName(Text.translatable("itemGroup.powergrid.main"))
//			.build();
//	public static RegistryKey<ItemGroup> ITEM_GROUP_KEY;

	public static AbstractPowerGridRegistrate REGISTRATE;

	@Override
	public void onInitialize() {
		LOGGER.info("Power grid starting, prepare to be electrocuted");
		ElectricalNetwork.LOGGER = LOGGER;

		ComponentRegistry.init();
		ModdedSoundEvents.prepare();

//		Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), ITEM_GROUP);
//		ITEM_GROUP_KEY = Registries.ITEM_GROUP.getKey(ITEM_GROUP).get();

		REGISTRATE = createRegistrate();

		register();

		GlobalElectricNetworks.init();
		ModdedPackets.registerPackets();
	}

	private static void register() {
		registerRecipes();

		ModdedBlocks.register();
		ModdedItems.register();
		ModdedFluids.register();
		ModdedBlockEntities.register();
		ModdedEntities.register();
		HeaterFanProcessingTypes.register();
		ModdedConfigs.register();
		ModdedMenus.register();
		Components.register();

		ModdedParticles.register();

		finalizeRegistrate();

		ModdedSoundEvents.register();

		AllMovementBehaviours.registerBehaviour(Blocks.LIGHTNING_ROD, new LightningRodMovementBehaviour());
	}

	private static void registerRecipes() {
		var magnetizing = MagnetizingRecipe.TYPE_INFO;
		// TODO: Fix
//		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, magnetizing.getId(), magnetizing.getSerializer());
//		Registry.register(BuiltInRegistries.RECIPE_TYPE, magnetizing.getId(), magnetizing.getType());
	}

	public static Identifier asResource(String path) {
		return new Identifier(MOD_ID, path);
	}

	public static Identifier texture(String path) {
		return asResource("textures/" + path + ".png");
	}

	@ExpectPlatform
	public static AbstractPowerGridRegistrate createRegistrate() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void finalizeRegistrate() {
		throw new AssertionError();
	}
}