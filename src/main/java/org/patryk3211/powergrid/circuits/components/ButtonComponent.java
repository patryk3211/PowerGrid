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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.Collection;
import java.util.List;

public class ButtonComponent extends OrientableComponent implements IInteractableComponent {
    public static final IntProperty STATE = (IntProperty) new IntProperty(PowerGrid.MOD_ID, "button_state", 0, 0, 10).hidden();

    public ButtonComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(STATE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = builder.connectSwitch(0.1f, builder.terminalNode(0), builder.terminalNode(1), false);
        placed.add(wire);
        thermals.builder()
                .setMaxPower(20, 150)
                .setThermalMass(0.01f)
                .addHeatSource(wire);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        var state = placed.get(STATE);
        if(state > 0) {
            --state;
            placed.set(STATE, state);
            if(state == 0) {
                placed.onServerWorld(() -> world -> ModdedSoundEvents.MICROBUTTON_OFF.playOnServer(world, placed.getPos()));
                placed.onClientWorld(() -> world -> modelChanged(placed.getPos()));
            }
        }
        if(!placed.wires.isEmpty()) {
            var wire = (SwitchedWire) placed.wires.get(0);
            wire.setState(state != 0);
        }
        return true;
    }

    @Override
    public @NotNull Identifier getModelId(@NotNull PlacedComponent component) {
        if(component.get(STATE) == 0)
            return PowerGrid.asResource("button");
        else
            return PowerGrid.asResource("button_on");
    }

    @Override
    public @NotNull Collection<Identifier> requestedModels() {
        return List.of(
                PowerGrid.asResource("button"),
                PowerGrid.asResource("button_on")
        );
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 2 / 16f);
    }

    @Override
    public ActionResult use(CircuitBoardBlockEntity be, PlacedComponent component, PlayerEntity player) {
        if(component.get(STATE) == 0) {
            component.onServerWorld(() -> world -> ModdedSoundEvents.MICROBUTTON_ON.playOnServer(world, component.getPos()));
            component.onClientWorld(() -> world -> modelChanged(component.getPos()));
        }
        component.set(STATE, 10);
        return ActionResult.SUCCESS;
    }
}
