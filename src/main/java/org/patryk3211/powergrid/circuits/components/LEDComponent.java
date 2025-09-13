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
import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.sim.special.DiodeWire;
import org.patryk3211.powergrid.utility.Unit;

public class LEDComponent extends OrientableComponent implements IRenderedComponent {
    public static final IntProperty RED = new IntProperty(PowerGrid.MOD_ID, "led_red", 255, 0, 255);
    public static final IntProperty GREEN = new IntProperty(PowerGrid.MOD_ID, "led_green", 0, 0, 255);
    public static final IntProperty BLUE = new IntProperty(PowerGrid.MOD_ID, "led_blue", 0, 0, 255);
    public static final CalculatedProperty<Float> FORWARD_VOLTAGE = new CalculatedProperty<>(PowerGrid.MOD_ID, "led_vf",
            c -> Mth.clamp(c.get(RED) / 255f * 2.0f + c.get(GREEN) / 255f * 2.4f + c.get(BLUE) / 255f * 3.0f, 2.0f, 4.0f),
            v -> String.format("%.1f %s", v, Unit.VOLTAGE.string()));
    public static final CalculatedProperty<Float> FORWARD_CURRENT = new CalculatedProperty<>(PowerGrid.MOD_ID, "led_if",
            c -> Mth.clamp(c.get(RED) / 255f * 0.005f + c.get(GREEN) / 255f * 0.005f + c.get(BLUE) / 255f * 0.005f, 0.005f, 0.010f),
            i -> String.format("%.1f m%s", i * 1000, Unit.CURRENT.string()));

    public LEDComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RED, GREEN, BLUE, FORWARD_VOLTAGE, FORWARD_CURRENT);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var Vf = placed.get(FORWARD_VOLTAGE);
        var If = placed.get(FORWARD_CURRENT);
        var wire = new DiodeWire(1e-3f, Vf, builder.terminalNode(1), builder.terminalNode(0));
        builder.add(wire);
        placed.add(wire);
        // Allow 125% power.
        thermals.builder()
                .setMaxPower(Vf * If * 1.25f, 125f)
                .setThermalMass(0.00025f)
                .addHeatSource(wire);
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        var buffer = CachedBuffers.partial(ModdedPartialModels.LED_BULB, be.getBlockState());
        var color = new Color(placed.get(RED), placed.get(GREEN), placed.get(BLUE));
        if(!placed.wires.isEmpty()) {
            var wire = placed.wires.get(0);
            var I = wire.current();
            if(wire.getNetwork() == null)
                I = 0;
            var intensity = Mth.clamp(I / placed.get(FORWARD_CURRENT), 0, 1.1f);
            if(intensity > 1) {
                color.mixWith(Color.WHITE, intensity - 1);
            } else {
                color.mixWith(Color.BLACK, 1 - (intensity * 0.75f + 0.25f));
            }
            var blockLight = Math.max(LightTexture.block(light), (int) (15 * intensity));
            var skyLight = LightTexture.sky(light);
            light = LightTexture.pack(blockLight, skyLight);
        }

        color.setAlpha(255);
        buffer
                .color(color)
                .light(light)
                .renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
    }
}
