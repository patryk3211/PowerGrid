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
package org.patryk3211.powergrid.circuits.components;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;

public class ComponentRegistry {
    public static final ResourceKey<Registry<Component>> REGISTRY_KEY = ResourceKey.createRegistryKey(PowerGrid.asResource("components"));

    @ExpectPlatform
    public static Iterable<Component> entries() {
        throw new AssertionError();
    }

    @NotNull
    @ExpectPlatform
    public static ResourceLocation getId(@NotNull Component component) {
        throw new AssertionError();
    }

    @NotNull
    @ExpectPlatform
    public static Component get(@NotNull ResourceLocation id) {
        throw new AssertionError();
    }
}
