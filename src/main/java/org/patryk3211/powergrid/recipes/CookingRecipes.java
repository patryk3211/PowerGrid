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
package org.patryk3211.powergrid.recipes;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import org.patryk3211.powergrid.collections.ModdedItems;

public class CookingRecipes extends StandardRecipeProvider {
    GeneratedRecipe

    SILVER_ORE = create(ModdedItems.SILVER_INGOT)
            .suffix("_ore")
            .cookingTag(() -> AllTags.forgeItemTag("silver_ores"))
            .rewardXP(1f)
            .inBlastFurnace(),

    RAW_SILVER = create(ModdedItems.SILVER_INGOT)
            .suffix("_raw")
            .cookingTag(() -> AllTags.forgeItemTag("raw_silver_ores"))
            .rewardXP(.7f)
            .inBlastFurnace(),

    CRUSHED_SILVER = blastCrushedMetal(ModdedItems.SILVER_INGOT::get, AllItems.CRUSHED_SILVER::get)
            ;

    public CookingRecipes(FabricDataOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "Power Grid's Cooking Recipes";
    }
}
