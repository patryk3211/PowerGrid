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
import net.minecraft.nbt.NbtFloat;
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

    private float limit(float value) {
        if(value < min)
            return min;
        if(value > max)
            return max;
        return value;
    }

    @Override
    public Float parse(String str) throws NumberFormatException {
        var value = Float.parseFloat(str);
        return limit(value);
    }

    @Override
    public String toString(Float value) {
        return Float.toString(value);
    }

    @Override
    public Float read(@Nullable NbtElement element) {
        if(element == null)
            return defaultValue;
        if(element.getType() != NbtElement.FLOAT_TYPE)
            return defaultValue;
        var value = ((NbtFloat) element).floatValue();
        return limit(value);
    }

    @Override
    public NbtElement write(Float value) {
        return NbtFloat.of(value);
    }

    @Override
    public Float defaultValue() {
        return defaultValue;
    }
}
