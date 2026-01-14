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

import java.util.function.BiFunction;
import java.util.function.Function;

public class Precalculated<T, D extends IStamped> implements IStamped {
    private final BiFunction<D, T, T> func;
    @Nullable
    private D dependency;
    private int dependencyStamp = -1;
    private int ourStamp = 0;

    private final T defaultValue;
    private T value = null;

    public Precalculated(Function<D, T> func) {
        this.func = (dep, prev) -> func.apply(dep);
        this.defaultValue = null;
    }

    public Precalculated(Function<D, T> func, T defaultValue) {
        this.func = (dep, prev) -> func.apply(dep);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public Precalculated(BiFunction<D, T, T> func, T defaultValue) {
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
            value = func.apply(dependency, value);
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
}
