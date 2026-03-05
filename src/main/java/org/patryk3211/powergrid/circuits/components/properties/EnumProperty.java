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

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class EnumProperty<T extends Enum<T>> extends ComponentProperty<T> {
    private final Class<T> clazz;
    private final T[] values;
    private final T[] allValues;
    private final int[] ordinalMap;
    private final T defaultValue;

    public EnumProperty(String namespace, String name, Class<T> clazz) {
        super(namespace, name);
        this.clazz = clazz;
        this.values = clazz.getEnumConstants();
        this.allValues = values;
        this.ordinalMap = new int[allValues.length];

        for (int i = 0; i < allValues.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (values[j].ordinal() == allValues[i].ordinal()) {
                    ordinalMap[i] = j;
                    break;
                }
            }
        }

        defaultValue = values[0];
    }

    public EnumProperty(String namespace, String name, Class<T> clazz, T[] values) {
        super(namespace, name);
        this.clazz = clazz;
        this.values = values;
        this.allValues = clazz.getEnumConstants();
        this.ordinalMap = new int[allValues.length];

        for (int i = 0; i < allValues.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (values[j].ordinal() == allValues[i].ordinal()) {
                    ordinalMap[i] = j;
                    break;
                }
            }
        }
        defaultValue = values[0];
    }

    public EnumProperty(String namespace, String name, Class<T> clazz, T[] values, T defaultValue) {
        super(namespace, name);
        this.clazz = clazz;
        this.values = values;
        this.allValues = clazz.getEnumConstants();
        this.ordinalMap = new int[allValues.length];

        for (int i = 0; i < allValues.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (values[j].ordinal() == allValues[i].ordinal()) {
                    ordinalMap[i] = j;
                    break;
                }
            }
        }
       
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
    public T read(@Nullable Tag element) {
        if(element == null)
            return defaultValue;
        if(element.getId() != Tag.TAG_INT)
            return defaultValue;
        var value = ((IntTag) element).getAsInt();
        return values[ordinalMap[value]];
    }

    @Override
    public Tag write(T value) {
        return IntTag.valueOf(value.ordinal());
    }

    @Override
    public T defaultValue() {
        return defaultValue;
    }
}
