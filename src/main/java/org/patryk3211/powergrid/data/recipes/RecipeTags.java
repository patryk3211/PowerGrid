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
import com.simibubi.create.AllItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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

    public static TagKey<Item> copperIngot() {
        return ingots("copper");
    }

    public static TagKey<Item> copperWire() {
        return wires("copper");
    }

    public static TagKey<Item> ironNugget() {
        return nuggets("iron");
    }

    public static TagKey<Item> ironIngot() {
        return ingots("iron");
    }

    public static TagKey<Item> ironWire() {
        return wires("iron");
    }

    public static TagKey<Item> zincSheet() {
        return plates("zinc");
    }

    public static TagKey<Item> zincIngot() {
        return ingots("zinc");
    }

    public static TagKey<Item> goldSheet() {
        return plates("gold");
    }

    public static TagKey<Item> goldWire() {
        return wires("gold");
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

    public static ItemLike cardboard() {
        return AllItems.CARDBOARD;
    }

    public static ItemLike andesiteAlloy() {
        return AllItems.ANDESITE_ALLOY;
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

    public static ItemLike coalBlock() {
        return Items.COAL_BLOCK;
    }

    public static ItemLike voltageMeter() {
        return ModdedBlocks.VOLTAGE_METER;
    }

    public static ItemLike currentMeter() {
        return ModdedBlocks.CURRENT_METER;
    }

    public static ItemLike polishedRoseQuartz() {
        return AllItems.POLISHED_ROSE_QUARTZ;
    }

    public static ItemLike glowstoneDust() {
        return Items.GLOWSTONE_DUST;
    }

    public static ItemLike amethystShard() {
        return Items.AMETHYST_SHARD;
    }

    public static ItemLike glass() {
        return Items.GLASS;
    }

    public static ItemLike paper() {
        return Items.PAPER;
    }

    public static ItemLike electronTube() {
        return AllItems.ELECTRON_TUBE.get();
    }

    public static ItemLike precisionMechanism() {
        return AllItems.PRECISION_MECHANISM;
    }

    public static ItemLike smallCog() {
        return AllBlocks.COGWHEEL.get();
    }

    public static ItemLike quartz() {
        return Items.QUARTZ;
    }

    public static ItemLike redstone() {
        return Items.REDSTONE;
    }

    public static ItemLike diamond() {
        return Items.DIAMOND;
    }
}
