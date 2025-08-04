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

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

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

    public static TagKey<Item> ironIngot() {
        return AllTags.forgeItemTag("iron_ingots");
    }

    public static TagKey<Item> zincSheet() {
        return AllTags.forgeItemTag("zinc_plates");
    }

    public static TagKey<Item> zincIngot() {
        return AllTags.forgeItemTag("zinc_ingots");
    }

    public static TagKey<Item> copperCoil() {
        return AllTags.forgeItemTag("copper_coils");
    }

    public static TagKey<Item> coal() {
        return ItemTags.COALS;
    }

    public static TagKey<Item> planks() {
        return ItemTags.PLANKS;
    }

    public static TagKey<Item> brassSheet() {
        return AllTags.forgeItemTag("brass_plates");
    }

    public static ItemConvertible conductiveCasing() {
        return ModdedBlocks.CONDUCTIVE_CASING;
    }

    public static ItemConvertible transformerCore() {
        return ModdedBlocks.TRANSFORMER_CORE;
    }

    public static ItemConvertible shaft() {
        return AllBlocks.SHAFT;
    }

    public static ItemConvertible andesiteCasing() {
        return AllBlocks.ANDESITE_CASING;
    }

    public static ItemConvertible resistiveCoil() {
        return ModdedItems.RESISTIVE_COIL;
    }

    public static ItemConvertible electricalGizmo() {
        return ModdedItems.ELECTRICAL_GIZMO;
    }
}
