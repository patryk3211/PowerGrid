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
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.EnumProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.sim.special.NeonBulbWire;
import org.patryk3211.powergrid.utility.Unit;

public class NeonBulbComponent extends OrientableComponent implements IRenderedComponent, IGoggleLabel {
    public static final FloatProperty BREAKDOWN_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "neon_tube_vb", 60, 30, 300);
    public static final CalculatedProperty<Float> HOLDING_VOLTAGE = new CalculatedProperty<>(PowerGrid.MOD_ID, "neon_tube_vh",
            placed -> 0.75f * placed.get(BREAKDOWN_VOLTAGE),
            v -> Unit.VOLTAGE.formatWithPrefixes(v).string());
    public static final CalculatedProperty<Float> HOLDING_CURRENT = new CalculatedProperty<>(PowerGrid.MOD_ID, "neon_tube_ih",
            placed -> 0.2f / placed.get(BREAKDOWN_VOLTAGE),
            v -> Unit.CURRENT.formatWithPrefixes(v).string());
    public static final EnumProperty<DyeColor> COLOR = new EnumProperty<DyeColor>(PowerGrid.MOD_ID, "color", DyeColor.class, new DyeColor[]{DyeColor.RED, DyeColor.YELLOW, DyeColor.BLUE, DyeColor.GREEN, DyeColor.WHITE});
    public static final BooleanProperty LIT = (BooleanProperty) new BooleanProperty(PowerGrid.MOD_ID, "lit").hidden();

    public NeonBulbComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(BREAKDOWN_VOLTAGE, HOLDING_VOLTAGE, HOLDING_CURRENT, COLOR, LABEL, LIT, power(1.5f));
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var ih = placed.get(HOLDING_CURRENT);
        var vb = placed.get(BREAKDOWN_VOLTAGE);
        var vh = placed.get(HOLDING_VOLTAGE);

        var wire = new NeonBulbWire(
                vb, vh, ih, 0.005f,
                builder.terminalNode(0), builder.terminalNode(1)
        );
        wire.setLit(placed.get(LIT));
        builder.add(wire);
        placed.add(wire);

        thermals.builder()
                .addHeatSource(wire)
                .setThermalMass(0.01f)
                .setMaxPower(1.5f, 125f);
    }

    private static class LerpPair {
        public final LerpedFloat first;
        public final LerpedFloat second;

        public LerpPair() {
            first = LerpedFloat.linear()
                    .chase(0, 1 / 10f, LerpedFloat.Chaser.LINEAR);
            second = LerpedFloat.linear()
                    .chase(0, 1 / 10f, LerpedFloat.Chaser.LINEAR);
        }

        public void tickChaser() {
            first.tickChaser();
            second.tickChaser();
        }
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return true;
        var wire = (NeonBulbWire) placed.wires.get(0);
        placed.onClientWorld(() -> world -> {
            LerpPair state;
            if(placed.customData instanceof LerpPair current) {
                state = current;
            } else {
                state = new LerpPair();
                placed.customData = state;
            }
            state.tickChaser();
            if(wire.isLit()) {
                if (wire.current() > 0) {
                    state.first.updateChaseTarget(1);
                    state.second.updateChaseTarget(0);
                } else {
                    state.first.updateChaseTarget(0);
                    state.second.updateChaseTarget(1);
                }
            } else {
                state.first.updateChaseTarget(0);
                state.second.updateChaseTarget(0);
            }
        });
        placed.onServerWorld(() -> $ -> {
            if(wire.isLit() != placed.get(LIT)) {
                placed.set(LIT, wire.isLit());
                placed.notifyClients(LIT);
            }
        });
        return true;
    }

    @Override
    public void stateUpdated(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return;
        var wire = (NeonBulbWire) placed.wires.get(0);
        wire.setLit(placed.get(LIT));
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        // Render the bulb here to avoid adding all circuit board quads to translucent layer.
        var bulb = CachedBuffers.partial(ModdedPartialModels.NEON_TUBE_BULB, be.getBlockState());
        bulb.light(light).renderInto(ms, bufferSource.getBuffer(RenderType.translucent()));

		var color = placed.get(COLOR).getTextureDiffuseColors();
		var red = color[0];
		var green = color[1];
		var blue = color[2];

        int a1 = 0, r1 = 0, g1 = 0, b1 = 0, a2 = 0, r2 = 0, g2 = 0, b2 = 0;
        if(placed.customData instanceof LerpPair pair) { 
            a1 = (int) (pair.first.getValue(partialTicks) * 128);
            r1 = (int) (red * pair.first.getValue(partialTicks) * 128);
            g1 = (int) (green * pair.first.getValue(partialTicks) * 128);
            b1 = (int) (blue * pair.first.getValue(partialTicks) * 128);

            a2 = (int) (pair.second.getValue(partialTicks) * 128);
            r2 = (int) (red * pair.second.getValue(partialTicks) * 128);
            g2 = (int) (green * pair.second.getValue(partialTicks) * 128);
            b2 = (int) (blue * pair.second.getValue(partialTicks) * 128);
        }

        var center = 1 / 16f;
        var orientation = placed.get(ORIENTATION);

	if(a1 != 0) {
            var buffer = CachedBuffers.partial(ModdedPartialModels.NEON_TUBE_GLOW, be.getBlockState());
            buffer
                    .disableDiffuse()
                    .color(r1, g1, b1, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .translate(center, center, center)
                    .rotateYDegrees(orientation.ordinal() * 90)
                    .translateBack(center, center, center)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
        }
        if(a2 != 0) {
            var buffer = CachedBuffers.partial(ModdedPartialModels.NEON_TUBE_GLOW, be.getBlockState());
            buffer
                    .disableDiffuse()
                    .color(r2, g2, b2, 255)
                    .light(LightTexture.FULL_BRIGHT)
                    .translate(center, center, center)
                    .rotateYDegrees(180 + orientation.ordinal() * 90)
                    .translateBack(center, center, center)
                    .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
        }
    }
}
