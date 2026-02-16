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
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.special.BJTWire;

public abstract class BJTComponent extends OrientableComponent {
    public static final FloatProperty GAIN = new FloatProperty(PowerGrid.MOD_ID, "bjt_gain", 20, 5, 100);

    private final boolean pnp;

    public BJTComponent(ComponentFootprint footprint, boolean pnp) {
        super(footprint);
        this.pnp = pnp;
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(GAIN, power(20), voltage(60));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new VLimBJT(
                builder.terminalNode(0), // Collector
                builder.terminalNode(1), // Base
                builder.terminalNode(2), // Emitter
                5.47e-12, placed.get(GAIN), 0.1, pnp
        );
        builder.add(wire);
        placed.add(wire);

        thermals.builder()
                .addHeatSource(wire)
                .setThermalMass(0.01f)
                .setMaxPower(20, 125);
    }

    public static class VLimBJT extends BJTWire {
        public VLimBJT(IElectricNode collector, IElectricNode base, IElectricNode emitter, double Is, double fBeta, double Rs, boolean pnp) {
            super(collector, base, emitter, Is, fBeta, Rs, pnp);
        }

        @Override
        public float power() {
            var power = super.power();
            if(Math.abs(potentialDifference()) > 60 || Math.abs(collectorBase.potentialDifference()) > 60)
                power += 40;
            return power;
        }
    }
}
