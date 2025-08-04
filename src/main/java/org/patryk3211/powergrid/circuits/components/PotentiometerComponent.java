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
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Components;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPackets;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;
import org.patryk3211.powergrid.network.packets.UpdateComponentBiPacket;
import org.patryk3211.powergrid.utility.CustomValueSettingsScreen;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public class PotentiometerComponent extends OrientableComponent implements IInteractableComponent, IRenderedComponent {
    public static final FloatProperty RESISTANCE = new FloatProperty(PowerGrid.MOD_ID, "potentiometer_resistance", 1000, 100, 100000);
    public static final IntProperty VALUE = new IntProperty(PowerGrid.MOD_ID, "potentiometer_value", 50, 0, 100);

    @Environment(EnvType.CLIENT)
    private static ValueSettingsBoard BOARD;

    public PotentiometerComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RESISTANCE, VALUE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var R = placed.get(RESISTANCE);
        // Remap setting to prevent infinite conductance.
        var V = MathHelper.map(placed.get(VALUE) / 100.0f, 0, 1, 0.01f, 0.99f);
        var wire1 = builder.connect(R * V, builder.terminalNode(0), builder.terminalNode(1));
        var wire2 = builder.connect(R * (1 - V), builder.terminalNode(1), builder.terminalNode(2));

        placed.add(wire1);
        placed.add(wire2);

        thermals.builder()
                .setMaxPower(10, 125)
                .setThermalMass(0.005f)
                .addHeatSource(wire1)
                .addHeatSource(wire2);
    }

    @Override
    public VoxelShape getShape(@NotNull PlacedComponent placed) {
        return IInteractableComponent.extrudedFootprint(placed, 3 / 16f);
    }

    @Override
    public ActionResult use(CircuitBoardBlockEntity be, PlacedComponent component, PlayerEntity player) {
        component.onClientWorld(() -> world -> {
            var value = component.get(VALUE);
            if(BOARD == null) {
                BOARD = CustomValueSettingsScreen.makeBoard(
                        Lang.translateDirect("gui.potentiometer.setting"),
                        100, 10,
                        List.of(Components.literal("Value")));
            }
            CustomValueSettingsScreen.beginInteraction(() -> new CustomValueSettingsScreen(
                    be.getPos(), BOARD, new ValueSettingsBehaviour.ValueSettings(0, value),
                    setting -> {
                        component.set(VALUE, setting.value());
                        ModdedPackets.getChannel().sendToServer(new UpdateComponentBiPacket(be, component, VALUE));
                    }
            ));
        });
        return ActionResult.SUCCESS;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return;

        var R = placed.get(RESISTANCE);
        // Remap setting to prevent infinite conductance.
        var V = MathHelper.map(placed.get(VALUE) / 100.0f, 0, 1, 0.01f, 0.99f);
        var wire1 = (ElectricWire) placed.wires.get(0);
        var wire2 = (ElectricWire) placed.wires.get(1);

        wire1.setResistance(R * V);
        wire2.setResistance(R * (1 - V));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        var buffer = CachedBufferer.partial(ModdedPartialModels.POTENTIOMETER_KNOB, be.getCachedState());
        var angle = 135 - 135 * 2 * (placed.get(VALUE) / 100.0f);
        buffer
                .translate(2.5 / 16, 0, 2.5 / 16)
                .rotateY(angle)
                .translate(-2.5 / 16, 0, -2.5 / 16)
                .light(light)
                .renderInto(ms, bufferSource.getBuffer(RenderLayer.getSolid()));
    }
}
