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

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.ProcessingRecipeGen;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedFluids;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.function.UnaryOperator;

public class MixingRecipes extends ProcessingRecipeGen {
    GeneratedRecipe

    ACID = create("acid", b -> b
            .require(Items.REDSTONE)
            .require(Items.BLAZE_POWDER)
            .require(Fluids.WATER, FluidConstants.BOTTLE)
            .requiresHeat(HeatCondition.HEATED)
            .output(ModdedFluids.ACID.getSource(), FluidConstants.BOTTLE)),

    ETCHED_CIRCUIT_BOARD = create("etched_circuit_board", b -> b
            .require(ModdedItems.UNETCHED_CIRCUIT)
            .require(ModdedFluids.ACID.getSource(), FluidConstants.BOTTLE)
            .requiresHeat(HeatCondition.HEATED)
            .output(ModdedItems.INCOMPLETE_CIRCUIT))
    ;

    public MixingRecipes(FabricDataOutput output) {
        super(output);
    }

    <T extends ProcessingRecipe<?>> GeneratedRecipe create(String name, UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
        return create(PowerGrid.asResource(name), transform);
    }

    @Override
    protected AllRecipeTypes getRecipeType() {
        return AllRecipeTypes.MIXING;
    }
}
