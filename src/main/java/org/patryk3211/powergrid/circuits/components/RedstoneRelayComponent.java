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
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.Collection;
import java.util.List;

public class RedstoneRelayComponent extends EdgeComponent implements IRedstoneComponent {
    public static final float RESISTANCE = 0.1f;
    public static final BooleanProperty POWERED = (BooleanProperty) new BooleanProperty(PowerGrid.MOD_ID, "redstone_relay_powered").hidden();

    public RedstoneRelayComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(POWERED, current(32));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connectSwitch(RESISTANCE, builder.terminalNode(0), builder.terminalNode(1), placed.get(POWERED));
        placed.add(wire);
        thermals.builder()
                .setMaxCurrent(32, RESISTANCE, 125f)
                .setThermalMass(0.075f)
                .addHeatSource(wire);
    }

    @Override
    public boolean isReceiver() {
        return true;
    }

    @Override
    public void receiveRedstone(@NotNull PlacedComponent component, int level) {
        component.set(POWERED, level > 0);
        component.notifyClients(POWERED);
        stateUpdated(component);
    }

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        var base = super.getModelId(component);
        if(component.get(POWERED)) {
            return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "_on");
        }
        return base;
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
                PowerGrid.asResource("redstone_relay"),
                PowerGrid.asResource("redstone_relay_on")
        );
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
        if(placed.wires.isEmpty())
            return;
        var wire = (SwitchedWire) placed.wires.get(0);
        wire.setState(placed.get(POWERED));
    }
}
