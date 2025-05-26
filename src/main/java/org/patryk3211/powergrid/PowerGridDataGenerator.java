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

import com.simibubi.create.foundation.ponder.PonderLocalization;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.simibubi.create.infrastructure.ponder.GeneralText;
import com.simibubi.create.infrastructure.ponder.SharedText;
import com.tterrag.registrate.providers.ProviderType;
import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.patryk3211.powergrid.ponder.PonderIndex;
import org.patryk3211.powergrid.recipes.*;

import java.util.function.BiConsumer;

public class PowerGridDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		var pack = generator.createPack();
		var helper = ExistingFileHelper.withResourcesFromArg();
		PowerGrid.REGISTRATE.setupDatagen(pack, helper);

		pack.addProvider(SequencedAssemblyRecipes::new);
		pack.addProvider(CuttingRecipes::new);
		pack.addProvider(CraftingRecipes::new);
		pack.addProvider(MechanicalCraftingRecipes::new);

		PowerGrid.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
			BiConsumer<String, String> langConsumer = provider::add;
			provideDefaultLang("interface", langConsumer);
			provideDefaultLang("messages", langConsumer);

			providePonderLang(langConsumer);
		});
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
		PonderIndex.register();

		SharedText.gatherText();
		PonderLocalization.generateSceneLang();

		PonderLocalization.provideLang(PowerGrid.MOD_ID, consumer);
	}
}
