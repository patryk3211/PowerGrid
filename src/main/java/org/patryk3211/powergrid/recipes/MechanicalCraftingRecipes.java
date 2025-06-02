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

import com.google.common.base.Supplier;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.item.ItemConvertible;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import java.util.function.UnaryOperator;

public class MechanicalCraftingRecipes extends CreateRecipeProvider {
    GeneratedRecipe

    ELECTRIC_MOTOR = create(ModdedBlocks.ELECTRIC_MOTOR::get)
            .recipe(b -> b
                    .key('C', ModdedItems.COPPER_COIL)
                    .key('M', ModdedItems.MAGNET)
                    .key('I', RecipeTags.ironSheet())
                    .key('S', AllBlocks.SHAFT)
                    .patternLine(" ICI ")
                    .patternLine("CMSMC")
                    .patternLine(" ICI ")
            ),

    GENERATOR_ROTOR = create(ModdedBlocks.GENERATOR_ROTOR::get)
            .recipe(b -> b
                    .key('A', AllItems.ANDESITE_ALLOY)
                    .key('M', ModdedItems.MAGNET)
                    .key('S', AllBlocks.SHAFT)
                    .key('C', AllBlocks.ANDESITE_CASING)
                    .patternLine(" C ")
                    .patternLine("AMA")
                    .patternLine("MSM")
                    .patternLine(" M ")
            )

            ;


    public MechanicalCraftingRecipes(FabricDataOutput output) {
        super(output);
    }

    GeneratedRecipeBuilder create(Supplier<ItemConvertible> result) {
        return new GeneratedRecipeBuilder(result);
    }

    @Override
    public String getName() {
        return "Power Grid's Mechanical Crafting Recipes";
    }

    public class GeneratedRecipeBuilder {
        private final Supplier<ItemConvertible> result;
        private String suffix;
        private int amount;

        public GeneratedRecipeBuilder(Supplier<ItemConvertible> result) {
            this.suffix = "";
            this.result = result;
            this.amount = 1;
        }

        GeneratedRecipeBuilder returns(int amount) {
            this.amount = amount;
            return this;
        }

        GeneratedRecipeBuilder withSuffix(String suffix) {
            this.suffix = suffix;
            return this;
        }

        GeneratedRecipe recipe(UnaryOperator<MechanicalCraftingRecipeBuilder> builder) {
            return register(consumer -> {
                var b = builder.apply(MechanicalCraftingRecipeBuilder.shapedRecipe(result.get(), amount));
                var location = PowerGrid.asResource("mechanical_crafting/" + RegisteredObjects.getKeyOrThrow(result.get().asItem()).getPath() + suffix);
                b.build(consumer, location);
            });
        }
    }
}
