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

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Precalculated<T, D extends IStamped> implements IStamped {
    private final BiConsumer<D, ValueHandler> func;
    @Nullable
    private D dependency;
    private int dependencyStamp = -1;
    private int ourStamp = 0;

    private final T defaultValue;
    private T value = null;
    private final List<Object> auxData = new ArrayList<>();
    private final ValueHandler handler = new ValueHandler();

    public Precalculated(BiConsumer<D, ValueHandler> func) {
        this.func = func;
        this.defaultValue = null;
    }

    public Precalculated(BiConsumer<D, ValueHandler> func, T defaultValue) {
        this.func = func;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public void updateDependency(@Nullable D dependency) {
        this.dependency = dependency;
        if(dependency == null) {
            this.dependencyStamp = -1;
        }
    }

    public T get() {
        if(dependency == null) {
            value = defaultValue;
        } else if(dependencyStamp != dependency.getStamp()) {
            ++ourStamp;
            dependencyStamp = dependency.getStamp();
            func.accept(dependency, handler);
        }
        return value;
    }

    @Override
    public int getStamp() {
        if(dependency == null)
            return -1;
        if(dependencyStamp != dependency.getStamp())
            return ourStamp + 1;
        return ourStamp;
    }

    public void invalidate() {
        --dependencyStamp;
    }

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
