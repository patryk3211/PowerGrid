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
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Color;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.sim.special.DiodeWire;

public class LEDComponent extends OrientableComponent implements IRenderedComponent {
    public static final IntProperty RED = new IntProperty(PowerGrid.MOD_ID, "led_red", 255, 0, 255);
    public static final IntProperty GREEN = new IntProperty(PowerGrid.MOD_ID, "led_green", 0, 0, 255);
    public static final IntProperty BLUE = new IntProperty(PowerGrid.MOD_ID, "led_blue", 0, 0, 255);

    public LEDComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(RED, GREEN, BLUE);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new DiodeWire(1e-3f, 2.7f, builder.terminalNode(1), builder.terminalNode(0));
        builder.add(wire);
        placed.add(wire);
        // 7.5mA is max current, 5 mA is target for max intensity
        thermals.builder()
                .setMaxPower(0.025f, 125f)
                .setThermalMass(0.00025f)
                .addHeatSource(wire);
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, MatrixStack ms, VertexConsumerProvider bufferSource, int light, int overlay) {
        var buffer = CachedBufferer.partial(ModdedPartialModels.LED_BULB, be.getCachedState());
        var color = new Color(placed.get(RED), placed.get(GREEN), placed.get(BLUE));
        if(!placed.wires.isEmpty()) {
            var wire = placed.wires.get(0);
            var I = wire.current();
            // 5mA is the target current.
            var intensity = MathHelper.clamp(I / 0.005f, 0, 1.1f);
            if(intensity > 1) {
                color.mixWith(Color.WHITE, intensity - 1);
            } else {
                color.mixWith(Color.BLACK, 1 - (intensity * 0.75f + 0.25f));
            }
            var blockLight = Math.max(LightmapTextureManager.getBlockLightCoordinates(light), (int) (15 * intensity));
            var skyLight = LightmapTextureManager.getSkyLightCoordinates(light);
            light = LightmapTextureManager.pack(blockLight, skyLight);
        }

        color.setAlpha(255);
        buffer
                .color(color)
                .light(light)
                .renderInto(ms, bufferSource.getBuffer(RenderLayer.getSolid()));
    }
}
