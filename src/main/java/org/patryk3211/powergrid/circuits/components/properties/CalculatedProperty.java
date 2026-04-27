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
package org.patryk3211.powergrid.circuits.components.properties;

import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

import java.util.function.Function;

public class CalculatedProperty<T> extends ComponentProperty<T> {
    private final Function<PlacedComponent, T> calculate;
    private final Function<T, String> displayFunc;

    public CalculatedProperty(String namespace, String name, Function<PlacedComponent, T> calculate, Function<T, String> displayFunc) {
        super(namespace, name);
        this.calculate = calculate;
        this.displayFunc = displayFunc;
    }

    @Override
    public T parse(String value) {
        return null;
    }

    @Override
    public String toString(T value) {
        return displayFunc.apply(value);
    }

    @Override
    public T read(@Nullable Tag element) {
        return null;
    }

    @Override
    public Tag write(T value) {
        return null;
    }

    @Override
    public T defaultValue() {
        return null;
    }

    public T calculate(PlacedComponent state) {
        return calculate.apply(state);
    }
}
