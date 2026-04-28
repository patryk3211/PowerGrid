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
package org.patryk3211.powergrid.compat.rei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.compat.rei.*;
import com.simibubi.create.compat.rei.category.CreateRecipeCategory;
import com.simibubi.create.compat.rei.category.MysteriousItemConversionCategory;
import com.simibubi.create.compat.rei.display.CreateDisplay;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CRecipes;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableEditScreen;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.compat.HiddenItems;
import org.patryk3211.powergrid.electricity.electromagnet.recipe.MagnetizingRecipe;
import org.patryk3211.powergrid.utility.Lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.simibubi.create.compat.rei.CreateREI.*;

public class PowerGridREI implements REIClientPlugin {
    private static final ResourceLocation ID = PowerGrid.asResource("rei_plugin");

    private final List<CreateRecipeCategory<?>> allCategories = new ArrayList<>();

    @Override
    public String getPluginProviderName() {
        return ID.toString();
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        REIClientPlugin.super.registerScreens(registry);
        registry.registerDraggableStackVisitor(new DraggableStackVisitor<CircuitDesignTableEditScreen>() {
            @Override
            public <R extends Screen> boolean isHandingScreen(R screen) {
                return screen instanceof CircuitDesignTableEditScreen;
            }

            @Override
            public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<CircuitDesignTableEditScreen> context, DraggableStack stack) {
                return Stream.of(() -> {
                    var r = context.getScreen().ghostTarget();
                    return BoundsProvider.fromRectangle(new Rectangle(r.getX(), r.getY(), r.getWidth(), r.getHeight()));
                });
            }

            @Override
            public DraggedAcceptorResult acceptDraggedStack(DraggingContext<CircuitDesignTableEditScreen> context, DraggableStack stack) {
                if(stack.get().getType() == VanillaEntryTypes.ITEM) {
                    var itemStack = ((EntryStack<ItemStack>) stack.get()).getValue();
                    context.getScreen().toolSelect(itemStack.getItem());
                    return DraggedAcceptorResult.ACCEPTED;
                }
                return DraggedAcceptorResult.PASS;
            }
        });
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        var circuitDesignCategory = new CircuitDesignCategory();
        var circuitAssemblyCategory = new CircuitAssemblyCategory();

        registry.add(circuitDesignCategory);
        registry.addWorkstations(
                CircuitDesignCategory.IDENTIFIER,
                EntryStack.of(VanillaEntryTypes.ITEM, ModdedBlocks.CIRCUIT_DESIGN_TABLE.asStack())
        );

        registry.add(circuitAssemblyCategory);
        registry.addWorkstations(
                CircuitAssemblyCategory.IDENTIFIER,
                EntryStack.of(VanillaEntryTypes.ITEM, AllBlocks.MECHANICAL_ARM.asStack())
        );

        var magnetizing = new CategoryBuilder<>(MagnetizingRecipe.class)
                .addTypedRecipes(MagnetizingRecipe.TYPE_INFO)
                .catalyst(ModdedBlocks.ELECTROMAGNET::get)
                .itemIcon(ModdedBlocks.ELECTROMAGNET.get())
                .emptyBackground(177, 88)
                .build("magnetizing", MagnetizingCategory::new);

