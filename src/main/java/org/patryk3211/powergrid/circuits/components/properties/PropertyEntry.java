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

import net.minecraft.nbt.NbtCompound;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;

public class PropertyEntry<T> {
    public final ComponentProperty<T> property;
    public final PlacedComponent parent;
    protected T value;

    protected PropertyEntry(ComponentProperty<T> property, PlacedComponent parent) {
        this.property = property;
        this.parent = parent;
        this.value = property.defaultValue();
    }

    public static <T> PropertyEntry<T> makeFor(ComponentProperty<T> property, PlacedComponent parent) {
        if(property instanceof CalculatedProperty<T> calculatedProperty) {
            return new Calculated<>(calculatedProperty, parent);
        } else {
            return new PropertyEntry<>(property, parent);
        }
    }

    public void read(NbtCompound compound) {
        if(property instanceof CalculatedProperty<T>)
            return;
        var element = compound.get(property.id().toString());
        if(element == null) {
            value = property.defaultValue();
        } else {
            value = property.read(element);
        }
    }

    public void write(NbtCompound compound) {
        var element = property.write(value);
        if(element == null)
            return;
        compound.put(property.id().toString(), element);
    }

    public String stringValue() {
        return property.toString(value);
    }

    public void setValue(String value) {
        this.value = property.parse(value);
    }

    public void setValueRaw(Object value) {
        this.value = (T) value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public static class Calculated<T> extends PropertyEntry<T> {
        private final CalculatedProperty<T> calculated;

        protected Calculated(CalculatedProperty<T> property, PlacedComponent parent) {
            super(property, parent);
            this.calculated = property;
        }

        @Override
        public void read(NbtCompound compound) { }
        @Override
        public void write(NbtCompound compound) { }

        @Override
        public void setValue(String value) {
            throw new IllegalCallerException("Cannot set value of calculated property");
        }

        @Override
        public void setValueRaw(Object value) { }

        @Override
        public void set(T value) {
            throw new IllegalCallerException("Cannot set value of calculated property");
        }

        @Override
        public String stringValue() {
            return calculated.toString(get());
        }

        @Override
        public T get() {
            return calculated.calculate(parent);
        }
    }
}
