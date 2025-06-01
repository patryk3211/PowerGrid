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

import com.simibubi.create.AllTags;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import org.patryk3211.powergrid.collections.ModdedTags;

public class RecipeTags {
    public static TagKey<Item> ironSheet() {
        return AllTags.forgeItemTag("iron_plates");
    }

    public static TagKey<Item> copperSheet() {
        return AllTags.forgeItemTag("copper_plates");
    }

    public static TagKey<Item> copperNugget() {
        return AllTags.forgeItemTag("copper_nuggets");
    }

    public static TagKey<Item> ironNugget() {
        return AllTags.forgeItemTag("iron_nuggets");
    }

    public static TagKey<Item> silverSheet() {
        return AllTags.forgeItemTag("silver_plates");
    }

    public static TagKey<Item> silverIngot() {
        return AllTags.forgeItemTag("silver_ingots");
    }
}
