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

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProxyProvider {
    private static final Map<Class<?>, Object> PROXIES = new HashMap<>();

    public static <T> Optional<T> get(Class<T> clazz) {
        var value = PROXIES.get(clazz);
        return Optional.ofNullable((T) value);
    }

    @NotNull
    public static <T> T getOrThrow(Class<T> clazz) {
        var value = PROXIES.get(clazz);
        if(value == null)
            throw new IllegalArgumentException("Proxy of type " + clazz + " doesn't exist");
        return (T) value;
    }

    public static <T> void add(Class<T> clazz, T proxy) {
        PROXIES.put(clazz, proxy);
    }
}
