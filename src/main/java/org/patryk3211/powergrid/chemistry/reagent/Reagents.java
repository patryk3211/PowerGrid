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

import net.minecraft.fluid.Fluids;
import org.patryk3211.powergrid.PowerGridRegistrate;
import org.patryk3211.powergrid.collections.ModdedItems;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class Reagents {
    public static final Reagent EMPTY = ReagentRegistry.DEFAULT;

    public static final ReagentEntry<Reagent> OXYGEN = REGISTRATE.reagent("oxygen", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-218.8f)
                    .boilingPoint(-182.9f)
                    .heatCapacity(29.37f))
            .register();
    public static final ReagentEntry<Reagent> HYDROGEN = REGISTRATE.reagent("hydrogen", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-259.2f)
                    .boilingPoint(-252.8f)
                    .heatCapacity(28.84f))
            .register();
    public static final ReagentEntry<Reagent> WATER = REGISTRATE.reagent("water", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(0.0f)
                    .boilingPoint(100.0f)
                    .heatCapacity(75.38f))
            .fluid(Fluids.WATER)
            .register();
    public static final ReagentEntry<Reagent> NITROGEN = REGISTRATE.reagent("nitrogen", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-209.8f)
                    .boilingPoint(-195.7f)
                    .heatCapacity(29.12f))
            .register();
    public static final ReagentEntry<Reagent> SULFUR = REGISTRATE.reagent("sulfur", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(115.2f)
                    .boilingPoint(444.6f)
                    .heatCapacity(22.75f))
            .item(ModdedItems.SULFUR, 250)
            .register();
    public static final ReagentEntry<Reagent> SULFUR_DIOXIDE = REGISTRATE.reagent("sulfur_dioxide", Reagent::new)
            .properties(properties -> properties
                    .meltingPoint(-72.0f)
                    .boilingPoint(10.0f)
                    .heatCapacity(42.5f))
            .register();
    public static final ReagentEntry<Reagent> SULFUR_TRIOXIDE = simpleReagent("sulfur_trioxide", 16.9f, 45.0f, 61.5f)
            .register();
    public static final ReagentEntry<Reagent> SULFURIC_ACID = simpleReagent("sulfuric_acid", 10.3f, 337.0f, 135.8f)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }

    private static ReagentBuilder<Reagent, PowerGridRegistrate> simpleReagent(String name, float meltingPoint, float boilingPoint, float heatCapacity) {
        return REGISTRATE.reagent(name, Reagent::new)
                .properties(properties -> properties
                        .meltingPoint(meltingPoint)
                        .boilingPoint(boilingPoint)
                        .heatCapacity(heatCapacity));
    }
}
