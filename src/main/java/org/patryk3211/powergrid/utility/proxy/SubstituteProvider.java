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
package org.patryk3211.powergrid.utility.proxy;

import java.util.HashMap;
import java.util.Map;

public class SubstituteProvider<T> {
    private final Map<Class<?>, T> mappings = new HashMap<>();
    private boolean locked = false;

    public SubstituteProvider() {

    }

    public void register(Class<?> clazz, T value) {
        if(locked)
            throw new IllegalStateException("Cannot register to locked substitute provider");
        mappings.put(clazz, value);
    }

    public void registerDefault(Class<?> clazz, T value) {
        if(locked)
            throw new IllegalStateException("Cannot register to locked substitute provider");
        if(!mappings.containsKey(clazz))
            mappings.put(clazz, value);
    }

    public void lock() {
        this.locked = true;
    }

    public T getObject(Class<?> clazz) {
        if(!mappings.containsKey(clazz))
            throw new IllegalArgumentException("Class " + clazz + " is not registered");
        return mappings.get(clazz);
    }
}
