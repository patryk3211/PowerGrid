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

import com.simibubi.create.AllItems;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class Components {
    public static final ComponentEntry<ViaComponent> VIA = REGISTRATE.component("via", ViaComponent::new)
            .footprint(1, 1, b -> b.addPad(0, 0))
            .item(AllItems.COPPER_NUGGET)
            .register();

    public static final ComponentEntry<VacuumTubeComponent> VACUUM_TUBE = REGISTRATE.component("vacuum_tube", VacuumTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1)
                    .addPad(0, 4)
                    .addPad(5, 1)
                    .addPad(5, 4)
                    .withItem(AllItems.ELECTRON_TUBE)
                    .withOutline())
            .item(AllItems.ELECTRON_TUBE)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
