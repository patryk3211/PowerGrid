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
package org.patryk3211.powergrid.chemistry.reagent;

import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

public class ReagentRegistry {
    public static final Identifier DEFAULT_ID = new Identifier(PowerGrid.MOD_ID, "empty");
    public static final RegistryKey<Registry<Reagent>> REGISTRY_KEY = RegistryKey.ofRegistry(new Identifier(PowerGrid.MOD_ID, "reagents"));
    public static final Registry<Reagent> REGISTRY = FabricRegistryBuilder
            .createDefaulted(REGISTRY_KEY, DEFAULT_ID)
            .attribute(RegistryAttribute.SYNCED)
            .buildAndRegister();

    public static Reagent DEFAULT;

    public static void init() {
        // Register the default reagent
        DEFAULT = Registry.register(REGISTRY, DEFAULT_ID, new Reagent(Reagent.Properties.EMPTY));
    }
}
