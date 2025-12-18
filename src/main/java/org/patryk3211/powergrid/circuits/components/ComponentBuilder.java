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
package org.patryk3211.powergrid.circuits.components;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import com.tterrag.registrate.util.nullness.NonnullType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;

public class ComponentBuilder<T extends Component, P> extends AbstractBuilder<Component, T, P, ComponentBuilder<T, P>> {
    private final NonNullFunction<ComponentFootprint, T> factory;
    private ComponentFootprint footprint;

    public ComponentBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, ResourceKey<Registry<Component>> registryKey, NonNullFunction<ComponentFootprint, T> factory) {
        super(owner, parent, name, callback, registryKey);
        this.factory = factory;
    }

    public static <T extends Component, P> ComponentBuilder<T, P> create(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, NonNullFunction<ComponentFootprint, T> factory) {
        return new ComponentBuilder<>(owner, parent, name, callback, ComponentRegistry.REGISTRY_KEY, factory);
    }

    public ComponentBuilder<T, P> footprint(ComponentFootprint footprint) {
        this.footprint = footprint;
        return this;
    }

    public ComponentBuilder<T, P> footprint(int width, int height, NonNullUnaryOperator<ComponentFootprint.Builder> transform) {
        var keyBase = "component." + getOwner().getModid() + "." + getName();
        var builder = transform.apply(new ComponentFootprint.Builder(width, height, keyBase));
        addMiscData(ProviderType.LANG, provider -> builder.translatedPads.forEach(provider::add));
        this.footprint = builder.build();
        return this;
    }

    public ComponentBuilder<T, P> item(ItemLike item) {
        addMiscData(AbstractPowerGridRegistrate.COMPONENT_ITEMS, provider ->
                provider.add(getName(), item));
        return this;
    }

    @Override
    @NotNull
    protected @NonnullType T createEntry() {
        if(footprint == null)
            throw new IllegalStateException("Cannot create entry without a set footprint");
        return factory.apply(footprint);
    }
}
