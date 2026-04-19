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
import com.simibubi.create.foundation.render.RenderTypes;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.sim.ElectricWire;

public class LightBulbComponent extends OrientableComponent implements IRenderedComponent, IGoggleLabel {
    
    public static final EnumProperty<DyeColor> COLOR = new EnumProperty<DyeColor>(PowerGrid.MOD_ID, "color", DyeColor.class);

    public LightBulbComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LABEL, COLOR, voltage(12), power(3f));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new ElectricWire(20f, builder.terminalNode(0), builder.terminalNode(1));
        builder.add(wire);
        placed.add(wire);

        final float R_max = 12 * 12 / 3.0f;
        var data = new FloatPair();
        placed.customData = data;
        thermals.builder()
                .addHeatSource(wire)
                .setThermalMass(0.001f)
                .setMaxPower(3.0f, 1450f)
                .setOverheatTemperature(1850f)
                .withTemperatureCallback(T -> {
                    wire.setResistance(20f + (R_max - 20f) / 1450f * T);
                    var x = Mth.clamp((T - 600f) / (1400f - 600f), 0, 1);
                    data.current = x * x;
                });
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(!placed.isClient())
            return false;
        renderDataTick(placed);
        return true;
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        // Render the bulb here to avoid adding all circuit board quads to cutout layer.
        var bulb = CachedBuffers.partial(ModdedPartialModels.LIGHT_BULB_BULB, be.getBlockState());
        bulb.light(light).renderInto(ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
        
        var color = placed.get(COLOR).getTextureDiffuseColor();

        var red = (color >> 16) & 0xFF;
        var green = (color >> 8) & 0xFF;
        var blue = color & 0xFF;

        int a = 0, r = 0, g = 0, b = 0;
        if(placed.customData instanceof FloatPair temps) {
            a = (int) (temps.lerped(partialTicks) * 128);
            r = (int) (red * temps.lerped(partialTicks) * 128 / 256);
            g = (int) (green * temps.lerped(partialTicks) * 128 / 256);
            b = (int) (blue * temps.lerped(partialTicks) * 128 / 256);
        }
        var center = 1.5f / 16f;
        var orientation = placed.get(ORIENTATION);
        if(a != 0) {
            var buffer = CachedBuffers.partial(ModdedPartialModels.LIGHT_BULB_GLOW, be.getBlockState());
            buffer
                    .disableDiffuse()
                    .color(r, g, b, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .translate(center, center, center)
                    .rotateYDegrees(orientation.ordinal() * 90)
                    .translateBack(center, center, center)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
        }
    }
}
