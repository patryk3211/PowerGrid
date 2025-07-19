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
import net.minecraft.item.Items;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

public class Components {
    public static final ComponentEntry<ViaComponent> VIA = REGISTRATE.component("via", ViaComponent::new)
            .footprint(1, 1, b -> b.addPad(0, 0))
            .item(AllItems.COPPER_NUGGET)
            .register();

    public static final ComponentEntry<ElectronTubeComponent> ELECTRON_TUBE = REGISTRATE.component("electron_tube", ElectronTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 0, 2, "Anode")
                    .addPad(0, 2, 0, "Cathode")
                    .addPad(2, 0, 1, "Grid")
                    .addPad(2, 2, 3, "Heater")
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
                    .withItem(ModdedBlocks.LV_SWITCH::asItem)
                    .withOutline())
            .item(ModdedBlocks.LV_SWITCH)
            .register();

    public static final ComponentEntry<RelayComponent> RELAY = REGISTRATE.component("relay", RelayComponent::new)
            .footprint(5, 4, b -> b
                    .addPad(0, 0, 0, "Coil")
                    .addPad(0, 3, 1, "Coil")
                    .addPad(3, 0, 2, "Normally Closed")
                    .addPad(4, 1, 3, "Common")
                    .addPad(4, 2, 3, "Common")
                    .addPad(3, 3, 4, "Normally Open")
                    .withItem(ModdedItems.RELAY)
                    .withOutline()
            )
            .item(ModdedItems.RELAY)
            .register();

    public static final ComponentEntry<ResistorComponent> RESISTOR = REGISTRATE.component("resistor", ResistorComponent::new)
            .footprint(6, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(5, 1, 1)
                    .withItem(ModdedItems.RESISTOR)
                    .withOutline()
            )
            .item(ModdedItems.RESISTOR)
            .register();

    public static final ComponentEntry<RedstoneRelayComponent> REDSTONE_RELAY = REGISTRATE.component("redstone_relay", RedstoneRelayComponent::new)
            .footprint(3, 5, b -> b
                    .addPad(1, 0, 0)
                    .addPad(1, 4, 1)
                    .withItem(ModdedItems.REDSTONE_RELAY)
                    .withArrow()
                    .withOutline()
            )
            .item(ModdedItems.REDSTONE_RELAY)
            .register();

    public static final ComponentEntry<RedstoneEmitterComponent> REDSTONE_EMITTER = REGISTRATE.component("redstone_emitter", RedstoneEmitterComponent::new)
            .footprint(3, 5, b -> b
                    .addPad(1, 0, 0)
                    .addPad(1, 4, 1)
                    .withItem(ModdedBlocks.ANDESITE_VOLTAGE_METER::asItem)
                    .withArrow()
                    .withOutline()
            )
            .item(ModdedBlocks.ANDESITE_VOLTAGE_METER)
            .register();

    public static final ComponentEntry<DiodeComponent> DIODE = REGISTRATE.component("diode", DiodeComponent::new)
            .footprint(6, 3, b -> b
                    .addPad(0, 1, 0, "Anode")
                    .addPad(5, 1, 1, "Cathode")
                    .withItem(ModdedItems.DIODE)
                    .withOutline()
            )
            .item(ModdedItems.DIODE)
            .register();

    public static final ComponentEntry<CapacitorComponent> CAPACITOR = REGISTRATE.component("capacitor", CapacitorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem(ModdedItems.CAPACITOR)
                    .withOutline()
            )
            .item(ModdedItems.CAPACITOR)
            .register();

    public static final ComponentEntry<LEDComponent> LED = REGISTRATE.component("led", LEDComponent::new)
            .footprint(2, 2, b -> b
                    .addPad(0, 0, 0, "Anode")
                    .addPad(1, 1, 1, "Cathode")
                    .withItem(ModdedItems.LED)
                    .withOutline()
            )
            .item(ModdedItems.LED)
            .register();

    public static final ComponentEntry<ButtonComponent> BUTTON = REGISTRATE.component("button", ButtonComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem(ModdedBlocks.LV_BUTTON::asItem)
                    .withOutline()
            )
            .item(ModdedBlocks.LV_BUTTON)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
