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
package org.patryk3211.powergrid.electricity.sim.calculation;

import java.util.ArrayList;
import java.util.List;

public abstract class Precalculated<T> implements IStamped {
    protected int ourStamp = 0;

    protected final T defaultValue;
    protected T value;

    protected final List<Object> auxData = new ArrayList<>();
    protected final ValueHandler handler = new ValueHandler();

    public Precalculated(T defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public void setValue(T value) {
        if(!this.value.equals(value)) {
            this.value = value;
            ++ourStamp;
        }
    }

    public abstract T get();
    public abstract int getStamp();
    public abstract void invalidate();

    public class ValueHandler {
        public void emit(T value) {
            Precalculated.this.value = value;
        }

        public <A> void store(int slot, A value) {
            if(slot < auxData.size()) {
                auxData.set(slot, value);
            } else if(slot == auxData.size()) {
                auxData.add(value);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        public T get() {
            return Precalculated.this.value;
        }

        public <A> A get(int slot, Class<A> clazz) {
            return get(slot, clazz, null);
        }

        public <A> A get(int slot, Class<A> clazz, A defaultValue) {
            if(slot < 0 || slot >= auxData.size())
                return defaultValue;
            return clazz.cast(auxData.get(slot));
        }
    }
}
