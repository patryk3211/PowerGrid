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
package org.patryk3211.powergrid.forge;

import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.AbstractPowerGridRegistrate;

public class ForgePowerGridRegistrate extends AbstractPowerGridRegistrate {
    protected ForgePowerGridRegistrate(String modid) {
        super(modid);
    }

    public static AbstractPowerGridRegistrate create(String modid) {
        return new ForgePowerGridRegistrate(modid);
    }

    @NotNull
    @Override
    protected <R, T extends R> RegistryEntry<R, T> accept(String name, ResourceKey<? extends Registry<R>> type, Builder<R, T, ?, ?> builder, NonNullSupplier<? extends T> creator, NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);
        if(type.equals(Registries.ITEM) && this.tooltipModifierFactory != null) {
            this.addRegisterCallback(name, Registries.ITEM, item -> {
                TooltipModifier modifier = tooltipModifierFactory.apply(item);
                TooltipModifier.REGISTRY.register(item, modifier);
            });
        }

        return entry;
    }

    @Override
    public AbstractPowerGridRegistrate registerEventListeners(IEventBus bus) {
        return super.registerEventListeners(bus);
    }
}
