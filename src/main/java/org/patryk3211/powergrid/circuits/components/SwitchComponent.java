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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;

import java.util.Collection;
import java.util.List;

public class SwitchComponent extends OrientableComponent implements IInteractableComponent {
    public static final BooleanProperty STATE = new BooleanProperty(PowerGrid.MOD_ID, "switch_state");

    public SwitchComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(STATE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
        var wire = builder.connectSwitch(0.150f, builder.terminalNode(0), builder.terminalNode(1), placed.get(STATE));
        placed.add(wire);
        thermals.builder()
                .setMaxPower(30, 150)
                .setThermalMass(0.01f)
                .addHeatSource(wire);
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 2 / 16f);
    }

    @Override
    public InteractionResult use(CircuitBoardBlockEntity be, PlacedComponent placed, Player player) {
        var newState = !placed.get(STATE);
        placed.set(STATE, newState);

        if(be.getLevel().isClientSide) {
            Component.modelChanged(be.getBlockPos());
        } else {
            if(newState) {
                ModdedSoundEvents.MICROSWITCH_ON.playOnServer(be.getLevel(), be.getBlockPos());
            } else {
                ModdedSoundEvents.MICROSWITCH_OFF.playOnServer(be.getLevel(), be.getBlockPos());
            }
            placed.notifyClients(STATE);
            stateUpdated(placed);
        }
        be.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        super.stateUpdated(placed);
        if(placed.wires.isEmpty())
            return;
        ((SwitchedWire) placed.wires.get(0)).setState(placed.get(STATE));
    }

    @Override
    public @NotNull ResourceLocation getModelId(@NotNull PlacedComponent component) {
        return component.get(STATE)
                ? PowerGrid.asResource("switch_on")
                : PowerGrid.asResource("switch");
    }

    @Override
    public @NotNull Collection<ResourceLocation> requestedModels() {
        return List.of(
                PowerGrid.asResource("switch"),
                PowerGrid.asResource("switch_on")
        );
    }
}
