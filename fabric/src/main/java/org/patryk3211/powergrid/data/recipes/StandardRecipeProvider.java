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

import com.google.common.base.Supplier;
import com.google.gson.JsonObject;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.createmod.catnip.platform.CatnipServices;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
import net.minecraft.data.server.recipe.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * @see com.simibubi.create.foundation.data.recipe.CreateStandardRecipeGen
 */
public abstract class StandardRecipeProvider extends BaseRecipeProvider {
    public StandardRecipeProvider(FabricDataOutput output) {
        super(output, PowerGrid.MOD_ID);
    }

    static class Marker {
	}

	String currentFolder = "";

	Marker enterFolder(String folder) {
		currentFolder = folder;
		return new Marker();
	}

	GeneratedRecipeBuilder create(Supplier<ItemConvertible> result) {
		return new GeneratedRecipeBuilder(currentFolder, result);
	}

	GeneratedRecipeBuilder create(Identifier result) {
		return new GeneratedRecipeBuilder(currentFolder, result);
	}

	GeneratedRecipeBuilder create(ItemProviderEntry<? extends ItemConvertible> result) {
		return create(result::get);
	}

	GeneratedRecipe createSpecial(Supplier<? extends SpecialRecipeSerializer<?>> serializer, String recipeType,
                                  String path) {
		Identifier location = Create.asResource(recipeType + "/" + currentFolder + "/" + path);
		return register(consumer -> {
			ComplexRecipeJsonBuilder b = ComplexRecipeJsonBuilder.create(serializer.get());
			b.offerTo(consumer, location.toString());
		});
	}

	GeneratedRecipe conversionCycle(List<ItemProviderEntry<? extends ItemConvertible>> cycle) {
		GeneratedRecipe result = null;
		for (int i = 0; i < cycle.size(); i++) {
			ItemProviderEntry<? extends ItemConvertible> currentEntry = cycle.get(i);
			ItemProviderEntry<? extends ItemConvertible> nextEntry = cycle.get((i + 1) % cycle.size());
			result = create(nextEntry).withSuffix("_from_conversion")
				.unlockedBy(currentEntry::get)
				.viaShapeless(b -> b.input(currentEntry.get()));
		}
		return result;
	}

	GeneratedRecipe clearData(ItemProviderEntry<? extends ItemConvertible> item) {
		return create(item).withSuffix("_clear")
			.unlockedBy(item::get)
			.viaShapeless(b -> b.input(item.get()));
	}

	@Override
	public void generate(Consumer<RecipeJsonProvider> p_200404_1_) {
		all.forEach(c -> c.register(p_200404_1_));
		Create.LOGGER.info(getName() + " registered " + all.size() + " recipe" + (all.size() == 1 ? "" : "s"));
	}

	protected GeneratedRecipe register(GeneratedRecipe recipe) {
		all.add(recipe);
		return recipe;
	}

	class GeneratedRecipeBuilder {
		private String path;
		private String suffix;
		private Supplier<? extends ItemConvertible> result;
		private Identifier compatDatagenOutput;
		List<ConditionJsonProvider> recipeConditions;

		private Supplier<ItemPredicate> unlockedBy;
		private int amount;

		private GeneratedRecipeBuilder(String path) {
			this.path = path;
			this.recipeConditions = new ArrayList<>();
			this.suffix = "";
			this.amount = 1;
		}

		public GeneratedRecipeBuilder(String path, Supplier<? extends ItemConvertible> result) {
			this(path);
			this.result = result;
		}

		public GeneratedRecipeBuilder(String path, Identifier result) {
			this(path);
			this.compatDatagenOutput = result;
		}

		GeneratedRecipeBuilder returns(int amount) {
			this.amount = amount;
			return this;
		}

		GeneratedRecipeBuilder unlockedBy(Supplier<? extends ItemConvertible> item) {
			this.unlockedBy = () -> ItemPredicate.Builder.create()
				.items(item.get())
				.build();
			return this;
		}

		GeneratedRecipeBuilder unlockedByTag(Supplier<TagKey<Item>> tag) {
			this.unlockedBy = () -> ItemPredicate.Builder.create()
				.tag(tag.get())
				.build();
			return this;
		}

		GeneratedRecipeBuilder whenModLoaded(String modid) {
			return withCondition(DefaultResourceConditions.allModsLoaded(modid));
		}

		GeneratedRecipeBuilder whenModMissing(String modid) {
			return withCondition(DefaultResourceConditions.not(DefaultResourceConditions.allModsLoaded(modid)));
		}

		GeneratedRecipeBuilder withCondition(ConditionJsonProvider condition) {
			recipeConditions.add(condition);
			return this;
		}

		GeneratedRecipeBuilder withSuffix(String suffix) {
			this.suffix = suffix;
			return this;
		}

		GeneratedRecipe viaShaped(UnaryOperator<ShapedRecipeJsonBuilder> builder) {
			return register(consumer -> {
				ShapedRecipeJsonBuilder b =
					builder.apply(ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, result.get(), amount));
				if (unlockedBy != null)
					b.criterion("has_item", conditionsFromItemPredicates(unlockedBy.get()));
				b.offerTo(consumer, createLocation("crafting"));
			});
		}

