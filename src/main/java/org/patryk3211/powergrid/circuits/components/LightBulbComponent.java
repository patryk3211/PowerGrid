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
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, 
                       PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
                    
        DyeColor dyeColor = placed.get(COLOR);
    
        // Select correct models - WHITE uses undyed models for proper appearance
        var glowModel = ModdedPartialModels.LIGHT_BULB_GLOW_DYED;
        var bulbModel = ModdedPartialModels.LIGHT_BULB_BULB_DYED;
    
        if (dyeColor == DyeColor.WHITE) {
            glowModel = ModdedPartialModels.LIGHT_BULB_GLOW;
            bulbModel = ModdedPartialModels.LIGHT_BULB_BULB;
        }
    
        // === Get proper RGB color for the bulb ===
        float red, green, blue;
    
        if (dyeColor == DyeColor.WHITE) {
            // Pure white for the white bulb
            red = green = blue = 1.0f;
        } else {
            // Using getFireworkColor() gives much better and more intuitive colors
            // than getTextureDiffuseColor() which was causing wrong hues (orange→blue, etc.)
            int fireworkColor = dyeColor.getFireworkColor();
    
            red   = ((fireworkColor >> 16) & 0xFF) / 255.0f;
            green = ((fireworkColor >> 8)  & 0xFF) / 255.0f;
            blue  = (fireworkColor & 0xFF) / 255.0f;
    
            // Slightly boost saturation and brightness for better visual appeal
            red   = Math.min(1.0f, red * 1.25f);
            green = Math.min(1.0f, green * 1.25f);
            blue  = Math.min(1.0f, blue * 1.25f);
        }
    
        // Render the bulb glass / housing
        var bulb = CachedBuffers.partial(bulbModel, be.getBlockState());
        bulb.color((int)(red * 255), (int)(green * 255), (int)(blue * 255), 255)
            .light(light)
            .renderInto(ms, bufferSource.getBuffer(RenderType.cutoutMipped()));
    
        // === Render glowing effect when powered ===
        if (placed.customData instanceof FloatPair temps) {
            float brightness = temps.lerped(partialTicks);   // Value between 0.0 and 1.0
    
            if (brightness > 0.01f) {
                // Calculate final color with brightness applied
                int r = (int) (red * brightness * 255);
                int g = (int) (green * brightness * 255);
                int b = (int) (blue * brightness * 255);
                int alpha = (int) (brightness * 255);
    
                var center = 1.5f / 16f;
                var orientation = placed.get(ORIENTATION);
    
                var glow = CachedBuffers.partial(glowModel, be.getBlockState());
                glow.disableDiffuse()
                    .color(r, g, b, alpha)
                    .light(LightTexture.FULL_BRIGHT)
                    .translate(center, center, center)
                    .rotateYDegrees(orientation.ordinal() * 90)
                    .translateBack(center, center, center)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
            }
        }
    }
}
