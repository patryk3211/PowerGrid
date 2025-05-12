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
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGridRegistrate;

import java.util.function.Supplier;

public class ReagentBuilder<T extends Reagent, P> extends AbstractBuilder<Reagent, Reagent, P, ReagentBuilder<T, P>> {
    private final NonNullFunction<Reagent.Properties, T> factory;
    private Supplier<Reagent.Properties> propertiesSupplier;
    private NonNullUnaryOperator<Reagent.Properties> propertiesModifier = NonNullUnaryOperator.identity();

    public ReagentBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, RegistryKey<Registry<Reagent>> registryKey, NonNullFunction<Reagent.Properties, T> factory) {
        super(owner, parent, name, callback, registryKey);
        this.factory = factory;
        this.propertiesSupplier = Reagent.Properties::new;
    }

    public static <T extends Reagent, P> ReagentBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<Reagent.Properties, T> factory) {
        return new ReagentBuilder<>(owner, parent, name, callback, ReagentRegistry.REGISTRY_KEY, factory);
    }

    public ReagentBuilder<T, P> properties(NonNullUnaryOperator<Reagent.Properties> func) {
        propertiesModifier = propertiesModifier.andThen(func);
        return this;
    }

    @NotNull
    @Override
    protected Reagent createEntry() {
        var properties = propertiesSupplier.get();
        properties = propertiesModifier.apply(properties);
        return this.factory.apply(properties);
    }

    @NotNull
    public ReagentEntry<Reagent> register() {
        return (ReagentEntry<Reagent>) super.register();
    }
}
