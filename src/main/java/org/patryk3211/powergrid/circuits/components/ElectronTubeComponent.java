/*
 * Copyright 2026 patryk3211
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.ElectronTubeWire;

public class ElectronTubeComponent extends MirrorableComponent implements IRenderedComponent {
    // Koren mu (amplification factor), not small-signal voltage gain
    public static final FloatProperty GAIN = new FloatProperty(PowerGrid.MOD_ID, "tube_gain", 5, 1, 100);
    // Doubled from 6800 to preserve operating point with the Koren (1 + sgn(E1)) factor
    public static final FloatProperty K_G = new FloatProperty(PowerGrid.MOD_ID, "tube_kg", 13_600, 200, 20_000);
    public static final FloatProperty K_P = new FloatProperty(PowerGrid.MOD_ID, "tube_kp", 600, 50, 2000);
    public static final FloatProperty K_VB = new FloatProperty(PowerGrid.MOD_ID, "tube_kvb", 300, 10, 1000);
    public static final FloatProperty EX = new FloatProperty(PowerGrid.MOD_ID, "tube_ex", 1.5f, 1.2f, 1.6f);
    public static final FloatProperty SATURATION_CURRENT = new FloatProperty(PowerGrid.MOD_ID, "tube_saturation_current", 0.1f, 0.001f, 20);
    public static final FloatProperty HEATER_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "tube_heater_voltage", 6f, 1f, 16f);
    public static final CalculatedProperty<Float> HEATER_POWER = new CalculatedProperty<>(PowerGrid.MOD_ID, "tube_heater_power", state -> {
        var Is = state.get(SATURATION_CURRENT);
        return Math.max(5f, Is * 50f);
        }, value -> String.format("%.1f W", value));

    public ElectronTubeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(GAIN, K_G, K_P, K_VB, EX, SATURATION_CURRENT, HEATER_VOLTAGE, HEATER_POWER);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, @NotNull ThermalBuilder.IEmitter thermals) {
        final var saturationCurrent = placed.get(SATURATION_CURRENT);
        var tube = new ElectronTubeWire(
                placed.get(GAIN), placed.get(K_G), placed.get(K_P), placed.get(K_VB), placed.get(EX), saturationCurrent,
                builder.terminalNode(0), // Cathode
                builder.terminalNode(2), // Anode
                builder.terminalNode(1)  // Grid
        );
        builder.add(tube);

        var targetPower = placed.get(HEATER_POWER);
        var heaterCurrent = targetPower / placed.get(HEATER_VOLTAGE);
        var heaterResistance = placed.get(HEATER_VOLTAGE) / heaterCurrent;
        var heater = builder.connect(heaterResistance, builder.terminalNode(3), builder.terminalNode(4));

        placed.add(tube);
        placed.add(heater);

        var data = new RenderData();
        placed.customData = data;

        final var operatingTemperature = 1400f;
        final var dissipationFactor = ThermalBehaviour.dissipationFactor(targetPower, operatingTemperature);
        thermals.builder()
                .addHeatSource(heater)
                .setThermalMass(0.001f * targetPower / 5f)
                .setOverheatTemperature(1600f)
                .setDissipationFactor(dissipationFactor)
                .withTemperatureCallback(temperature -> {
                    tube.setSaturationCurrent(
                            Mth.clamp(temperature - 1300f, 0, 150) * saturationCurrent / 100
                    );
                    data.prev = data.current;
                    data.current = Mth.clamp((temperature - 1000) / 400f, 0, 1.125f);
                });
    }

    @Override
    public void dataFixup(@NotNull CompoundTag tag) {
        var props = tag.getCompound("Properties");
        if(props.contains("powergrid:tube_anode_resistance")) {
            var resistance = props.getFloat("powergrid:tube_anode_resistance");
            var kg = ElectronTubeWire.calculateKg1(
                    1, 0,
                    props.getFloat(GAIN.id().toString()),
                    props.contains(K_P.id().toString()) ? props.getFloat(K_P.id().toString()) : K_P.defaultValue(),
                    props.contains(K_VB.id().toString()) ? props.getFloat(K_VB.id().toString()) : K_VB.defaultValue(),
                    props.contains(EX.id().toString()) ? props.getFloat(EX.id().toString()) : EX.defaultValue(),
                    1 / resistance);
            PowerGrid.LOGGER.info("Fixing electron tube anode resistance ({}) into k_g ({})", resistance, kg);
            props.putFloat(K_G.id().toString(), kg);
            props.remove("powergrid:tube_anode_resistance");
        }
    }

    public static class RenderData {
        float prev;
        float current;
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        int a = 0;
        if(placed.customData instanceof RenderData data) {
            a = (int) (Mth.lerp(partialTicks, data.prev, data.current) * 64);
        }
        if(a == 0)
            return;
        var buffer = CachedBuffers.partial(ModdedPartialModels.ELECTRON_TUBE_GLOW, be.getBlockState());
        buffer
                .disableDiffuse()
                .color(a, a, a, 255)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
    }
}
