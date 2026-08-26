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
import net.minecraft.world.item.Items;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.collections.ModdedItems;
import org.patryk3211.powergrid.collections.ModdedTags;

import static org.patryk3211.powergrid.PowerGrid.REGISTRATE;

@SuppressWarnings("unused")
public class Components {
    public static final RegistryEntry<Component, ViaComponent> VIA = REGISTRATE.component("via", ViaComponent::new)
            .footprint(1, 1, b -> b.addPad(0, 0))
            .item(AllItems.COPPER_NUGGET, ModdedTags.nuggets("copper"))
            .register();

    public static final RegistryEntry<Component, LabelComponent> LABEL = REGISTRATE.component("label", LabelComponent::new)
            .footprint(3, 1, b -> b.withOutline())
            .item(Items.PAPER)
            .register();

    public static final RegistryEntry<Component, ElectronTubeComponent> ELECTRON_TUBE = REGISTRATE.component("electron_tube", ElectronTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 0, 2, "Anode", "A")
                    .addPad(1, 1, 0, "Cathode", "C")
                    .addPad(2, 0, 1, "Grid", "G")
                    .addPad(0, 2, 3, "Heater", "H")
                    .addPadSharedText(2, 2, 4, "electron_tube.3", "electron_tube.3.short")
                    .withItem().withOutline())
            .item(AllItems.ELECTRON_TUBE)
            .register();

    public static final RegistryEntry<Component, VFETComponent> VFET = REGISTRATE.component("vfet", VFETComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 0, 0, "Drain", "D")
                    .addPad(2, 0, 1, "Source", "S")
                    .addPad(1, 2, 2, "Gate", "G")
                    .withItem().withOutline())
            .item(ModdedItems.VFET)
            .register();

    public static final RegistryEntry<Component, NPNComponent> BJT_NPN = REGISTRATE.component("bjt_npn", NPNComponent::new)
            .footprint(3, 2, b -> b
                    .addPad(0, 0, 0, "Collector", "C")
                    .addPad(2, 0, 2, "Emitter", "E")
                    .addPad(1, 1, 1, "Base", "B")
                    .withItem().withOutline())
            .item(ModdedItems.BJT_NPN)
            .register();

    public static final RegistryEntry<Component, PNPComponent> BJT_PNP = REGISTRATE.component("bjt_pnp", PNPComponent::new)
            .footprint(3, 2, b -> b
                    .addPad(2, 0, 0, "Collector", "C")
                    .addPad(0, 0, 2, "Emitter", "E")
                    .addPad(1, 1, 1, "Base", "B")
                    .withItem().withOutline())
            .item(ModdedItems.BJT_PNP)
            .register();

    public static final RegistryEntry<Component, RegulatorTubeComponent> REGULATOR_TUBE = REGISTRATE.component("regulator_tube", RegulatorTubeComponent::new)
            .footprint(3, 3, b -> b
                    .addPadSharedText(0, 1, 0, "generic.anode", "generic.anode.short")
                    .addPadSharedText(2, 1, 1, "generic.cathode", "generic.cathode.short")
                    .withItem().withOutline())
            .item(ModdedItems.REGULATOR_TUBE)
            .register();

    public static final RegistryEntry<Component, BarretterTubeComponent> BARRETTER_TUBE = REGISTRATE.component("barretter_tube", BarretterTubeComponent::new)
            .footprint(2, 2, b -> b
                    .addPad(0, 0, 0)
                    .addPad(1, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.BARRETTER_TUBE)
            .register();

    public static final RegistryEntry<Component, NeonBulbComponent> NEON_BULB = REGISTRATE.component("neon_bulb", NeonBulbComponent::new)
            .footprint(2, 2, b -> b
                    .addPad(0, 0, 0)
                    .addPad(1, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.NEON_BULB)
            .register();

    public static final RegistryEntry<Component, LightBulbComponent> LIGHT_BULB = REGISTRATE.component("light_bulb", LightBulbComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.LV_LIGHT_BULB)
            .register();

    public static final RegistryEntry<Component, ConnectorComponent> CONNECTOR = REGISTRATE.component("connector", ConnectorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(1, 1, 0)
                    .withOutline())
            .item(ModdedBlocks.WIRE_CONNECTOR)
            .register();

    public static final RegistryEntry<Component, SwitchComponent> SWITCH = REGISTRATE.component("switch", SwitchComponent::new)
            .footprint(4, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(3, 1, 1)
                    .withItem().withOutline())
            .item(ModdedBlocks.LV_SWITCH)
            .register();

    public static final RegistryEntry<Component, RelayComponent> RELAY = REGISTRATE.component("relay", RelayComponent::new)
            .footprint(4, 3, b -> b
                    .addPadSharedText(0, 0, 0, "relay.coil", null)
                    .addPadSharedText(0, 2, 1, "relay.coil", null)
                    .addPadSharedText(2, 0, 2, "relay.nc", "relay.nc.short")
                    .addPadSharedText(3, 1, 3, "relay.cc", "relay.cc.short")
                    .addPadSharedText(2, 2, 4, "relay.no", "relay.no.short")
                    .withItem().withOutline()
            )
            .item(ModdedItems.RELAY)
            .register();

    public static final RegistryEntry<Component, DoubleRelayComponent> RELAY_DPDT = REGISTRATE.component("relay_dpdt", DoubleRelayComponent::new)
            .footprint(5, 3, b -> b
                    .addPadSharedText(0, 0, 0, "relay.coil", null)
                    .addPadSharedText(0, 2, 1, "relay.coil", null)
                    .addPadSharedText(2, 0, 2, "relay.nc", "relay.nc.short")
                    .addPadSharedText(2, 1, 3, "relay.cc", "relay.cc.short")
                    .addPadSharedText(2, 2, 4, "relay.no", "relay.no.short")
                    .addPadSharedText(4, 0, 5, "relay.nc", "relay.nc.short")
                    .addPadSharedText(4, 1, 6, "relay.cc", "relay.cc.short")
                    .addPadSharedText(4, 2, 7, "relay.no", "relay.no.short")
                    .withItem().withOutline()
            )
            .item(ModdedItems.RELAY_DPDT)
            .register();

    public static final RegistryEntry<Component, ResistorComponent> RESISTOR = REGISTRATE.component("resistor", ResistorComponent::new)
            .footprint(5, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(4, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedItems.RESISTOR)
            .register();

    public static final RegistryEntry<Component, RedstoneRelayComponent> REDSTONE_RELAY = REGISTRATE.component("redstone_relay", RedstoneRelayComponent::new)
            .footprint(3, 5, b -> b
                    .addPad(1, 0, 0)
                    .addPad(1, 4, 1)
                    .withItem().withArrow().withOutline()
            )
            .item(ModdedItems.REDSTONE_RELAY)
            .register();

    public static final RegistryEntry<Component, VoltageGaugeComponent> VOLTAGE_GAUGE = REGISTRATE.component("voltage_gauge", VoltageGaugeComponent::new)
            .footprint(5, 5, b -> b
                    .addPad(2, 0, 0)
                    .addPad(2, 4, 1)
                    .withItem().withArrow().withOutline()
            )
            .item(ModdedBlocks.VOLTAGE_METER)
            .register();

    public static final RegistryEntry<Component, CurrentGaugeComponent> CURRENT_GAUGE = REGISTRATE.component("current_gauge", CurrentGaugeComponent::new)
            .footprint(5, 5, b -> b
                    .addPad(2, 0, 0)
                    .addPad(2, 4, 1)
                    .withItem().withArrow().withOutline()
            )
            .item(ModdedBlocks.CURRENT_METER)
            .register();

    public static final RegistryEntry<Component, DiodeComponent> DIODE = REGISTRATE.component("diode", DiodeComponent::new)
            .footprint(5, 3, b -> b
                    .addPadSharedText(0, 1, 0, "generic.cathode", "generic.cathode.short")
                    .addPadSharedText(4, 1, 1, "generic.anode", "generic.anode.short")
                    .withItem().withOutline()
            )
            .item(ModdedItems.DIODE)
            .register();

    public static final RegistryEntry<Component, CapacitorComponent> CAPACITOR = REGISTRATE.component("capacitor", CapacitorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedItems.CAPACITOR)
            .register();

    public static final RegistryEntry<Component, InductorComponent> INDUCTOR = REGISTRATE.component("inductor", InductorComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline())
            .item(ModdedItems.COPPER_COIL)
            .register();

    public static final RegistryEntry<Component, ButtonComponent> BUTTON = REGISTRATE.component("button", ButtonComponent::new)
            .footprint(3, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(2, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedBlocks.LV_BUTTON)
            .register();

    public static final RegistryEntry<Component, PotentiometerComponent> POTENTIOMETER = REGISTRATE.component("potentiometer", PotentiometerComponent::new)
            .footprint(5, 5, b -> b
                    .addPad(1, 2, 0)
                    .addPad(2, 3, 1)
                    .addPad(3, 2, 2)
                    .withItem().withOutline()
            )
            .item(ModdedItems.POTENTIOMETER)
            .register();

    public static final RegistryEntry<Component, VaristorComponent> VARISTOR = REGISTRATE.component("varistor", VaristorComponent::new)
            .footprint(4, 4, b -> b
                    .addPad(0, 1, 0)
                    .addPad(3, 2, 1)
                    .withItem().withOutline()
            )
            .item(ModdedItems.VARISTOR)
            .register();

    public static final RegistryEntry<Component, FuseHolderComponent> FUSE_HOLDER = REGISTRATE.component("fuse_holder", FuseHolderComponent::new)
            .footprint(4, 3, b -> b
                    .addPad(0, 1, 0)
                    .addPad(3, 1, 1)
                    .withItem().withOutline()
            )
            .item(ModdedBlocks.FUSE_HOLDER)
            .register();

    public static final RegistryEntry<Component, ModularDisplayComponent> DISPLAY_MODULE = REGISTRATE.component("display_module", ModularDisplayComponent::new)
            .footprint(4, 4, b -> b
                    .addPadSharedText(0, 1, 0, "generic.positive", "generic.positive.short")
                    .addPadSharedText(3, 2, 1, "generic.negative", "generic.negative.short")
                    .addPadSharedText(2, 0, 2, "display_module.reset", "display_module.reset.short")
                    .withItem().withOutline()
            )
            .item(ModdedItems.DISPLAY_MODULE)
            .register();

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
