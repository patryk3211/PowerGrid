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
package org.patryk3211.powergrid.data.recipes;

import com.simibubi.create.AllBlocks;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import static org.patryk3211.powergrid.collections.ModdedTags.*;

public class RecipeTags {
    public static TagKey<Item> ironSheet() {
        return plates("iron");
    }

    public static TagKey<Item> copperSheet() {
        return plates("copper");
    }

    public static TagKey<Item> copperNugget() {
        return nuggets("copper");
    }

    public static TagKey<Item> ironNugget() {
        return nuggets("iron");
    }

    public static TagKey<Item> ironIngot() {
        return ingots("iron");
    }

    public static TagKey<Item> zincSheet() {
        return plates("zinc");
    }

    public static TagKey<Item> zincIngot() {
        return ingots("zinc");
    }

    public static TagKey<Item> copperCoil() {
        return forgeItemTag("copper_coils");
    }

    public static TagKey<Item> coal() {
        return ItemTags.COALS;
    }

    public static TagKey<Item> planks() {
        return ItemTags.PLANKS;
    }

    public static TagKey<Item> brassSheet() {
        return plates("brass");
    }

    public static ItemLike conductiveCasing() {
        return ModdedBlocks.CONDUCTIVE_CASING;
    }

    public static ItemLike transformerCore() {
        return ModdedBlocks.TRANSFORMER_CORE;
    }

    public static ItemLike shaft() {
        return AllBlocks.SHAFT;
    }

    public static ItemLike andesiteCasing() {
        return AllBlocks.ANDESITE_CASING;
    }

    public static ItemLike resistiveCoil() {
        return ModdedItems.RESISTIVE_COIL;
    }

    public static ItemLike electricalGizmo() {
        return ModdedItems.ELECTRICAL_GIZMO;
    }
}