        allCategories.forEach(category -> {
            registry.add(category);
            category.registerCatalysts(registry);
        });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.add(new CircuitDesignDisplay());
        registry.add(new CircuitAssemblyDisplay());
        allCategories.forEach(c -> c.registerRecipes(registry));
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(
                new ItemStack(Items.IRON_INGOT),
                ModdedItems.MAGNET.asStack()
        ));
        registry.removeEntryIf(entryStack -> {
            if(entryStack.getType() == VanillaEntryTypes.ITEM) {
                ItemStack itemStack = entryStack.castValue();
                return HiddenItems.getHiddenPredicate().test(itemStack.getItem());
            }
            return false;
        });
    }

    /**
     * @see CreateREI.CategoryBuilder
     */
    private class CategoryBuilder<T extends Recipe<?>> {
        private final Class<? extends T> recipeClass;
        private Predicate<CRecipes> predicate = cRecipes -> true;

        private Renderer background;
        private Renderer icon;

        private int width;
        private int height;

        private Function<T, ? extends CreateDisplay<T>> displayFactory;

        private final List<Consumer<List<T>>> recipeListConsumers = new ArrayList<>();
        private final List<Supplier<? extends ItemStack>> catalysts = new ArrayList<>();

        public CategoryBuilder(Class<? extends T> recipeClass) {
            this.recipeClass = recipeClass;
        }

        public CategoryBuilder<T> enableIf(Predicate<CRecipes> predicate) {
            this.predicate = predicate;
            return this;
        }

        public CategoryBuilder<T> enableWhen(Function<CRecipes, ConfigBase.ConfigBool> configValue) {
            predicate = c -> configValue.apply(c).get();
            return this;
        }

        public CategoryBuilder<T> addRecipeListConsumer(Consumer<List<T>> consumer) {
            recipeListConsumers.add(consumer);
            return this;
        }

        public CategoryBuilder<T> addRecipes(Supplier<Collection<? extends T>> collection) {
            return addRecipeListConsumer(recipes -> recipes.addAll(collection.get()));
        }

        public CategoryBuilder<T> addAllRecipesIf(Predicate<Recipe<?>> pred) {
            return addRecipeListConsumer(recipes -> consumeAllRecipes(recipe -> {
                if (pred.test(recipe)) {
                    recipes.add((T) recipe);
                }
            }));
        }

        public CategoryBuilder<T> addAllRecipesIf(Predicate<Recipe<?>> pred, Function<Recipe<?>, T> converter) {
            return addRecipeListConsumer(recipes -> consumeAllRecipes(recipe -> {
                if (pred.test(recipe)) {
                    recipes.add(converter.apply(recipe));
                }
            }));
        }

        public CategoryBuilder<T> addTypedRecipes(IRecipeTypeInfo recipeTypeEntry) {
            return addTypedRecipes(recipeTypeEntry::getType);
        }

        public CategoryBuilder<T> addTypedRecipes(Supplier<RecipeType<? extends T>> recipeType) {
            return addRecipeListConsumer(recipes -> CreateREI.<T>consumeTypedRecipes(recipes::add, recipeType.get()));
        }

        public CategoryBuilder<T> addTypedRecipes(Supplier<RecipeType<? extends T>> recipeType, Function<Recipe<?>, T> converter) {
            return addRecipeListConsumer(recipes -> CreateREI.<T>consumeTypedRecipes(recipe -> recipes.add(converter.apply(recipe)), recipeType.get()));
        }

        public CategoryBuilder<T> addTypedRecipesIf(Supplier<RecipeType<? extends T>> recipeType, Predicate<Recipe<?>> pred) {
            return addRecipeListConsumer(recipes -> CreateREI.<T>consumeTypedRecipes(recipe -> {
                if (pred.test(recipe)) {
                    recipes.add(recipe);
                }
            }, recipeType.get()));
        }

        public CategoryBuilder<T> addTypedRecipesExcluding(Supplier<RecipeType<? extends T>> recipeType,
                                                                     Supplier<RecipeType<? extends T>> excluded) {
            return addRecipeListConsumer(recipes -> {
                List<Recipe<?>> excludedRecipes = getTypedRecipes(excluded.get());
                CreateREI.<T>consumeTypedRecipes(recipe -> {
                    for (Recipe<?> excludedRecipe : excludedRecipes) {
                        if (doInputsMatch(recipe, excludedRecipe)) {
                            return;
                        }
                    }
                    recipes.add(recipe);
                }, recipeType.get());
            });
        }

        public CategoryBuilder<T> removeRecipes(Supplier<RecipeType<? extends T>> recipeType) {
            return addRecipeListConsumer(recipes -> {
                List<Recipe<?>> excludedRecipes = getTypedRecipes(recipeType.get());
                recipes.removeIf(recipe -> {
                    for (Recipe<?> excludedRecipe : excludedRecipes) {
                        if (doInputsMatch(recipe, excludedRecipe)) {
                            return true;
                        }
                    }
                    return false;
                });
            });
        }

        public CategoryBuilder<T> catalystStack(Supplier<ItemStack> supplier) {
            catalysts.add(supplier);
            return this;
        }

        public CategoryBuilder<T> catalyst(Supplier<ItemLike> supplier) {
            return catalystStack(() -> new ItemStack(supplier.get()
                    .asItem()));
        }

        public CategoryBuilder<T> icon(Renderer icon) {
            this.icon = icon;
            return this;
        }

        public CategoryBuilder<T> itemIcon(ItemLike item) {
            icon(new ItemIcon(() -> new ItemStack(item)));
            return this;
        }

        public CategoryBuilder<T> doubleItemIcon(ItemLike item1, ItemLike item2) {
            icon(new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2)));
            return this;
        }

        public CategoryBuilder<T> background(Renderer background) {
            this.background = background;
            return this;
        }

        public CategoryBuilder<T> emptyBackground(int width, int height) {
            background(new EmptyBackground(width, height));
            dimensions(width, height);
            return this;
        }

        public CategoryBuilder<T> width(int width) {
            this.width = width;
            return this;
        }

        public CategoryBuilder<T> height(int height) {
            this.height = height;
            return this;
        }

        public CategoryBuilder<T> dimensions(int width, int height) {
            width(width);
            height(height);
            return this;
        }

        public CategoryBuilder<T> displayFactory(Function<T, ? extends CreateDisplay<T>> factory) {
            this.displayFactory = factory;
            return this;
        }

        public CreateRecipeCategory<T> build(String name, CreateRecipeCategory.Factory<T> factory) {
            Supplier<List<T>> recipesSupplier;
            if (predicate.test(AllConfigs.server().recipes)) {
                recipesSupplier = () -> {
                    List<T> recipes = new ArrayList<>();
                    if (predicate.test(AllConfigs.server().recipes)) {
                        for (Consumer<List<T>> consumer : recipeListConsumers)
                            consumer.accept(recipes);
                    }
                    return recipes;
                };
            } else {
                recipesSupplier = Collections::emptyList;
            }

            if (width <= 0 || height <= 0) {
                Create.LOGGER.warn("Create REI category [{}] has weird dimensions: {}x{}", name, width, height);
            }

            CreateRecipeCategory.Info<T> info = new CreateRecipeCategory.Info<>(
                    CategoryIdentifier.of(Create.asResource(name)),
                    Lang.translateDirect("recipe." + name), background, icon, recipesSupplier, catalysts, width, height, displayFactory == null ? (recipe) -> new CreateDisplay<>(recipe, CategoryIdentifier.of(Create.asResource(name))) : displayFactory);
            CreateRecipeCategory<T> category = factory.create(info);
            allCategories.add(category);
            return category;
        }
    }
}
