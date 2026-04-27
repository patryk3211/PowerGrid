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

import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedConfigs;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.special.RelaySwitchWire;

import static org.patryk3211.powergrid.circuits.components.RelayComponent.*;

public class DoubleRelayComponent extends MirrorableComponent {
    public DoubleRelayComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(THRESHOLD_VOLTAGE, STATE, THRESHOLD_CURRENT, current(16));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        // Target power is 1.2 W
        var onCurrent = placed.get(THRESHOLD_CURRENT);
        var offCurrent = onCurrent * ModdedConfigs.server().electricity.holdingCurrentPercent.getF();
        var resistance = placed.get(THRESHOLD_VOLTAGE) / onCurrent;
        var coilWire = builder.connect(resistance, builder.terminalNode(0), builder.terminalNode(1));

        final var switchResistance = 0.05f;
        var state = placed.get(STATE);

        var common1 = builder.terminalNode(3);
        var nc1 = new RelaySwitchWire(switchResistance, common1, builder.terminalNode(2), !state, coilWire, onCurrent, offCurrent, true);
        var no1 = new RelaySwitchWire(switchResistance, common1, builder.terminalNode(4), state, coilWire, onCurrent, offCurrent, false);

        var common2 = builder.terminalNode(6);
        var nc2 = new RelaySwitchWire(switchResistance, common2, builder.terminalNode(5), !state, coilWire, onCurrent, offCurrent, true);
        var no2 = new RelaySwitchWire(switchResistance, common2, builder.terminalNode(7), state, coilWire, onCurrent, offCurrent, false);

        builder.add(nc1);
        builder.add(no1);
        builder.add(nc2);
        builder.add(no2);

        placed.add(nc1);
        placed.add(no1);
        placed.add(nc2);
        placed.add(no2);

        thermals.builder()
                .setMaxCurrent(onCurrent * 2, resistance, 125f)
                .setThermalMass(0.02f)
                .addHeatSource(coilWire);
        thermals.builder()
                .setMaxCurrent(16, switchResistance, 125f)
                .setThermalMass(0.075f)
                .addHeatSource(nc1)
                .addHeatSource(no1);
        thermals.builder()
                .setMaxCurrent(16, switchResistance, 125f)
                .setThermalMass(0.075f)
                .addHeatSource(nc2)
                .addHeatSource(no2);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return true;

        var NC1 = (RelaySwitchWire) placed.wires.get(0);
        var NO1 = (RelaySwitchWire) placed.wires.get(1);
        var NC2 = (RelaySwitchWire) placed.wires.get(2);
        var NO2 = (RelaySwitchWire) placed.wires.get(3);
        if(NO1.wasSwitched() | NC1.wasSwitched() | NO2.wasSwitched() | NC2.wasSwitched()) {
            boolean state = NO1.getState();
            placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(
                    world,
                    placed.getPos(),
                    0.75f,
                    state ? 2.0f : 1.9f
            ));
            placed.set(STATE, state);
        }

        return true;
    }
}
