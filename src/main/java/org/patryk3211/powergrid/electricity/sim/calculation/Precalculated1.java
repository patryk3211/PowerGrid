/*
 * Copyright 2026 patryk3211
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

import java.util.function.BiConsumer;

public class Precalculated1<T, D extends IStamped> extends Precalculated<T> {
    @Nullable
    private D dependency;
    private int dependencyStamp = -1;
    private final BiConsumer<D, ValueHandler> func;

    public Precalculated1(BiConsumer<D, ValueHandler> func) {
        super(null);
        this.func = func;
    }

    public Precalculated1(BiConsumer<D, ValueHandler> func, T defaultValue) {
        super(defaultValue);
        this.func = func;
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
}
