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
import org.patryk3211.powergrid.electricity.sim.special.ElectronTubeWire;

public class ElectronTubeComponent extends Component {
    // TODO: Value ranges might need balancing
    public static final FloatProperty TUBE_GAIN = new FloatProperty(PowerGrid.MOD_ID, "tube_gain", 5, 1, 50);
    public static final FloatProperty ANODE_RESISTANCE = new FloatProperty(PowerGrid.MOD_ID, "tube_anode_resistance", 5000, 100, 10000);
    public static final FloatProperty SATURATION_CURRENT = new FloatProperty(PowerGrid.MOD_ID, "tube_saturation_current", 0.01f, 0.0001f, 10);

    public ElectronTubeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        properties.add(TUBE_GAIN, ANODE_RESISTANCE, SATURATION_CURRENT);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder) {
        var perveance = ElectronTubeWire.calculatePerveance(1,
                placed.get(TUBE_GAIN),
                1 / placed.get(ANODE_RESISTANCE));
        var tube = new ElectronTubeWire(
                placed.get(TUBE_GAIN), perveance, placed.get(SATURATION_CURRENT),
                builder.terminalNode(0), // Cathode
                builder.terminalNode(1), // Grid
                builder.terminalNode(2)  // Anode
        );
        builder.add(tube);
    }
}
