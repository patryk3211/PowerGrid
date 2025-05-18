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
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.StandardRecipeGen;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedTags;

import java.util.function.UnaryOperator;

import static com.simibubi.create.AllTags.forgeItemTag;

public class CraftingRecipes extends CreateRecipeProvider {
    GeneratedRecipe

        GENERATOR_COIL = create(ModdedBlocks.COIL)
            .unlockedBy(ModdedItems.WIRE::get)
            .shaped(b -> b
                    .pattern("WWW")
                    .pattern("WIW")
                    .pattern("WIW")
                    .input('W', ModdedTags.Item.COIL_WIRE.tag)
                    .input('I', RecipeTags.ironSheet())
            ),

    WIRE_CONNECTOR = create(ModdedBlocks.WIRE_CONNECTOR)
            .unlockedBy(AllItems.ANDESITE_ALLOY::get)
            .shaped(b -> b
                    .pattern(" C ")
                    .pattern("CAC")
                    .input('C', RecipeTags.copperNugget())
                    .input('A', AllItems.ANDESITE_ALLOY)
            ),

    LIGHT_BULB = create(ModdedItems.LIGHT_BULB)
            .unlockedBy(ModdedBlocks.LIGHT_FIXTURE::get)
            .shaped(b -> b
                    .pattern(" G ")
                    .pattern("GWG")
                    .pattern(" I ")
                    .input('G', Items.GLASS_PANE)
                    .input('W', ModdedItems.IRON_WIRE)
                    .input('I', RecipeTags.ironSheet())
            ),

    RESISTIVE_COIL = create(ModdedItems.RESISTIVE_COIL)
            .unlockedBy(ModdedItems.IRON_WIRE::get)
            .shapeless(b -> b
                    .input(ModdedItems.IRON_WIRE, 4)
                    .input(Items.STICK, 1)
            ),

    HEATING_COIL = create(ModdedBlocks.HEATING_COIL)
            .unlockedBy(ModdedItems.RESISTIVE_COIL::get)
            .shaped(b -> b
                    .pattern("C C")
                    .pattern("IRI")
                    .pattern("IRI")
                    .input('C', RecipeTags.copperNugget())
                    .input('I', RecipeTags.ironSheet())
                    .input('R', ModdedItems.RESISTIVE_COIL)
            ),

    GENERATOR_HOUSING = create(ModdedBlocks.GENERATOR_HOUSING)
            .unlockedBy(AllItems.IRON_SHEET::get)
            .shaped(b -> b
                    .pattern("II")
                    .pattern(" I")
                    .input('I', RecipeTags.ironSheet())
            ),

    ANDESITE_VOLTAGE_GAUGE = create(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .shaped(b -> b
                    .pattern("NCN")
                    .pattern(" A ")
                    .input('N', RecipeTags.copperNugget())
                    .input('A', AllBlocks.ANDESITE_CASING)
                    .input('C', Items.COMPASS)
            ),

    BRASS_VOLTAGE_GAUGE = create(ModdedBlocks.BRASS_VOLTAGE_METER)
            .unlockedBy(AllBlocks.BRASS_CASING::get)
            .shaped(b -> b
            .pattern("NCN")
            .pattern(" B ")
                    .input('N', RecipeTags.copperNugget())
            .input('B', AllBlocks.BRASS_CASING)
                    .input('C', Items.COMPASS)
            ),

    ANDESITE_CURRENT_GAUGE = create(ModdedBlocks.ANDESITE_CURRENT_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .shapeless(b -> b.input(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            ),

    BRASS_CURRENT_GAUGE = create(ModdedBlocks.BRASS_CURRENT_METER)
            .unlockedBy(AllBlocks.BRASS_CASING::get)
            .shapeless(b -> b.input(ModdedBlocks.BRASS_VOLTAGE_METER)
            ),

    ANDESITE_VOLTAGE_GAUGE_BACK = create(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .suffix("_convert")
            .shapeless(b -> b.input(ModdedBlocks.ANDESITE_CURRENT_METER)
            ),

    BRASS_VOLTAGE_GAUGE_BACK = create(ModdedBlocks.BRASS_VOLTAGE_METER)
            .unlockedBy(AllBlocks.BRASS_CASING::get)
            .suffix("_convert")
            .shapeless(b -> b.input(ModdedBlocks.BRASS_CURRENT_METER)
            ),

    LIGHT_FIXTURE = create(ModdedBlocks.LIGHT_FIXTURE)
            .unlockedBy(AllBlocks.ANDESITE_CASING::get)
            .shaped(b -> b
                    .pattern(" I ")
                    .pattern("CAC")
                    .input('I', RecipeTags.ironSheet())
                    .input('C', RecipeTags.copperNugget())
                    .input('A', AllBlocks.ANDESITE_CASING)
            )
            ;

    public CraftingRecipes(FabricDataOutput output) {
        super(output);
    }

    protected RecipeBuilder create(Supplier<ItemConvertible> result) {
        return new RecipeBuilder(result);
    }

    protected RecipeBuilder create(ItemConvertible result) {
        return new RecipeBuilder(() -> result);
    }

    /**
     * @see StandardRecipeGen.GeneratedRecipeBuilder
     */
    protected class RecipeBuilder {
        private final Supplier<ItemConvertible> result;
        private int amount;
        private Supplier<ItemPredicate> unlockedBy;
        private String suffix;

        public RecipeBuilder(Supplier<ItemConvertible> result) {
            this.result = result;
            this.amount = 1;
            this.unlockedBy = null;
            this.suffix = "";
        }

        public RecipeBuilder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public RecipeBuilder suffix(String nameSuffix) {
            this.suffix = nameSuffix;
            return this;
        }

        public RecipeBuilder unlockedBy(Supplier<ItemConvertible> unlockedBy) {
            this.unlockedBy = () -> ItemPredicate.Builder.create()
                    .items(unlockedBy.get())
                    .build();
            return this;
        }

        public GeneratedRecipe shaped(UnaryOperator<ShapedRecipeJsonBuilder> builder) {
            return register(consumer -> {
                ShapedRecipeJsonBuilder b = builder.apply(ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, result.get(), amount));
                if(unlockedBy != null)
                    b.criterion("has_item", conditionsFromItemPredicates(unlockedBy.get()));
                b.offerTo(consumer, createLocation("crafting"));
            });
        }

        public GeneratedRecipe shapeless(UnaryOperator<ShapelessRecipeJsonBuilder> builder) {
            return register(consumer -> {
                ShapelessRecipeJsonBuilder b = builder.apply(ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, result.get(), amount));
                if (unlockedBy != null)
                    b.criterion("has_item", conditionsFromItemPredicates(unlockedBy.get()));

                b.offerTo(result -> {
                    consumer.accept(result);
//                    consumer.accept(
//                            !recipeConditions.isEmpty() ? new StandardRecipeGen.ConditionSupportingShapelessRecipeResult(result, recipeConditions)
//                                    : result);
                }, createLocation("crafting"));
            });
        }

        private Identifier createSimpleLocation(String recipeType) {
            return Create.asResource(recipeType + "/" + getRegistryName().getPath() + suffix);
        }

        private Identifier createLocation(String recipeType) {
            return Create.asResource(recipeType + "/" + getRegistryName().getPath() + suffix);
        }

        private Identifier getRegistryName() {
            return RegisteredObjects.getKeyOrThrow(result.get().asItem());
        }
    }
}
