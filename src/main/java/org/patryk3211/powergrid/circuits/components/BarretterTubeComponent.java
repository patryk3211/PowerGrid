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
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.BarretterWire;

public class BarretterTubeComponent extends OrientableComponent implements IRenderedComponent {
    public static final FloatProperty MINIMUM_RESISTANCE = new FloatProperty(PowerGrid.MOD_ID, "barretter_resistance", 10, 10, 1000);
    public static final FloatProperty HOLDING_CURRENT = new FloatProperty(PowerGrid.MOD_ID, "barretter_current", 0.1f, 0.01f, 2);

    public BarretterTubeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(HOLDING_CURRENT, MINIMUM_RESISTANCE, power(50));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
        final var Ih = placed.get(HOLDING_CURRENT);
        final var Rmin = placed.get(MINIMUM_RESISTANCE);
        var tube = new BarretterWire(Ih, Rmin, builder.terminalNode(0), builder.terminalNode(1));
        builder.add(tube);

        placed.add(tube);

        var data = new FloatPair();
        placed.customData = data;

        final var operatingTemperature = 1400f;
        final var dissipationFactor = ThermalBehaviour.dissipationFactor(50, operatingTemperature);
        thermals.builder()
                .addHeatSource(tube)
                .setThermalMass(0.05f)
                .setOverheatTemperature(1850f)
                .setDissipationFactor(dissipationFactor)
                .withTemperatureCallback(T ->
                    data.current = Mth.clamp((T - 600f) / (1400f - 600f), 0, 1));
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
        int a = 0;
        if(placed.customData instanceof FloatPair data) {
            a = (int) (data.lerped(partialTicks) * 64);
        }
        if(a == 0)
            return;
        var buffer = CachedBuffers.partial(ModdedPartialModels.BARRETTER_GLOW, be.getBlockState());
        buffer
                .disableDiffuse()
                .color(a, a, a, 255)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
    }
}
