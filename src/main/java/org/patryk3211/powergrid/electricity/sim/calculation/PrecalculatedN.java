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

import java.util.Arrays;
import java.util.function.BiConsumer;

public class PrecalculatedN<T, D extends IStamped> extends Precalculated<T> {
    @Nullable
    private D[] dependencies;
    private int[] dependencyStamps;
    private final BiConsumer<D[], ValueHandler> func;
    private boolean invalid = false;

    public PrecalculatedN(BiConsumer<D[], ValueHandler> func) {
        super(null);
        this.func = func;
    }

    public PrecalculatedN(BiConsumer<D[], ValueHandler> func, T defaultValue) {
        super(defaultValue);
        this.func = func;
    }

    public void updateDependency(D[] dependencies) {
        if(this.dependencies == null || this.dependencies.length != dependencies.length) {
            this.dependencies = Arrays.copyOf(dependencies, dependencies.length);
            dependencyStamps = new int[dependencies.length];
            Arrays.fill(dependencyStamps, -1);
        } else {
            for(int i = 0; i < dependencies.length; ++i) {
                this.dependencies[i] = dependencies[i];
                if(dependencies[i] == null)
                    this.dependencyStamps[i] = -1;
            }
        }
    }

    public T get() {
        if(dependencies == null) {
            value = defaultValue;
        } else {
            var changed = invalid;
            for(int i = 0; i < dependencies.length; ++i) {
                if(dependencies[i] == null)
                    continue;
                var stamp = dependencies[i].getStamp();
                if(stamp != dependencyStamps[i]) {
                    changed = true;
                    dependencyStamps[i] = stamp;
                }
            }
            if(changed) {
                ++ourStamp;
                func.accept(dependencies, handler);
                invalid = false;
            }
        }
        return value;
    }

    @Override
    public int getStamp() {
        if(dependencies == null)
            return -1;
        if(invalid)
            return ourStamp + 1;
        for(int i = 0; i < dependencies.length; ++i) {
            if(dependencies[i] == null)
                continue;
            if(dependencies[i].getStamp() != dependencyStamps[i])
                return ourStamp + 1;
        }
        return ourStamp;
    }

    public void invalidate() {
        invalid = true;
    }
}
