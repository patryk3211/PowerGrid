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

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.patryk3211.powergrid.PowerGrid;

public class ComponentRegistry {
    public static final RegistryKey<Registry<Component>> REGISTRY_KEY = RegistryKey.ofRegistry(PowerGrid.asResource("components"));
    public static final Registry<Component> REGISTRY = FabricRegistryBuilder
            .createSimple(REGISTRY_KEY)
            .buildAndRegister();

    @SuppressWarnings("EmptyMethod")
    public static void init() { /* Initialize static fields. */ }
}
