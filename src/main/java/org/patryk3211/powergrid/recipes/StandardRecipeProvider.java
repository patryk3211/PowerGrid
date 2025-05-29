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
import com.simibubi.create.Create;
import com.simibubi.create.foundation.data.recipe.CreateRecipeProvider;
import com.simibubi.create.foundation.data.recipe.StandardRecipeGen;
import com.simibubi.create.foundation.utility.RegisteredObjects;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

public abstract class StandardRecipeProvider extends CreateRecipeProvider {
    public StandardRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    protected RecipeBuilder create(Supplier<ItemConvertible> result) {
        return new RecipeBuilder(result);
    }

    protected RecipeBuilder create(ItemConvertible result) {
        return new RecipeBuilder(() -> result);
    }

    GeneratedRecipe blastCrushedMetal(Supplier<? extends ItemConvertible> result, Supplier<? extends ItemConvertible> ingredient) {
        return create(result::get).suffix("_from_crushed")
                .cooking(ingredient)
                .rewardXP(.1f)
                .inBlastFurnace();
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

        public RecipeBuilder unlockedBy(Supplier<? extends ItemConvertible> unlockedBy) {
            this.unlockedBy = () -> ItemPredicate.Builder.create()
                    .items(unlockedBy.get())
                    .build();
            return this;
        }

        public RecipeBuilder unlockedByTag(Supplier<TagKey<Item>> unlockedBy) {
            this.unlockedBy = () -> ItemPredicate.Builder.create()
                    .tag(unlockedBy.get())
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

        CookingRecipeBuilder cooking(Supplier<? extends ItemConvertible> item) {
            return unlockedBy(item).cookingIngredient(() -> Ingredient.ofItems(item.get()));
        }

        RecipeBuilder.CookingRecipeBuilder cookingTag(Supplier<TagKey<Item>> tag) {
            return unlockedByTag(tag).cookingIngredient(() -> Ingredient.fromTag(tag.get()));
        }

        CookingRecipeBuilder cookingIngredient(Supplier<Ingredient> ingredient) {
            return new RecipeBuilder.CookingRecipeBuilder(ingredient);
        }

        class CookingRecipeBuilder {
            private Supplier<Ingredient> ingredient;
            private float exp;
            private int cookingTime;

            private final RecipeSerializer<? extends AbstractCookingRecipe> FURNACE = RecipeSerializer.SMELTING,
                    SMOKER = RecipeSerializer.SMOKING, BLAST = RecipeSerializer.BLASTING,
                    CAMPFIRE = RecipeSerializer.CAMPFIRE_COOKING;

            CookingRecipeBuilder(Supplier<Ingredient> ingredient) {
                this.ingredient = ingredient;
                cookingTime = 200;
                exp = 0;
            }

            CookingRecipeBuilder forDuration(int duration) {
                cookingTime = duration;
                return this;
            }

            CookingRecipeBuilder rewardXP(float xp) {
                exp = xp;
                return this;
            }

            GeneratedRecipe inFurnace() {
                return inFurnace(b -> b);
            }

            GeneratedRecipe inFurnace(UnaryOperator<CookingRecipeJsonBuilder> builder) {
                return create(FURNACE, builder, 1);
            }

            GeneratedRecipe inSmoker() {
                return inSmoker(b -> b);
            }

            GeneratedRecipe inSmoker(UnaryOperator<CookingRecipeJsonBuilder> builder) {
                create(FURNACE, builder, 1);
                create(CAMPFIRE, builder, 3);
                return create(SMOKER, builder, .5f);
            }

            CreateRecipeProvider.GeneratedRecipe inBlastFurnace() {
                return inBlastFurnace(b -> b);
            }

            GeneratedRecipe inBlastFurnace(UnaryOperator<CookingRecipeJsonBuilder> builder) {
                create(FURNACE, builder, 1);
                return create(BLAST, builder, .5f);
            }

            private GeneratedRecipe create(RecipeSerializer<? extends AbstractCookingRecipe> serializer, UnaryOperator<CookingRecipeJsonBuilder> builder, float cookingTimeModifier) {
                return register(consumer -> {
                    CookingRecipeJsonBuilder b = builder.apply(CookingRecipeJsonBuilder.create(ingredient.get(),
                            RecipeCategory.MISC, result.get(), exp,
                            (int) (cookingTime * cookingTimeModifier), serializer));

                    if (unlockedBy != null)
                        b.criterion("has_item", conditionsFromItemPredicates(unlockedBy.get()));

                    b.offerTo(consumer, createSimpleLocation(RegisteredObjects.getKeyOrThrow(serializer).getPath()));
                });
            }
        }
    }
}
