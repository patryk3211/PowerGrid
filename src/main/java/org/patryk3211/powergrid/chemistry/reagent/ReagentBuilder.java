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
package org.patryk3211.powergrid.chemistry.reagent;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.fabric.EnvExecutor;
import com.tterrag.registrate.fabric.RegistryObject;
import com.tterrag.registrate.fabric.SimpleFlowableFluid;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ReagentBuilder<T extends Reagent, P> extends AbstractBuilder<Reagent, T, P, ReagentBuilder<T, P>> {
    private final NonNullFunction<Reagent.Properties, T> factory;
    private Supplier<Reagent.Properties> propertiesSupplier;
    private NonNullUnaryOperator<Reagent.Properties> propertiesModifier = NonNullUnaryOperator.identity();

    public ReagentBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, RegistryKey<Registry<Reagent>> registryKey, NonNullFunction<Reagent.Properties, T> factory) {
        super(owner, parent, name, callback, registryKey);
        this.factory = factory;
        this.propertiesSupplier = Reagent.Properties::new;
    }

    public static <T extends Reagent, P> ReagentBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<Reagent.Properties, T> factory) {
        return new ReagentBuilder<>(owner, parent, name, callback, ReagentRegistry.REGISTRY_KEY, factory).defaultLang();
    }

    public ReagentBuilder<T, P> initialProperties(Supplier<Reagent> supplier) {
        this.propertiesSupplier = () -> supplier.get().properties.copy();
        return this;
    }

    public ReagentBuilder<T, P> initialProperties(Reagent.Properties properties) {
        this.propertiesSupplier = () -> properties;
        return this;
    }

    public ReagentBuilder<T, P> properties(NonNullUnaryOperator<Reagent.Properties> func) {
        propertiesModifier = propertiesModifier.andThen(func);
        return this;
    }

    public ReagentBuilder<T, P> item(ItemEntry<?> item, int amount, float temperature) {
        onRegisterAfter(RegistryKeys.ITEM, reagent -> reagent.withItem(item.get(), amount, temperature));
        return this;
    }

    public ReagentBuilder<T, P> item(Item item, int amount, float temperature) {
        onRegisterAfter(RegistryKeys.ITEM, reagent -> reagent.withItem(item, amount, temperature));
        return this;
    }

    public ReagentBuilder<T, P> fluid(FluidEntry<?> fluid, float temperature) {
        onRegisterAfter(RegistryKeys.FLUID, reagent -> reagent.withFluid(fluid.get().getStill(), temperature));
        return this;
    }

    public ReagentBuilder<T, P> fluid(Fluid fluid, float temperature) {
        onRegisterAfter(RegistryKeys.FLUID, reagent -> reagent.withFluid(fluid, temperature));
        return this;
    }

    public FluidBuilder<SimpleFlowableFluid.Flowing, ReagentBuilder<T, P>> coloredWaterFluid(int tint, float temperature) {
        var waterStill = new Identifier("block/water_still");
        var waterFlowing = new Identifier("block/water_flowing");
        var fluidBuilder = getOwner().fluid(this, getName(), waterStill, waterFlowing)
                .renderType(() -> RenderLayer::getTranslucent)
                .tag(FluidTags.WATER) // Fabric: water tag controls physics
                .source(SimpleFlowableFluid.Source::new) // TODO: remove when Registrate fixes FluidBuilder
                .fluidAttributes(() -> new FluidVariantAttributeHandler() { });
        // TODO: This is a HACK to prevent our renderer from getting overwritten by the fluid builder's onRegister setting the default renderer.
        onRegisterAfter(RegistryKeys.FLUID, reagent -> {
            var fluid = fluidBuilder.getEntry();
            EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> registerSimpleFluidRenderer(fluid, tint));
            reagent.withFluid(fluid.getStill(), temperature);
        });
        return fluidBuilder;
    }

    @Environment(EnvType.CLIENT)
    private static void registerSimpleFluidRenderer(SimpleFlowableFluid.Flowing fluid, int tint) {
        var handler = SimpleFluidRenderHandler.coloredWater(tint);
        FluidRenderHandlerRegistry.INSTANCE.register(fluid.getStill(), fluid.getFlowing(), handler);
    }

    public ReagentBuilder<T, P> fixedState(ReagentState state) {
        onRegister(r -> r.withFixedState(state));
        return this;
    }

    public ReagentBuilder<T, P> simpleFluid(int tint, float temperature) {
        return coloredWaterFluid(tint, temperature).build();
    }

    public ReagentBuilder<T, P> liquidConductance(float conductance) {
        onRegister(reagent -> reagent.withLiquidConductance(conductance));
        return this;
    }

    public ReagentBuilder<T, P> particleColor(int rgb) {
        onRegister(reagent -> reagent.withParticleColor(rgb));
        return this;
    }

    public ReagentBuilder<T, P> recipe(NonNullBiConsumer<DataGenContext<Reagent, T>, RegistrateRecipeProvider> cons) {
        return this.setData(ProviderType.RECIPE, cons);
    }

    public ReagentBuilder<T, P> defaultLang() {
        return this.lang(Reagent::getTranslationKey);
    }

    public ReagentBuilder<T, P> lang(String name) {
        return this.lang(Reagent::getTranslationKey, name);
    }

    @NotNull
    @Override
    protected T createEntry() {
        var properties = propertiesSupplier.get();
        properties = propertiesModifier.apply(properties);
        return this.factory.apply(properties);
    }

    @NotNull
    @Override
    protected RegistryEntry<T> createEntryWrapper(RegistryObject<T> delegate) {
        return new ReagentEntry<>(getOwner(), delegate);
    }

    @NotNull
    public ReagentEntry<T> register() {
        return (ReagentEntry<T>) super.register();
    }
}
