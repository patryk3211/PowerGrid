/*
 * Copyright 2026 patryk3211
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
import org.patryk3211.powergrid.circuits.components.properties.ConstantProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.special.ElectronTubeWire;
import org.patryk3211.powergrid.utility.Unit;

public class VFETComponent extends MirrorableComponent {
    public static final FloatProperty GAIN = new FloatProperty(PowerGrid.MOD_ID, "vfet_gain", 10, 1, 100);
    public static final FloatProperty K_G = new FloatProperty(PowerGrid.MOD_ID, "vfet_kg", 250, 10, 500);
    public static final ConstantProperty SATURATION_CURRENT = new ConstantProperty(PowerGrid.MOD_ID, "vfet_isat",
            Unit.CURRENT.formatWithPrefixes(0.5).component());

    public VFETComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(GAIN, K_G, SATURATION_CURRENT, power(10), voltage(60));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new VFETWire(
                placed.get(GAIN), placed.get(K_G), 0.5f,
                builder.terminalNode(1), // Source
                builder.terminalNode(0), // Drain
                builder.terminalNode(2)  // Gate
        );
        builder.add(wire);
        placed.add(wire);

        thermals.builder()
                .addHeatSource(wire)
                .setThermalMass(0.01f)
                .setMaxPower(10, 125);
    }

    public static class VFETWire extends ElectronTubeWire {
        public VFETWire(float gain, float kg1, float saturationCurrent, IElectricNode cathode, IElectricNode anode, IElectricNode grid) {
            super(gain, kg1, 600, 300, 1.5f, saturationCurrent, cathode, anode, grid);
        }

        @Override
        public double internalPower() {
            var power = super.internalPower();
            if(Math.abs(potentialDifference()) > 60)
                power += 20;
            return power;
        }
    }
}
