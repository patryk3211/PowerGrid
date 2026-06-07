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
package org.patryk3211.powergrid.fabric;

import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.DataProvider;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.data.BlockTagProvider;
import org.patryk3211.powergrid.data.EntityHandlerProvider;
import org.patryk3211.powergrid.data.ItemTagProvider;
import org.patryk3211.powergrid.data.recipe.fabric.MixingRecipes;
import org.patryk3211.powergrid.data.recipes.*;
import org.patryk3211.powergrid.ponder.PowerGridPonderPlugin;

import java.util.function.BiConsumer;

public class PowerGridDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		var pack = generator.createPack();
		var helper = ExistingFileHelper.withResourcesFromArg();
		PowerGrid.REGISTRATE.setupDatagen(pack, helper);

		addProvider(pack, SequencedAssemblyRecipes::new);
		addProvider(pack, org.patryk3211.powergrid.data.recipe.fabric.SequencedAssemblyRecipes::new);
		addProvider(pack, CuttingRecipes::new);
		addProvider(pack, CraftingRecipes::new);
		addProvider(pack, CookingRecipes::new);
		addProvider(pack, MechanicalCraftingRecipes::new);
		addProvider(pack, MixingRecipes::new);
		addProvider(pack, PressingRecipes::new);
		addProvider(pack, ModdedSoundEvents::provider);
		addProvider(pack, MagnetizingRecipes::new);
		pack.addProvider(BlockTagProvider::new);
		pack.addProvider(ItemTagProvider::new);
		pack.addProvider((FabricDataGenerator.Pack.Factory<EntityHandlerProvider>) EntityHandlerProvider::new);
		addProvider(pack, ItemApplicationRecipes::new);
		addProvider(pack, DeployerApplicationRecipes::new);

		PowerGrid.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
			BiConsumer<String, String> langConsumer = provider::add;
			provideDefaultLang("interface", langConsumer);
			provideDefaultLang("messages", langConsumer);
			provideDefaultLang("tooltips", langConsumer);
			provideDefaultLang("components", langConsumer);
			provideDefaultLang("pads", langConsumer);

			providePonderLang(langConsumer);
			ModdedSoundEvents.provideLang(langConsumer);
		});
	}

	private static void addProvider(FabricDataGenerator.Pack pack, FabricDataGenerator.Pack.Factory<DataProvider> factory) {
		pack.addProvider(factory);
	}

	/**
	 * @see com.simibubi.create.infrastructure.data.CreateDatagen#provideDefaultLang(String, BiConsumer)
	 */
	private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
		var path = "assets/powergrid/lang/default/" + fileName + ".json";
		var jsonElement = FilesHelper.loadJsonResource(path);
		if (jsonElement == null) {
			throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
		}
		var jsonObject = jsonElement.getAsJsonObject();
		for(var entry : jsonObject.entrySet()) {
			var key = entry.getKey();
			var value = entry.getValue().getAsString();
			consumer.accept(key, value);
		}
	}

	private static void providePonderLang(BiConsumer<String, String> consumer) {
		PonderIndex.addPlugin(new PowerGridPonderPlugin());
		PonderIndex.getLangAccess().provideLang(PowerGrid.MOD_ID, consumer);
	}
}
