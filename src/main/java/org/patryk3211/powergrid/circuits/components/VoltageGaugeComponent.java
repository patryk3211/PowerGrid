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
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.gauge.VoltageGaugeBlockEntity;

import java.util.List;

public class VoltageGaugeComponent extends GaugeComponent {
    public static final FloatProperty MAX_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "voltage_gauge_max", 10.0f, 1.0f, 100.0f);

    public VoltageGaugeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(MAX_VOLTAGE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connect(50_000f, builder.terminalNode(0), builder.terminalNode(1));
        placed.add(wire);
    }

    @Override
    public float getTarget(PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return 0;
        var wire = placed.wires.get(0);
        return Mth.clamp(Math.abs(wire.potentialDifference()) / placed.get(MAX_VOLTAGE), 0, 1.125f);
    }

    public float getValue(PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return 0;
        var wire = placed.wires.get(0);
        return Math.abs(wire.potentialDifference());
    }

    @Override
    public boolean addToGoggleTooltip(PlacedComponent component, List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(component, tooltip, isPlayerSneaking);
        var max = component.get(MAX_VOLTAGE);
        VoltageGaugeBlockEntity.addTooltip(tooltip, getValue(component), max);
        return true;
    }
}
