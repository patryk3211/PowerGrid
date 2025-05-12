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

import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.patryk3211.powergrid.PowerGrid;

public class Reagents {
    public static final Reagent EMPTY = ReagentRegistry.DEFAULT;

    public static final Reagent OXYGEN = register("oxygen", new Reagent(Reagent.Properties.OXYGEN));
    public static final Reagent HYDROGEN = register("hydrogen", new Reagent(Reagent.Properties.HYDROGEN));
    public static final Reagent WATER = register("water", new Reagent(Reagent.Properties.WATER));

    private static Reagent register(String name, Reagent reagent) {
        return Registry.register(ReagentRegistry.REGISTRY, new Identifier(PowerGrid.MOD_ID, name), reagent);
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
