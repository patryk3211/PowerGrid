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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.schematic.CircuitLayer;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;

public class ViaComponent extends Component {
    private static final ComponentFootprint NODED_FOOTPRINT = new ComponentFootprint.Builder(1, 1)
            .addPad(0, 0, 0).build();

    public ViaComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        if(placed != null) {
            if(placed.x == 0 || placed.y == 0 || placed.x == 15 || placed.y == 15)
                return NODED_FOOTPRINT;
        }
        return super.footprint(placed);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {

    }
}
