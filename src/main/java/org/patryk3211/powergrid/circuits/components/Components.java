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
import com.tterrag.registrate.util.entry.RegistryEntry;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

@SuppressWarnings("unused")
public class Components {
    public static final RegistryEntry<ViaComponent> VIA = REGISTRATE.component("via", ViaComponent::new)
            .footprint(1, 1, b -> b.addPad(0, 0))
            .item(AllItems.COPPER_NUGGET)
            .register();

    public static final RegistryEntry<ElectronTubeComponent> ELECTRON_TUBE = REGISTRATE.component("electron_tube", ElectronTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 0, 2, "Anode", "A")
                    .addPad(1, 1, 0, "Cathode", "C")
                    .addPad(2, 0, 1, "Grid", "G")
                    .addPad(0, 2, 3, "Heater", "H")
                    .addPad(2, 2, 4, "Heater", "H")
                    .withItem().withOutline())
            .item(AllItems.ELECTRON_TUBE)
            .register();

    public static final RegistryEntry<RegulatorTubeComponent> REGULATOR_TUBE = REGISTRATE.component("regulator_tube", RegulatorTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0, "Anode", "+")
                    .addPad(2, 1, 1, "Cathode", "-")
                    .withItem().withOutline())
            .item(ModdedItems.REGULATOR_TUBE)
            .register();

    public static final RegistryEntry<BarretterTubeComponent> BARRETTER_TUBE = REGISTRATE.component("barretter_tube", BarretterTubeComponent::new)
            .footprint(2, 2, b -> b
                    .addPad(0, 0, 0)
                    .addPad(1, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.BARRETTER_TUBE)
            .register();

    public static final RegistryEntry<NeonBulbComponent> NEON_BULB = REGISTRATE.component("neon_bulb", NeonBulbComponent::new)
            .footprint(2, 2, b -> b
                    .addPad(0, 0, 0)
                    .addPad(1, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.NEON_BULB)
            .register();

    public static final RegistryEntry<LightBulbComponent> LIGHT_BULB = REGISTRATE.component("light_bulb", LightBulbComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.LV_LIGHT_BULB)
            .register();

    public static final RegistryEntry<ConnectorComponent> CONNECTOR = REGISTRATE.component("connector", ConnectorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(1, 1, 0)
                    .withOutline())
            .item(ModdedBlocks.WIRE_CONNECTOR)
            .register();

    public static final RegistryEntry<SwitchComponent> SWITCH = REGISTRATE.component("switch", SwitchComponent::new)
            .footprint(4, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(3, 1, 1)
                    .withItem().withOutline())
            .item(ModdedBlocks.LV_SWITCH)
            .register();

    public static final RegistryEntry<RelayComponent> RELAY = REGISTRATE.component("relay", RelayComponent::new)
            .footprint(4, 3, b -> b
                    .addPad(0, 0, 0, "Coil", null)
                    .addPad(0, 2, 1, "Coil", null)
                    .addPad(2, 0, 2, "Normally Closed", "NC")
                    .addPad(3, 1, 3, "Common", "CC")
                    .addPad(2, 2, 4, "Normally Open", "NO")
                    .withItem().withOutline()
            )
            .item(ModdedItems.RELAY)
            .register();

    public static final RegistryEntry<ResistorComponent> RESISTOR = REGISTRATE.component("resistor", ResistorComponent::new)
            .footprint(5, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(4, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedItems.RESISTOR)
            .register();

    public static final RegistryEntry<RedstoneRelayComponent> REDSTONE_RELAY = REGISTRATE.component("redstone_relay", RedstoneRelayComponent::new)
            .footprint(3, 5, b -> b
                    .addPad(1, 0, 0)
                    .addPad(1, 4, 1)
                    .withItem().withArrow().withOutline()
            )
            .item(ModdedItems.REDSTONE_RELAY)
            .register();

    public static final RegistryEntry<VoltageGaugeComponent> VOLTAGE_GAUGE = REGISTRATE.component("voltage_gauge", VoltageGaugeComponent::new)
            .footprint(5, 5, b -> b
                    .addPad(2, 0, 0)
                    .addPad(2, 4, 1)
                    .withItem().withArrow().withOutline()
            )
            .item(ModdedBlocks.VOLTAGE_METER)
            .register();

    public static final RegistryEntry<CurrentGaugeComponent> CURRENT_GAUGE = REGISTRATE.component("current_gauge", CurrentGaugeComponent::new)
            .footprint(5, 5, b -> b
                    .addPad(2, 0, 0)
                    .addPad(2, 4, 1)
                    .withItem().withArrow().withOutline()
            )
            .item(ModdedBlocks.CURRENT_METER)
            .register();

    public static final RegistryEntry<DiodeComponent> DIODE = REGISTRATE.component("diode", DiodeComponent::new)
            .footprint(5, 3, b -> b
                    .addPad(0, 1, 0, "Cathode", "-")
                    .addPad(4, 1, 1, "Anode", "+")
                    .withItem().withOutline()
            )
            .item(ModdedItems.DIODE)
            .register();

    public static final RegistryEntry<CapacitorComponent> CAPACITOR = REGISTRATE.component("capacitor", CapacitorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedItems.CAPACITOR)
            .register();

    public static final RegistryEntry<InductorComponent> INDUCTOR = REGISTRATE.component("inductor", InductorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.COPPER_COIL)
            .register();

    public static final RegistryEntry<ButtonComponent> BUTTON = REGISTRATE.component("button", ButtonComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedBlocks.LV_BUTTON)
            .register();

    public static final RegistryEntry<PotentiometerComponent> POTENTIOMETER = REGISTRATE.component("potentiometer", PotentiometerComponent::new)
            .footprint(5, 5, b -> b
                    .addPad(1, 2, 0)
                    .addPad(2, 3, 1)
                    .addPad(3, 2, 2)
                    .withItem().withOutline()
            )
            .item(ModdedItems.POTENTIOMETER)
            .register();

    public static final RegistryEntry<VaristorComponent> VARISTOR = REGISTRATE.component("varistor", VaristorComponent::new)
            .footprint(4, 4, b -> b
                    .addPad(0, 1, 0)
                    .addPad(3, 2, 1)
                    .withItem().withOutline()
            )
            .item(ModdedItems.VARISTOR)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
