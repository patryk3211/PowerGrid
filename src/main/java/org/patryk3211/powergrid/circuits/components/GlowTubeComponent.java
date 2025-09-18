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
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.ComponentCircuitBuilder;
import org.patryk3211.powergrid.circuits.components.properties.BooleanProperty;
import org.patryk3211.powergrid.circuits.components.properties.CalculatedProperty;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.FloatProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.thermal.ThermalBuilder;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.sim.special.ColdCathodeTubeWire;
import org.patryk3211.powergrid.utility.Unit;

public class GlowTubeComponent extends OrientableComponent implements IRenderedComponent, IGlow {
    public static final FloatProperty HOLDING_VOLTAGE = new FloatProperty(PowerGrid.MOD_ID, "glow_tube_vh", 60, 30, 500);
    public static final CalculatedProperty<Float> BREAKDOWN_VOLTAGE = new CalculatedProperty<>(PowerGrid.MOD_ID, "glow_tube_vb",
            placed -> placed.get(HOLDING_VOLTAGE) * 1.4f,
            v -> Unit.VOLTAGE.format(v).getString());
    public static final CalculatedProperty<Float> HOLDING_CURRENT = new CalculatedProperty<>(PowerGrid.MOD_ID, "glow_tube_ih",
            placed -> 0.2f / placed.get(HOLDING_VOLTAGE),
            v -> Unit.CURRENT.formatWithPrefixes(v).string());
    public static final BooleanProperty LIT = (BooleanProperty) new BooleanProperty(PowerGrid.MOD_ID, "glow_tube_lit").hidden();

    public GlowTubeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(HOLDING_VOLTAGE, BREAKDOWN_VOLTAGE, HOLDING_CURRENT, LIT);
    }

    @Override
    public void bake(@NotNull PlacedComponent placed, @NotNull ComponentCircuitBuilder builder, ThermalBuilder.@NotNull IEmitter thermals) {
        var wire = new ColdCathodeTubeWire(
                placed.get(BREAKDOWN_VOLTAGE), placed.get(HOLDING_VOLTAGE), placed.get(HOLDING_CURRENT),
                0.0025f, builder.terminalNode(0), builder.terminalNode(1)
        );
        builder.add(wire);
        placed.add(wire);
    }

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        if(placed.wires.isEmpty())
            return true;
        var wire = (ColdCathodeTubeWire) placed.wires.get(0);
        placed.onClientWorld(() -> world -> {
            LerpedFloat state;
            if(placed.customData instanceof LerpedFloat current) {
                state = current;
            } else {
                state = LerpedFloat.linear();
                placed.customData = state;
            }
            var prevValue = state.getValue();
            state.tickChaser();
            if(wire.lit != placed.get(LIT)) {
                state.chase(wire.lit ? 1 : 0, 1 / 10f, LerpedFloat.Chaser.LINEAR);
            }
            if(prevValue > 0.5f != state.getValue() > 0.5f)
                modelChanged(placed.getPos());
        });
        if(wire.lit != placed.get(LIT)) {
            placed.set(LIT, wire.lit);
        }
        return true;
    }

    @Override
    public boolean shouldGlow(PlacedComponent placed) {
        if(placed.customData instanceof LerpedFloat current) {
            return current.getValue() > 0.5f;
        }
        return false;
    }

    @Override
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        int a = 0;
        if(placed.customData instanceof LerpedFloat lerped) {
            a = (int) (lerped.getValue(partialTicks) * 128);
        }
        if(a == 0)
            return;
        var buffer = CachedBuffers.partial(ModdedPartialModels.GLOW_TUBE_GLOW, be.getBlockState());
        buffer
                .disableDiffuse()
                .color(a, a, a, 255)
                .light(LightTexture.FULL_BRIGHT)
                .renderInto(ms, bufferSource.getBuffer(RenderTypes.additive()));
    }
}
