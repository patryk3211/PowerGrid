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
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;

public class RedstoneEmitterComponent extends EdgeComponent implements IRedstoneComponent {
    public static final IntProperty LEVEL = (IntProperty) new IntProperty(PowerGrid.MOD_ID, "redstone_emitter_level", 0, 0, 15).hidden();
    public static final BooleanProperty DIGITAL = new BooleanProperty(PowerGrid.MOD_ID, "redstone_emitter_digital");

    public RedstoneEmitterComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LEVEL, DIGITAL);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connect(1000f, builder.terminalNode(0), builder.terminalNode(1));
        placed.add(wire);
    }

    @Override
    public boolean isEmitter() {
        return true;
    }

    @Override
    public int getEmittedLevel(@NotNull PlacedComponent component) {
        return component.get(LEVEL);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return true;
        var wire = placed.wires.get(0);
        // TODO: Right now polarity doesn't matter but this might change.
        int redstoneLevel;
        if(placed.get(DIGITAL)) {
            redstoneLevel = Math.abs(wire.potentialDifference()) > 3.3f ? 15 : 0;
        } else {
            redstoneLevel = MathHelper.clamp((int) Math.floor(Math.abs(wire.potentialDifference() * 15 / 5)), 0, 15);
        }

        if(redstoneLevel != placed.get(LEVEL)) {
            placed.set(LEVEL, redstoneLevel);
            IRedstoneComponent.notifyNeighbours(placed);
        }

        return true;
    }
}
