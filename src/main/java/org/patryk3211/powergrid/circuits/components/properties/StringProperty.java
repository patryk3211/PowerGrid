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

import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class StringProperty extends ComponentProperty<String> {
    private final String defaultValue;

    public StringProperty(String namespace, String name) {
        this(namespace, name, null);
    }

    public StringProperty(String namespace, String name, @Nullable String defaultValue) {
        super(namespace, name);
        this.defaultValue = defaultValue == null ? "" : defaultValue;
    }

    private String limit(String value) {
        if(value.length() > 32)
            return value.substring(0, 32);
        return value;
    }

    @Override
    public String parse(String str) {
        return limit(str);
    }

    @Override
    public String toString(String value) {
        return value;
    }

    @Override
    public String read(@Nullable Tag element) {
        if(element == null)
            return defaultValue;
        if(element.getId() != Tag.TAG_STRING)
            return defaultValue;
        var value = element.getAsString();
        return limit(value);
    }

    @Override
    public Tag write(String value) {
        return StringTag.valueOf(value);
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }

    @Override
    public String[] allValues() {
        return null;
    }
}
