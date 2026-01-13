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
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;

import java.util.Collection;
import java.util.List;

public class ResistorComponent extends OrientableComponent {
    public static final FloatProperty RESISTANCE = new FloatProperty(PowerGrid.MOD_ID, "resistor_value", 100f, 1f, 1000_000f);

    private static final ComponentFootprint VERTICAL_FOOTPRINT = new ComponentFootprint.Builder(3, 3)
            .addPad(0, 1, 0).addPad(2, 1, 1).withItem().withOutline().build();
    private static final ResourceLocation MODEL_DEFAULT = PowerGrid.asResource("resistor");
    private static final ResourceLocation MODEL_VERTICAL = PowerGrid.asResource("resistor_vertical");

    public ResistorComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RESISTANCE, VERTICAL, power(25));
    }

    @Override
    public ComponentFootprint footprint(@Nullable PlacedComponent placed) {
        if(placed != null && placed.get(VERTICAL)) {
            return VERTICAL_FOOTPRINT.rotated(placed.get(ORIENTATION));
        }
        return super.footprint(placed);
    }

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return component.get(VERTICAL) ? MODEL_VERTICAL : MODEL_DEFAULT;
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(MODEL_DEFAULT, MODEL_VERTICAL);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connect(placed.get(RESISTANCE), builder.terminalNode(0), builder.terminalNode(1));
        thermals.builder()
                .setThermalMass(0.05f)
                .setMaxPower(25, 125f)
                .addHeatSource(wire);
    }

    @Override
    public boolean rotate(@NotNull PlacedComponent placed, boolean counterClockwise) {
        if(!counterClockwise) {
            if (!placed.get(VERTICAL)) {
                placed.set(VERTICAL, true);
                return true;
            }
            placed.set(VERTICAL, false);
        } else {
            if(placed.get(VERTICAL)) {
                placed.set(VERTICAL, false);
                return true;
            }
            placed.set(VERTICAL, true);
        }
        return super.rotate(placed, counterClockwise);
    }
}
