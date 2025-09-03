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
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.special.CapacitorWire;

public class CapacitorComponent extends OrientableComponent {
    public static final FloatProperty CAPACITANCE = new FloatProperty(PowerGrid.MOD_ID, "capacitor_value", 0.1f, 1e-4f, 1.0f);
    private static final ChargeProperty CHARGE = new ChargeProperty(PowerGrid.MOD_ID, "charge");

    public CapacitorComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(CAPACITANCE, CHARGE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var capacitorWire = new CapacitorWire(placed.get(CAPACITANCE), builder.terminalNode(0), builder.terminalNode(1));
        capacitorWire.setVoltage(placed.get(CHARGE));
        builder.add(capacitorWire);
        placed.add(capacitorWire);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(!placed.wires.isEmpty()) {
            // Make charge persistent
            placed.set(CHARGE, placed.wires.get(0).potentialDifference());
        }
        return true;
    }

    private static class ChargeProperty extends FloatProperty {
        public ChargeProperty(String namespace, String name) {
            super(namespace, name, 0, 0, 0);
        }

        @Override
        public Float parse(String str) throws NumberFormatException {
            // Not allowed
            return 0.0f;
        }

        @Override
        public boolean isHidden() {
            return true;
        }

        @Override
        protected float limit(float value) {
            return value;
        }
    }
}
