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
package org.patryk3211.powergrid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.registrate.SimpleBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentBuilder;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.electricity.wire.registry.WireItemEntry;
import org.patryk3211.powergrid.utility.SimpleBlockEntityVisualFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractPowerGridRegistrate extends AbstractRegistrate<AbstractPowerGridRegistrate> {
    protected Function<Item, TooltipModifier> tooltipModifierFactory;

    protected AbstractPowerGridRegistrate(String modid) {
        super(modid);
    }

    public AbstractPowerGridRegistrate setTooltipModifierFactory(Function<Item, TooltipModifier> factory) {
        this.tooltipModifierFactory = factory;
        return this.self();
    }

    @NotNull
    @Override
    public <T extends BlockEntity> PowerGridBlockEntityBuilder<T, AbstractPowerGridRegistrate> blockEntity(String name, BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return this.blockEntity(this.self(), name, factory);
    }

    @NotNull
    @Override
    public <T extends BlockEntity, P> PowerGridBlockEntityBuilder<T, P> blockEntity(P parent, String name, BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return (PowerGridBlockEntityBuilder<T, P>) this.entry(name, (callback) -> PowerGridBlockEntityBuilder.create(this, parent, name, callback, factory));
    }

    public <T extends Component> ComponentBuilder<T, AbstractPowerGridRegistrate> component(String name, NonNullFunction<ComponentFootprint, T> factory) {
        return component(this.self(), name, factory);
    }

    public <T extends Component, P> ComponentBuilder<T, P> component(P parent, String name, NonNullFunction<ComponentFootprint, T> factory) {
        return this.entry(name, callback ->  ComponentBuilder.create(this, parent, name, callback, factory));
    }

    public <T extends DisplaySource> SimpleBuilder<DisplaySource, T, AbstractPowerGridRegistrate> displaySource(String name, Supplier<T> supplier) {
        return this.entry(name, callback ->
                new SimpleBuilder<>(this, this, name, callback, CreateRegistries.DISPLAY_SOURCE, supplier)
                        .byBlock(DisplaySource.BY_BLOCK).byBlockEntity(DisplaySource.BY_BLOCK_ENTITY));
    }

    public static class PowerGridBlockEntityBuilder<T extends BlockEntity, P> extends BlockEntityBuilder<T, P> {
        @Nullable
        private NonNullSupplier<SimpleBlockEntityVisualFactory<T>> visualFactory;
        private Predicate<T> renderNormally;

        private Collection<NonNullSupplier<? extends Collection<NonNullSupplier<? extends Block>>>> deferredValidBlocks = new ArrayList<>();

        public static <T extends BlockEntity, P> BlockEntityBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
            return new PowerGridBlockEntityBuilder<>(owner, parent, name, callback, factory);
        }

        protected PowerGridBlockEntityBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, BlockEntityFactory<T> factory) {
            super(owner, parent, name, callback, factory);
        }

        public PowerGridBlockEntityBuilder<T, P> validBlocksDeferred(NonNullSupplier<? extends Collection<NonNullSupplier<? extends Block>>> blocks) {
            deferredValidBlocks.add(blocks);
            return this;
        }

        @Override
        protected BlockEntityType<T> createEntry() {
            deferredValidBlocks.stream()
                    .map(Supplier::get)
                    .flatMap(Collection::stream)
                    .forEach(this::validBlock);
            return super.createEntry();
        }

        public PowerGridBlockEntityBuilder<T, P> displaySource(RegistryEntry<? extends DisplaySource> source) {
            this.onRegisterAfter(CreateRegistries.DISPLAY_SOURCE, type -> DisplaySource.BY_BLOCK_ENTITY.add(type, source.get()));
            return this;
        }

        public PowerGridBlockEntityBuilder<T, P> displayTarget(RegistryEntry<? extends DisplayTarget> target) {
            this.onRegisterAfter(CreateRegistries.DISPLAY_TARGET, type -> DisplayTarget.BY_BLOCK_ENTITY.register(type, target.get()));
            return this;
        }

        public PowerGridBlockEntityBuilder<T, P> visual(
                NonNullSupplier<SimpleBlockEntityVisualFactory<T>> visualFactory) {
            return visual(visualFactory, true);
        }

        public PowerGridBlockEntityBuilder<T, P> visual(
                NonNullSupplier<SimpleBlockEntityVisualFactory<T>> visualFactory,
                boolean renderNormally) {
            return visual(visualFactory, be -> renderNormally);
        }

        public PowerGridBlockEntityBuilder<T, P> visual(NonNullSupplier<SimpleBlockEntityVisualFactory<T>> visualFactory, Predicate<T> renderNormally) {
            if (this.visualFactory == null) {
                EnvExecutor.runInEnv(Env.CLIENT, () -> this::registerVisualizer);
            }

            this.visualFactory = visualFactory;
            this.renderNormally = renderNormally;

            return this;
        }

        protected void registerVisualizer() {
            this.onRegister((entry) -> {
                Objects.requireNonNull(this.visualFactory);
                Predicate<T> renderNormally = this.renderNormally;
                SimpleBlockEntityVisualizer.builder(this.getEntry())
                        .factory(this.visualFactory.get()::create)
                        .skipVanillaRender(be -> !renderNormally.test(be))
                        .apply();
            });
        }
    }

    public static ProviderType<ComponentItemEntryProvider> COMPONENT_ITEMS;
    public abstract static class ComponentItemEntryProvider implements RegistrateProvider {
        private final Map<String, Item> entries = new HashMap<>();
        private final AbstractRegistrate<?> owner;
        private final PackOutput output;

        public ComponentItemEntryProvider(AbstractRegistrate<?> owner, PackOutput output) {
            this.owner = owner;
            this.output = output;
        }

        @NotNull
        @Override
        public CompletableFuture<?> run(@NotNull CachedOutput output) {
            entries.clear();
            owner.genData(COMPONENT_ITEMS, this);
            var namespace = owner.getModid();

            // Generate circuit_component tag
            var tagJson = new JsonObject();
            var array = new JsonArray();
            tagJson.add("values", array);
            entries.values().stream().map(item -> {
                var id = BuiltInRegistries.ITEM.getResourceKey(item);
                return id.get().location().toString();
            }).forEach(array::add);
            var tag = DataProvider.saveStable(output, tagJson, this.output.getOutputFolder()
                    .resolve("data/" + namespace + "/tags/items/circuit_component.json"));

            // Generate all item mappings
            var path = this.output.getOutputFolder().resolve("data/" + namespace + "/powergrid/component_items");
            return CompletableFuture.allOf(CompletableFuture.allOf(entries.entrySet().stream().map(entry -> {
                var json = new JsonObject();
                var id = BuiltInRegistries.ITEM.getResourceKey(entry.getValue());
                json.addProperty("item", id.get().location().toString());
                return DataProvider.saveStable(output, json, path.resolve(entry.getKey() + ".json"));
            }).toArray(CompletableFuture[]::new)), tag);
        }

        @NotNull
        @Override
        public String getName() {
            return "Component Item Mapping";
        }

        public void add(String componentId, ItemLike item) {
            entries.put(componentId, item.asItem());
        }
    }

    public static ProviderType<WireItemEntryProvider> WIRE_ITEMS;
    public abstract static class WireItemEntryProvider implements RegistrateProvider {
        private final Map<String, WireItemEntry> entries = new HashMap<>();
        private final AbstractRegistrate<?> owner;
        private final PackOutput output;

        public WireItemEntryProvider(AbstractRegistrate<?> owner, PackOutput output) {
            this.owner = owner;
            this.output = output;
        }

        @NotNull
        @Override
        public CompletableFuture<?> run(@NotNull CachedOutput output) {
            entries.clear();
            owner.genData(WIRE_ITEMS, this);
            var namespace = owner.getModid();

            // Generate all item mappings
            var path = this.output.getOutputFolder().resolve("data/" + namespace + "/powergrid/wire_types");
            return CompletableFuture.allOf(entries.entrySet().stream().map(entry -> {
                var result = WireItemEntry.CODEC.encode(entry.getValue(), JsonOps.INSTANCE, new JsonObject());
                return result.result()
                        .map(json -> DataProvider.saveStable(output, json, path.resolve(entry.getKey() + ".json")))
                        .orElseThrow();
            }).toArray(CompletableFuture[]::new));
        }

        @NotNull
        @Override
        public String getName() {
            return "Wire Item Mapping";
        }

        public void add(String itemId, WireItemEntry entry) {
            entries.put(itemId, entry);
        }
    }
}
