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
import net.minecraft.text.Text;
import org.patryk3211.powergrid.collections.ModdedBlocks;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class Components {
    public static final ComponentEntry<ViaComponent> VIA = REGISTRATE.component("via", ViaComponent::new)
            .footprint(1, 1, b -> b.addPad(0, 0))
            .item(AllItems.COPPER_NUGGET)
            .register();

    public static final ComponentEntry<ElectronTubeComponent> ELECTRON_TUBE = REGISTRATE.component("electron_tube", ElectronTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 0, 2, Text.of("Anode"))
                    .addPad(0, 2, 0, Text.of("Cathode"))
                    .addPad(2, 0, 1, Text.of("Grid"))
                    .addPad(2, 2, 3, Text.of("Heater"))
                    .withItem(AllItems.ELECTRON_TUBE)
                    .withOutline())
            .item(AllItems.ELECTRON_TUBE)
            .register();

    public static final ComponentEntry<ConnectorComponent> CONNECTOR = REGISTRATE.component("connector", ConnectorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(1, 1, 0)
                    .withOutline())
            .item(ModdedBlocks.WIRE_CONNECTOR)
            .register();

    public static final ComponentEntry<SwitchComponent> SWITCH = REGISTRATE.component("switch", SwitchComponent::new)
            .footprint(4, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(3, 1, 1)
                    .withOutline())
            .item(ModdedBlocks.LV_SWITCH)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
