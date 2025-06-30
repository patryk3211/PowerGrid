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
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

public class RelayComponent extends Component {
    public static final FloatProperty THRESHOLD_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "relay_threshold", 12, 1, 48);
    public static final BooleanProperty STATE = new BooleanProperty(PowerGrid.MOD_ID, "relay_state");

    public RelayComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        properties.add(THRESHOLD_VOLTAGE, STATE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        // Target current is 100mA
        var resistance = placed.get(THRESHOLD_VOLTAGE) / 0.1f;
        var coilWire = builder.connect(resistance, builder.terminalNode(0), builder.terminalNode(1));

        final var switchResistance = 0.05f;
        var common = builder.terminalNode(3);
        var normallyClosed = builder.connectSwitch(switchResistance, common, builder.terminalNode(2), true);
        var normallyOpen = builder.connectSwitch(switchResistance, common, builder.terminalNode(4), false);

        placed.add(coilWire);
        placed.add(normallyClosed);
        placed.add(normallyOpen);

        super.bake(placed, builder, thermals);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return true;

        var coilWire = placed.wires.get(0);
        var current = Math.abs(coilWire.current());
        var NC = (SwitchedWire) placed.wires.get(1);
        var NO = (SwitchedWire) placed.wires.get(2);
        if(placed.get(STATE) && current < 0.095f) {
            // Below 95mA the relay can turn off
            NC.setState(true);
            NO.setState(false);
            placed.set(STATE, false);
            placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(world, placed.getPos(), 0.75f, 1.9f));
        } else if(!placed.get(STATE) && current > 0.1f) {
            // Above 100mA the relay can turn on
            NC.setState(false);
            NO.setState(true);
            placed.set(STATE, true);
            placed.onServerWorld(() -> world -> ModdedSoundEvents.RELAY_CLICK.playOnServer(world, placed.getPos(), 0.75f, 2.0f));
        }

        return true;
    }
}
