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

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import org.jetbrains.annotations.Nullable;

public class EnumProperty<T extends Enum<T>> extends ComponentProperty<T> {
    private final Class<T> clazz;
    private final T[] values;
    private final T defaultValue;

    public EnumProperty(String namespace, String name, Class<T> clazz) {
        super(namespace, name);
        this.clazz = clazz;
        values = clazz.getEnumConstants();
        defaultValue = values[0];
    }

    public EnumProperty(String namespace, String name, Class<T> clazz, T[] values) {
        super(namespace, name);
        this.clazz = clazz;
        this.values = values;
        defaultValue = values[0];
    }

    public EnumProperty(String namespace, String name, Class<T> clazz, T[] values, T defaultValue) {
        super(namespace, name);
        this.clazz = clazz;
        this.values = values;
        this.defaultValue = defaultValue;
    }

    @Override
    public T parse(String str) {
        for(var value : values) {
            if(value.name().equals(str))
                return value;
        }
        return null;
    }

    @Override
    public String toString(T value) {
        return value.name();
    }

    @Override
    public T read(@Nullable NbtElement element) {
        if(element == null)
            return defaultValue;
        if(element.getType() != NbtElement.INT_TYPE)
            return defaultValue;
        var value = ((NbtInt) element).intValue();
        return values[value];
    }

    @Override
    public NbtElement write(T value) {
        return NbtInt.of(value.ordinal());
    }

    @Override
    public T defaultValue() {
        return defaultValue;
    }
}
