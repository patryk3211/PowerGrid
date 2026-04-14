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

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class IntProperty extends ComponentProperty<Integer> {
    private final int defaultValue;
    private final int min;
    private final int max;

    public IntProperty(String namespace, String name, int defaultValue, int min, int max) {
        super(namespace, name);
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    private int limit(int value) {
        if(value < min)
            return min;
        if(value > max)
            return max;
        return value;
    }

    @Override
    public Integer parse(String str) throws NumberFormatException {
        var value = Integer.parseInt(str);
        return limit(value);
    }

    @Override
    public String toString(Integer value) {
        return Integer.toString(value);
    }

    @Override
    public Integer read(@Nullable Tag element) {
        if(element == null)
            return defaultValue;
        if(element.getId() != Tag.TAG_INT)
            return defaultValue;
        var value = ((IntTag) element).getAsInt();
        return limit(value);
    }

    @Override
    public Tag write(Integer value) {
        return IntTag.valueOf(value);
    }

    @Override
    public Integer defaultValue() {
        return defaultValue;
    }

    @Override
    public Integer[] allValues() {
        return null;
    }
}
