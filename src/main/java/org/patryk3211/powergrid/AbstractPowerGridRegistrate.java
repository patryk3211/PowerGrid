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

import com.simibubi.create.foundation.data.CreateBlockEntityBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.Component;
import org.patryk3211.powergrid.circuits.components.ComponentBuilder;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;

import java.util.function.Function;

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
    public <T extends BlockEntity> CreateBlockEntityBuilder<T, AbstractPowerGridRegistrate> blockEntity(String name, BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return this.blockEntity(this.self(), name, factory);
    }

    @NotNull
    @Override
    public <T extends BlockEntity, P> CreateBlockEntityBuilder<T, P> blockEntity(P parent, String name, BlockEntityBuilder.BlockEntityFactory<T> factory) {
        return (CreateBlockEntityBuilder<T, P>) this.entry(name, (callback) -> CreateBlockEntityBuilder.create(this, parent, name, callback, factory));
    }

    public <T extends Component> ComponentBuilder<T, AbstractPowerGridRegistrate> component(String name, NonNullFunction<ComponentFootprint, T> factory) {
        return component(this.self(), name, factory);
    }

    public <T extends Component, P> ComponentBuilder<T, P> component(P parent, String name, NonNullFunction<ComponentFootprint, T> factory) {
        return this.entry(name, callback ->  ComponentBuilder.create(this, parent, name, callback, factory));
    }
}
