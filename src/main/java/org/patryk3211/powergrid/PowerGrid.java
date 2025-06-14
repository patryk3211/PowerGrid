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

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.chemistry.electrolysis.ElectrolysisRecipe;
import org.patryk3211.powergrid.chemistry.electrolysis.ElectrolysisRecipeSerializer;
import org.patryk3211.powergrid.chemistry.reagent.ReagentRegistry;
import org.patryk3211.powergrid.chemistry.reagent.Reagents;
import org.patryk3211.powergrid.chemistry.recipe.ReactionRecipe;
import org.patryk3211.powergrid.chemistry.recipe.ReactionRecipeSerializer;
import org.patryk3211.powergrid.collections.*;
import org.patryk3211.powergrid.electricity.GlobalElectricNetworks;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.electricity.heater.HeaterFanProcessingTypes;
import org.patryk3211.powergrid.electricity.info.ElectricProperties;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.network.ServerBoundPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerGrid implements ModInitializer {
	public static final String MOD_ID = "powergrid";

	public static final Logger LOGGER = LoggerFactory.getLogger("PowerGrid");

	public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(ModdedItems.WIRE))
			.displayName(Text.translatable("itemGroup.powergrid.main"))
			.build();
	public static RegistryKey<ItemGroup> ITEM_GROUP_KEY;

	public static PowerGridRegistrate REGISTRATE;

	@Override
	public void onInitialize() {
		LOGGER.info("Power grid starting, prepare to be electrocuted");
		ElectricalNetwork.LOGGER = LOGGER;

		ReagentRegistry.init();
		ModdedSoundEvents.prepare();

		Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"), ITEM_GROUP);
		ITEM_GROUP_KEY = Registries.ITEM_GROUP.getKey(ITEM_GROUP).get();

		registerRecipes();

		REGISTRATE = PowerGridRegistrate.create(MOD_ID)
				.defaultCreativeTab(ITEM_GROUP_KEY)
				.setTooltipModifierFactory(item ->
						new ItemDescription.Modifier(item, TooltipHelper.Palette.STANDARD_CREATE)
								.andThen(TooltipModifier.mapNull(KineticStats.create(item)))
								.andThen(TooltipModifier.mapNull(ElectricProperties.create(item)))
				);

		ModdedBlocks.register();
		ModdedItems.register();
		ModdedBlockEntities.register();
		ModdedEntities.register();
		HeaterFanProcessingTypes.register();
		ModdedConfigs.register();
		Reagents.register();

		ModdedParticles.register();

		REGISTRATE.register();
		ModdedSoundEvents.register();

		GlobalElectricNetworks.init();
		ServerBoundPackets.init();
	}

	private static void registerRecipes() {
		Registry.register(Registries.RECIPE_SERIALIZER, ReactionRecipe.ID, ReactionRecipeSerializer.INSTANCE);
		Registry.register(Registries.RECIPE_SERIALIZER, ElectrolysisRecipe.ID, ElectrolysisRecipeSerializer.INSTANCE);
		Registry.register(Registries.RECIPE_TYPE, ReactionRecipe.ID, ReactionRecipe.TYPE);
		Registry.register(Registries.RECIPE_TYPE, ElectrolysisRecipe.ID, ElectrolysisRecipe.TYPE);

		var magnetizing = MagnetizingRecipe.TYPE_INFO;
		Registry.register(Registries.RECIPE_SERIALIZER, magnetizing.getId(), magnetizing.getSerializer());
		Registry.register(Registries.RECIPE_TYPE, magnetizing.getId(), magnetizing.getType());
	}

	public static Identifier asResource(String path) {
		return new Identifier(MOD_ID, path);
	}

	public static Identifier texture(String path) {
		return asResource("textures/" + path + ".png");
	}
}