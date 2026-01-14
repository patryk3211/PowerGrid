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
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.PNJunctionWire;

public class DiodeComponent extends VerticallyOrientableComponent {
    private static final ComponentFootprint VERTICAL_FOOTPRINT = new ComponentFootprint.Builder(3, 3, null, "component." + PowerGrid.MOD_ID)
            .addPadSharedText(0, 1, 0, "generic.cathode", "generic.cathode.short")
            .addPadSharedText(2, 1, 1, "generic.anode", "generic.anode.short")
            .withItem().withOutline().build();

    public DiodeComponent(ComponentFootprint footprint) {
        super(footprint, VERTICAL_FOOTPRINT);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        // Variation of 1N4007 diode model
        var pnJunctionWire = new PNJunctionWire(5.47e-9, 0.075f,22, 1.783,
                builder.terminalNode(1), builder.terminalNode(0));
        builder.add(pnJunctionWire);
        thermals.builder()
                .setMaxPower(25, 175)
                .setOverheatTemperature(175)
                .setThermalMass(0.05f)
                .withTemperatureCallback(pnJunctionWire::setTemperatureCelsius)
                .addHeatSource(pnJunctionWire);
    }
}