		GeneratedRecipe viaShapeless(UnaryOperator<ShapelessRecipeJsonBuilder> builder) {
			return register(consumer -> {
				ShapelessRecipeJsonBuilder b =
					builder.apply(ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, result.get(), amount));
				if (unlockedBy != null)
					b.criterion("has_item", conditionsFromItemPredicates(unlockedBy.get()));

				b.offerTo(result -> {
					consumer.accept(!recipeConditions.isEmpty()
						? new ConditionSupportingShapelessRecipeResult(result, recipeConditions)
						: result);
				}, createLocation("crafting"));
			});
		}

		GeneratedRecipe viaNetheriteSmithing(Supplier<? extends Item> base, Supplier<Ingredient> upgradeMaterial) {
			return register(consumer -> {
				SmithingTransformRecipeJsonBuilder b =
					SmithingTransformRecipeJsonBuilder.create(Ingredient.ofItems(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
						Ingredient.ofItems(base.get()), upgradeMaterial.get(), RecipeCategory.COMBAT, result.get()
							.asItem());
				b.criterion("has_item", conditionsFromItemPredicates(ItemPredicate.Builder.create()
					.items(base.get())
					.build()));
				b.offerTo(consumer, createLocation("crafting"));
			});
		}

		private Identifier createSimpleLocation(String recipeType) {
			return Create.asResource(recipeType + "/" + getRegistryName().getPath() + suffix);
		}

		private Identifier createLocation(String recipeType) {
			return Create.asResource(recipeType + "/" + path + "/" + getRegistryName().getPath() + suffix);
		}

		private Identifier getRegistryName() {
			return compatDatagenOutput == null ? CatnipServices.REGISTRIES.getKeyOrThrow(result.get()
				.asItem()) : compatDatagenOutput;
		}

		GeneratedCookingRecipeBuilder viaCooking(Supplier<? extends ItemConvertible> item) {
			return unlockedBy(item).viaCookingIngredient(() -> Ingredient.ofItems(item.get()));
		}

		GeneratedCookingRecipeBuilder viaCookingTag(Supplier<TagKey<Item>> tag) {
			return unlockedByTag(tag).viaCookingIngredient(() -> Ingredient.fromTag(tag.get()));
		}

		GeneratedCookingRecipeBuilder viaCookingIngredient(Supplier<Ingredient> ingredient) {
			return new GeneratedCookingRecipeBuilder(ingredient);
		}

		class GeneratedCookingRecipeBuilder {
			private Supplier<Ingredient> ingredient;
			private float exp;
			private int cookingTime;

			private final RecipeSerializer<? extends AbstractCookingRecipe> FURNACE = RecipeSerializer.SMELTING,
				SMOKER = RecipeSerializer.SMOKING, BLAST = RecipeSerializer.BLASTING,
				CAMPFIRE = RecipeSerializer.CAMPFIRE_COOKING;

			GeneratedCookingRecipeBuilder(Supplier<Ingredient> ingredient) {
				this.ingredient = ingredient;
				cookingTime = 200;
				exp = 0;
			}

			GeneratedCookingRecipeBuilder forDuration(int duration) {
				cookingTime = duration;
				return this;
			}

			GeneratedCookingRecipeBuilder rewardXP(float xp) {
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

			GeneratedRecipe inBlastFurnace() {
				return inBlastFurnace(b -> b);
			}

			GeneratedRecipe inBlastFurnace(UnaryOperator<CookingRecipeJsonBuilder> builder) {
				create(FURNACE, builder, 1);
				return create(BLAST, builder, .5f);
			}

			private GeneratedRecipe create(RecipeSerializer<? extends AbstractCookingRecipe> serializer,
																	  UnaryOperator<CookingRecipeJsonBuilder> builder, float cookingTimeModifier) {
				return register(consumer -> {
					boolean isOtherMod = compatDatagenOutput != null;

					CookingRecipeJsonBuilder b = builder.apply(CookingRecipeJsonBuilder.create(ingredient.get(),
						RecipeCategory.MISC, isOtherMod ? Items.DIRT : result.get(), exp,
						(int) (cookingTime * cookingTimeModifier), serializer));

					if (unlockedBy != null)
						b.criterion("has_item", conditionsFromItemPredicates(unlockedBy.get()));

					b.offerTo(result -> {
						consumer.accept(
							isOtherMod ? new ModdedCookingRecipeResult(result, compatDatagenOutput, recipeConditions)
								: result);
					}, createSimpleLocation(CatnipServices.REGISTRIES.getKeyOrThrow(serializer)
						.getPath()));
				});
			}
		}
	}

	@Override
	public String getName() {
		return modid + "'s Standard Recipes";
	}

	private record ModdedCookingRecipeResult(RecipeJsonProvider wrapped, Identifier outputOverride,
		List<ConditionJsonProvider> conditions) implements RecipeJsonProvider {
		@Override
		public Identifier getRecipeId() {
			return wrapped.getRecipeId();
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return wrapped.getSerializer();
		}

		@Override
		public JsonObject toAdvancementJson() {
			return wrapped.toAdvancementJson();
		}

		@Override
		public Identifier getAdvancementId() {
			return wrapped.getAdvancementId();
		}

		@Override
		public void serialize(JsonObject object) {
			wrapped.serialize(object);
			object.addProperty("result", outputOverride.toString());

			ConditionJsonProvider.write(object, conditions.toArray(new ConditionJsonProvider[0]));
		}
	}

	private record ConditionSupportingShapelessRecipeResult(RecipeJsonProvider wrapped, List<ConditionJsonProvider> conditions) implements RecipeJsonProvider {
		@Override
		public Identifier getRecipeId() {
			return wrapped.getRecipeId();
		}

		@Override
		public RecipeSerializer<?> getSerializer() {
			return wrapped.getSerializer();
		}

		@Override
		public JsonObject toAdvancementJson() {
			return wrapped.toAdvancementJson();
		}

		@Override
		public Identifier getAdvancementId() {
			return wrapped.getAdvancementId();
		}

		@Override
		public void serialize(@NotNull JsonObject pJson) {
			wrapped.serialize(pJson);

			ConditionJsonProvider.write(pJson, conditions.toArray(new ConditionJsonProvider[0]));
		}
	}
}
