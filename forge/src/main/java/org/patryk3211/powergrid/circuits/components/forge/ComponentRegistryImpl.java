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
package org.patryk3211.powergrid.circuits.components.forge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.components.Component;

import java.util.Objects;

public class ComponentRegistryImpl {
    public static Registry<Component> REGISTRY;

    public static Iterable<Component> entries() {
        return REGISTRY;
    }

    @NotNull
    public static ResourceLocation getId(@NotNull Component component) {
        return Objects.requireNonNull(REGISTRY.getKey(component), "This component is not registered");
    }

    @NotNull
    public static Component get(@NotNull ResourceLocation id) {
        return Objects.requireNonNull(REGISTRY.get(id), "This id doesn't exist");
    }
}
