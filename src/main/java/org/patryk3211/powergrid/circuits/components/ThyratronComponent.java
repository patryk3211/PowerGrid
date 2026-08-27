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
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.render.CachedBuffers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundSource;
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
import org.patryk3211.powergrid.collections.ModdedSoundEvents;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour;
import org.patryk3211.powergrid.electricity.sim.special.ThyratronWire;
import org.patryk3211.powergrid.utility.Unit;
import org.patryk3211.powergrid.utility.sound.ContinuousSound;

import java.util.List;

import static org.patryk3211.powergrid.circuits.components.NeonBulbComponent.LIT;

public class ThyratronComponent extends MirrorableComponent implements IRenderedComponent {
    public static final FloatProperty CONTROL_RATIO = new FloatProperty(PowerGrid.MOD_ID, "thyratron_mu", 20, 5, 1000);
    public static final FloatProperty BREAKDOWN_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "thyratron_vb", 60, 20, 2000);
    public static final CalculatedProperty<Float> HOLDING_VOLTAGE = new CalculatedProperty<>(PowerGrid.MOD_ID, "thyratron_vh",
            placed -> Mth.clamp(0.2f * placed.get(BREAKDOWN_VOLTAGE), 8f, 16f),
            v -> Unit.VOLTAGE.formatWithPrefixes(v).string());
    public static final CalculatedProperty<Float> HOLDING_CURRENT = new CalculatedProperty<>(PowerGrid.MOD_ID, "thyratron_ih",
            placed -> 0.5f / placed.get(BREAKDOWN_VOLTAGE),
            v -> Unit.CURRENT.formatWithPrefixes(v).string());
    public static final FloatProperty HEATER_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "tube_heater_voltage", 6f, 1f, 16f);
    public static final CalculatedProperty<Float> HEATER_POWER = new CalculatedProperty<>(PowerGrid.MOD_ID, "tube_heater_power",
            state -> 300f,
            value -> String.format("%.1f W", value));

    private static final float DISCHARGE_CONDUCTANCE = 50f;
    private static final float ANODE_POWER = 15_000f;
    private static final float GRID_LEAK_RESISTANCE = 1_000_000f;
    private static final int ANODE_NODE = 2;
    private static final float GLOW_CENTER_X = 1.5f / 16f;
    private static final float GLOW_CENTER_Y = 6.5f / 16f;
    private static final float GLOW_CENTER_Z = 1.5f / 16f;
    private static final float STEADY_PLASMA = 0.82f;
    private static final float HUM_VOLUME = 0.1f;
    private static final int HUM_DELAY_TICKS = 5;
    private static final int HUM_FADE_TICKS = 5;
    private static final List<TerminalBoundingBox> ANODE_TERMINAL = List.of(
            new TerminalBoundingBox(
                    net.minecraft.network.chat.Component.translatable("component.powergrid.thyratron.2"),
                    0.5f, 10.5f, 0.5f, 2.5f, 12.5f, 2.5f
            ).withColor(IDecoratedTerminal.RED)
    );

    public ThyratronComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(CONTROL_RATIO, BREAKDOWN_VOLTAGE, HOLDING_VOLTAGE, HOLDING_CURRENT, HEATER_VOLTAGE, HEATER_POWER, LIT, power(ANODE_POWER));
    }

    @Override
    public boolean isExternalNode(int nodeIndex) {
        return nodeIndex == ANODE_NODE;
    }

    @Override
    public List<TerminalBoundingBox> terminals(@NotNull PlacedComponent placed) {
        return ANODE_TERMINAL;
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var cathode = builder.terminalNode(0);
        var grid = builder.terminalNode(1);
        var anode = builder.terminalNode(2);

        var tube = new ThyratronWire(
                placed.get(CONTROL_RATIO),
                placed.get(BREAKDOWN_VOLTAGE),
                placed.get(HOLDING_VOLTAGE),
                placed.get(HOLDING_CURRENT),
                DISCHARGE_CONDUCTANCE,
                cathode, anode, grid
        );
        tube.setLit(placed.get(LIT));
        tube.setEmission(0);
        builder.add(tube);
        builder.connect(GRID_LEAK_RESISTANCE, grid, cathode);

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
                    tube.setEmission(Mth.clamp((temperature - 1300f) / 100f, 0, 1));
                    data.heaterPrev = data.heater;
                    data.heater = Mth.clamp((temperature - 1000) / 400f, 0, 1.125f);
                });

        thermals.builder()
                .addHeatSource(tube)
                .setThermalMass(0.05f * ANODE_POWER / 25f)
                .setMaxPower(ANODE_POWER, 125f);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if (placed.wires.isEmpty())
            return true;
        var wire = (ThyratronWire) placed.wires.get(0);
        placed.onClientWorld(() -> world -> {
            RenderData data;
            if (placed.customData instanceof RenderData current) {
                data = current;
            } else {
                data = new RenderData();
                placed.customData = data;
            }
            data.plasma.tickChaser();
            data.strike.tickChaser();
            boolean lit = placed.get(LIT);
            if (lit && !data.wasLit) {
                data.plasma.setValue(1);
                data.strike.setValue(1);
            }
            data.wasLit = lit;
            data.plasma.updateChaseTarget(lit ? STEADY_PLASMA : 0);
            data.strike.updateChaseTarget(0);
            tickConductionSound(placed, data, lit);
        });
        placed.onServerWorld(() -> world -> {
            if (wire.isLit() != placed.get(LIT)) {
                if (wire.isLit()) {
                    ModdedSoundEvents.THYRATRON_FIRE.playOnServer(world, placed.getPos(), 0.8f, 0.95f + world.random.nextFloat() * 0.2f);
                }
                placed.set(LIT, wire.isLit());
                placed.notifyClients(LIT);
            }
        });
        return true;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        if (placed.wires.isEmpty())
            return;
        var wire = (ThyratronWire) placed.wires.get(0);
        wire.setLit(placed.get(LIT));
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        float heater = 0;
        float plasma = 0;
        float strike = 0;
        if (placed.customData instanceof RenderData data) {
            heater = Mth.lerp(partialTicks, data.heaterPrev, data.heater);
            plasma = data.plasma.getValue(partialTicks);
            strike = data.strike.getValue(partialTicks);
        }

        int heaterAlpha = (int) (heater * 48);
        if (heaterAlpha > 0) {
            var buffer = CachedBuffers.partial(ModdedPartialModels.THYRATRON_GLOW, be.getBlockState());
            buffer
                    .disableDiffuse()
                    .color(heaterAlpha, heaterAlpha, heaterAlpha, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
        }

        int plasmaAlpha = (int) (plasma * 200);
        if (plasmaAlpha > 0) {
            CachedBuffers.partial(ModdedPartialModels.THYRATRON_GLOW, be.getBlockState())
                    .disableDiffuse()
                    .color(plasmaAlpha * 2 / 3, plasmaAlpha, plasmaAlpha * 5 / 6, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
        }

        int flash = (int) (strike * 255);
        if (flash > 0) {
            float s = 1f + strike * 0.07f;
            ms.pushPose();
            ms.translate(GLOW_CENTER_X, GLOW_CENTER_Y, GLOW_CENTER_Z);
            ms.scale(s, s, s);
            ms.translate(-GLOW_CENTER_X, -GLOW_CENTER_Y, -GLOW_CENTER_Z);
            CachedBuffers.partial(ModdedPartialModels.THYRATRON_GLOW, be.getBlockState())
                    .disableDiffuse()
                    .color(flash, flash, flash, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
            CachedBuffers.partial(ModdedPartialModels.THYRATRON_GLOW, be.getBlockState())
                    .disableDiffuse()
                    .color(flash, flash, flash, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
            ms.popPose();
        }
    }

    @Environment(EnvType.CLIENT)
    private void tickConductionSound(@NotNull PlacedComponent placed, RenderData data, boolean lit) {
        var sounds = Minecraft.getInstance().getSoundManager();
        if (lit)
            data.humTicks = Math.min(data.humTicks + 1, HUM_DELAY_TICKS);
        else
            data.humTicks = 0;
        boolean hum = data.humTicks >= HUM_DELAY_TICKS;
        if (hum) {
            if (data.buzz == null || data.buzz.isStopped()) {
                var pos = placed.getPos().getCenter();
                data.buzz = new ContinuousSound(
                        ModdedSoundEvents.THYRATRON_HUM.getMainEvent(), SoundSource.BLOCKS,
                        pos.x, pos.y, pos.z, HUM_VOLUME, 1.0f, HUM_FADE_TICKS,
                        () -> !placed.destroyed && placed.get(LIT) && data.humTicks >= HUM_DELAY_TICKS);
                sounds.play(data.buzz);
            }
        } else if (data.buzz != null && data.buzz.isStopped()) {
            data.buzz = null;
        }
    }

    private static class RenderData {
        float heaterPrev;
        float heater;
        boolean wasLit;
        int humTicks;
        ContinuousSound buzz;
        final LerpedFloat plasma = LerpedFloat.linear()
                .chase(0, 0.45f, LerpedFloat.Chaser.EXP);
        final LerpedFloat strike = LerpedFloat.linear()
                .chase(0, 0.85f, LerpedFloat.Chaser.EXP);
    }
}
