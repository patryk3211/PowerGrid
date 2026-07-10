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
package org.patryk3211.powergrid.data;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.block.CopperBlockSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import org.patryk3211.powergrid.collections.ModdedTags;

import java.util.concurrent.CompletableFuture;

public class BlockTagProvider extends TagsProvider<Block> {
    public BlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BLOCK, registriesFuture);
    }

    public ResourceKey<Block> reverseLookup(Block item) {
        var key = BuiltInRegistries.BLOCK.getResourceKey(item);
        if(key.isEmpty())
            throw new IllegalArgumentException("Item is not registered");
        return key.get();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModdedTags.Block.AFFECTED_BY_LAMP.tag)
                .addOptionalTag(BlockTags.BEE_GROWABLES.location())
                .add(reverseLookup(Blocks.CACTUS))
                .add(reverseLookup(Blocks.SUGAR_CANE));

        tag(ModdedTags.Block.CARBON_PILE_BLOCK.tag)
                .add(reverseLookup(Blocks.COAL_BLOCK));

        var builder = tag(ModdedTags.Block.CONDUCTIVE_GROUND.tag);
        builder
                .add(reverseLookup(Blocks.COPPER_BLOCK))
                .add(reverseLookup(Blocks.EXPOSED_COPPER))
                .add(reverseLookup(Blocks.OXIDIZED_COPPER))
                .add(reverseLookup(Blocks.WEATHERED_COPPER))
                .add(reverseLookup(Blocks.CUT_COPPER))
                .add(reverseLookup(Blocks.EXPOSED_CUT_COPPER))
                .add(reverseLookup(Blocks.OXIDIZED_CUT_COPPER))
                .add(reverseLookup(Blocks.WEATHERED_CUT_COPPER))
                .add(reverseLookup(Blocks.CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.EXPOSED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.OXIDIZED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.WEATHERED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.EXPOSED_CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.OXIDIZED_CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.WEATHERED_CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.WAXED_COPPER_BLOCK))
                .add(reverseLookup(Blocks.WAXED_EXPOSED_COPPER))
                .add(reverseLookup(Blocks.WAXED_OXIDIZED_COPPER))
                .add(reverseLookup(Blocks.WAXED_WEATHERED_COPPER))
                .add(reverseLookup(Blocks.WAXED_CUT_COPPER))
                .add(reverseLookup(Blocks.WAXED_EXPOSED_CUT_COPPER))
                .add(reverseLookup(Blocks.WAXED_OXIDIZED_CUT_COPPER))
                .add(reverseLookup(Blocks.WAXED_WEATHERED_CUT_COPPER))
                .add(reverseLookup(Blocks.WAXED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB))
                .add(reverseLookup(Blocks.WAXED_CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS))
                .add(reverseLookup(Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS))
                .add(reverseLookup(AllBlocks.COPPER_BARS.get()))
                .add(reverseLookup(Blocks.IRON_BLOCK))
                .add(reverseLookup(Blocks.IRON_BARS))
                .add(reverseLookup(Blocks.GOLD_BLOCK))
                .add(reverseLookup(AllBlocks.BRASS_BLOCK.get()))
                .add(reverseLookup(AllBlocks.BRASS_BARS.get()))
                .add(reverseLookup(Blocks.NETHERITE_BLOCK))
                ;
        for(var variant : CopperBlockSet.DEFAULT_VARIANTS) {
            for(var state : WeatheringCopper.WeatherState.values()) {
                builder.add(reverseLookup(AllBlocks.COPPER_TILES.get(variant, state, false).get()));
                builder.add(reverseLookup(AllBlocks.COPPER_TILES.get(variant, state, true).get()));
                builder.add(reverseLookup(AllBlocks.COPPER_SHINGLES.get(variant, state, false).get()));
                builder.add(reverseLookup(AllBlocks.COPPER_SHINGLES.get(variant, state, true).get()));
            }
        }
        var solarQuarterBuilder = tag(ModdedTags.Block.SOLAR_QUARTER_LIGHT.tag);
        var solarHalfBuilder = tag(ModdedTags.Block.SOLAR_HALF_LIGHT.tag);
        var solar3QuarterBuilder = tag(ModdedTags.Block.SOLAR_3QUARTER_LIGHT.tag);
        var solarFullBuilder = tag(ModdedTags.Block.SOLAR_FULL_LIGHT.tag);

        solarQuarterBuilder
                .addOptionalTag(BlockTags.LEAVES.location());

        solarHalfBuilder
                .add(reverseLookup(Blocks.TALL_GRASS))
                .add(reverseLookup(Blocks.GRASS))
                .add(reverseLookup(Blocks.WATER))
                .add(reverseLookup(Blocks.IRON_BARS));

        solar3QuarterBuilder
                .addOptionalTag(ModdedTags.Block.GLASS_BLOCK.tag.location())
                .addOptionalTag(ModdedTags.Block.GLASS_PANE.tag.location());
    }
}
