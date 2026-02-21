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
package org.patryk3211.powergrid.data.recipe.forge;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedFluids;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.data.FluidConstants;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class MixingRecipes<P extends ProcessingRecipeParams,R extends ProcessingRecipe<?, P>,B extends ProcessingRecipeBuilder<P, R, B>> extends ProcessingRecipeGen<P,R,B> {
    GeneratedRecipe

    ACID = create("acid", b -> b
            .require(Items.REDSTONE)
            .require(Items.BLAZE_POWDER)
            .require(Fluids.WATER, FluidConstants.BOTTLE)
            .requiresHeat(HeatCondition.HEATED)
            .output(ModdedFluids.acid(), FluidConstants.BOTTLE)),

    ETCHED_CIRCUIT_BOARD = create("etched_circuit_board", b -> b
            .require(ModdedItems.UNETCHED_CIRCUIT)
            .require((FlowingFluid) ModdedFluids.acid(), FluidConstants.BOTTLE)
            .requiresHeat(HeatCondition.HEATED)
            .output(ModdedItems.INCOMPLETE_CIRCUIT))
    ;

    public MixingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, PowerGrid.MOD_ID);
    }

    @Override
    protected AllRecipeTypes getRecipeType() {
        return AllRecipeTypes.MIXING;
    }

    @Override
    protected B getBuilder(ResourceLocation id) {
        return null;
    }
}
