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

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class FloatProperty extends ComponentProperty<Float> {
    private final float defaultValue;
    private final float min;
    private final float max;

    public FloatProperty(String namespace, String name, float defaultValue, float min, float max) {
        super(namespace, name);
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
    }

    protected float limit(float value) {
        if(value < min)
            return min;
        if(value > max)
            return max;
        return value;
    }

    @Override
    public Float parse(String str) throws NumberFormatException {
        if(str == null || str.isEmpty()) throw new NumberFormatException("Value is empty");
        str = str.trim();
        char modifierCharacter = str.charAt(str.length() - 1);
        boolean hasModifier = "pnumkM".indexOf(modifierCharacter) != -1; 
        String numericPart = hasModifier ? str.substring(0, str.length() - 1) : str;
        var value = Float.parseFloat(numericPart);
        switch (modifierCharacter) {
            case 'p' -> value /= 1_000_000_000_000f; // pico
            case 'n' -> value /= 1_000_000_000f;     // nano
            case 'u' -> value /= 1_000_000f;         // micro
            case 'm' -> value /= 1_000f;             // milli
            case 'k' -> value *= 1_000f;             // kilo
            case 'M' -> value *= 1_000_000f;         // Mega
        }
        return limit(value);
    }

    @Override
    public String toString(Float value) {
        if(value == 0)
            return Float.toString(0);
        String suffix;
        if(value < 1e-9f) {
            value *= 1e12f;
            suffix = "p";
        } else if(value < 1e-6f) {
            value *= 1e9f;
            suffix = "n";
        } else if(value < 1e-3f) {
            value *= 1e6f;
            suffix = "u";
        } else if(value < 1) {
            value *= 1e3f;
            suffix = "m";
        } else if(value < 1e3f) {
            suffix = "";
        } else if(value < 1e6f) {
            value *= 1e-3f;
            suffix = "k";
        } else {
            value *= 1e-6f;
            suffix = "M";
        }
        return value + suffix;
    }

    @Override
    public Float read(HolderLookup.Provider registries, @Nullable Tag element) {
        if(element == null)
            return defaultValue;
        if(element.getId() != Tag.TAG_FLOAT)
            return defaultValue;
        var value = ((FloatTag) element).getAsFloat();
        return limit(value);
    }

    @Override
    public Tag write(HolderLookup.Provider registries, Float value) {
        return FloatTag.valueOf(value);
    }

    @Override
    public Float defaultValue() {
        return defaultValue;
    }
}
