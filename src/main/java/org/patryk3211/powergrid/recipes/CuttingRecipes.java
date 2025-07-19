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
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.electricity.info.Power;

import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class CuttingRecipes extends ProcessingRecipeGen {
    GeneratedRecipe

    COPPER_WIRE = create(AllItems.COPPER_SHEET::get, b ->
            b.duration(50).output(ModdedItems.WIRE.get(), 4)),

    IRON_WIRE = create(AllItems.IRON_SHEET::get, b ->
            b.duration(50).output(ModdedItems.IRON_WIRE.get(), 4)),

    GOLD_WIRE = create(AllItems.GOLDEN_SHEET::get, b ->
            b.duration(50).output(ModdedItems.GOLDEN_WIRE.get(), 4)),

    EMPTY_CIRCUIT = create("empty_circuit_slabs", b -> b
            .output(ModdedItems.EMPTY_CIRCUIT, 2)
            .require(ItemTags.WOODEN_SLABS)
            .duration(50))

            ;

    public CuttingRecipes(FabricDataOutput output) {
        super(output);
    }

    protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(Supplier<ItemConvertible> singleIngredient, UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
        return super.create(PowerGrid.MOD_ID, singleIngredient, transform);
    }

    protected <T extends ProcessingRecipe<?>> GeneratedRecipe create(String name, UnaryOperator<ProcessingRecipeBuilder<T>> transform) {
        return super.create(PowerGrid.asResource(name), transform);
    }

    @Override
    protected IRecipeTypeInfo getRecipeType() {
        return AllRecipeTypes.CUTTING;
    }
}
