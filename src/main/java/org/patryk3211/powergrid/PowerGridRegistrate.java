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

import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.fabric.RegistryObject;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;

public class PowerGridRegistrate extends AbstractRegistrate<PowerGridRegistrate> {
    protected Function<Item, TooltipModifier> tooltipModifierFactory;

    protected PowerGridRegistrate(String modid) {
        super(modid);
    }

    public static PowerGridRegistrate create(String modid) {
        return new PowerGridRegistrate(modid);
    }

    public PowerGridRegistrate setTooltipModifierFactory(Function<Item, TooltipModifier> factory) {
        this.tooltipModifierFactory = factory;
        return this.self();
    }

    @Override
    protected <R, T extends R> RegistryEntry<T> accept(String name, RegistryKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator, NonNullFunction<RegistryObject<T>, ? extends RegistryEntry<T>> entryFactory) {
        RegistryEntry<T> entry = super.accept(name, type, builder, creator, entryFactory);
        if(type.equals(RegistryKeys.ITEM) && this.tooltipModifierFactory != null) {
            TooltipModifier.REGISTRY.registerDeferred(entry.getId(), this.tooltipModifierFactory);
        }

        return entry;
    }
}
