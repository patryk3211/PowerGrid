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
import com.simibubi.create.Create;
import net.createmod.catnip.render.CachedBuffers;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.properties.ComponentProperty;
import org.patryk3211.powergrid.circuits.components.properties.IntProperty;
import org.patryk3211.powergrid.circuits.components.properties.StringProperty;
import org.patryk3211.powergrid.circuits.schematic.ComponentFootprint;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.collections.ModdedPartialModels;
import org.patryk3211.powergrid.electricity.info.customdisplay.Expression;
import org.patryk3211.powergrid.utility.Lang;

import java.util.List;

public abstract class GaugeComponent extends OrientableComponent implements IRedstoneComponent, IRenderedComponent, IComponentGoggleInformation {
    public static final IntProperty LEVEL = new IntProperty(PowerGrid.MOD_ID, "redstone_level", 0, 0, 15).hidden().cast();
    public static final StringProperty EQUATION = new StringProperty(PowerGrid.MOD_ID, "gauge_equation", "x");

    public GaugeComponent(ComponentFootprint footprint) {
        super(footprint);
    }

    @Override
    protected void addProperties(ImmutableCollection.Builder<ComponentProperty<?>> properties) {
        super.addProperties(properties);
        properties.add(LEVEL, LABEL, EQUATION);
    }

    @Override
    public boolean isEmitter() {
        return true;
    }

    @Override
    public int getEmittedLevel(@NotNull PlacedComponent component) {
        return component.get(LEVEL);
    }

    public abstract float getTarget(PlacedComponent placed);
    public abstract float getValue(PlacedComponent placed);

    public abstract float getMaxValue(PlacedComponent placed);
    public abstract String getUnit(PlacedComponent placed);
    public abstract ChatFormatting getColor(float value, float maxValue);

    @Override
    public boolean tick(@NotNull PlacedComponent placed) {
        var target = getTarget(placed);
        if(getEdge(placed) != null) {
            int redstoneLevel = Mth.clamp((int) (target * 15), 0, 15);
            if (redstoneLevel != placed.get(LEVEL)) {
                placed.set(LEVEL, redstoneLevel);
                IRedstoneComponent.notifyNeighbours(placed);
            }
        }

        placed.onClientWorld(() -> level -> {
            RenderData data;
            if(placed.customData instanceof RenderData current) {
                data = current;
            } else {
                data = new RenderData();
                data.expression = Expression.tryParse(placed.get(EQUATION)).orElse(null);
                placed.customData = data;
            }
            data.prevState = data.state;
            data.state += (target - data.state) * .125f;
            if(data.state > 1 && level.random.nextFloat() < 1 / 2f)
                data.state -= (data.state - 1) * level.random.nextFloat();
        });

        return true;
    }

    @Environment(EnvType.CLIENT)
    public static class RenderData {
        private float prevState;
        private float state;
        private Expression expression;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void render(CircuitBoardBlockEntity be, PlacedComponent placed, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
        if(placed.customData instanceof RenderData data) {
            var value = Mth.lerp(partialTicks, data.prevState, data.state);

            var buffer = CachedBuffers.partial(ModdedPartialModels.COMPONENT_GAUGE_NEEDLE, be.getBlockState());
            var angle = -90 * value;
            buffer
                    .translate(2.5 / 16, 0, 2.5 / 16)
                    .rotateYDegrees(placed.get(ORIENTATION).ordinal() * 90)
                    .translate(-1 / 16f, 0, 1 / 16f)
                    .rotateYDegrees(angle)
                    .translate(-1.5 / 16, 0, -3.5 / 16)
                    .light(light)
                    .renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
        }
    }

    @Override
    public boolean addToGoggleTooltip(PlacedComponent component, List<Component> tooltip, boolean isPlayerSneaking) {
        if(component.has(LABEL)) {
            var label = component.get(LABEL);
            if(label.isEmpty()) {
                Lang.builder(Create.ID).translate("gui.gauge.info_header").forGoggles(tooltip);
            } else {
                Lang.text(label).forGoggles(tooltip);
            }
        } else {
            Lang.builder(Create.ID).translate("gui.gauge.info_header").forGoggles(tooltip);
        }
        if(component.customData instanceof RenderData data && data.expression != null) {
            var expr = data.expression;
            float value = getValue(component);
            float maxValue = getMaxValue(component);
            StringBuilder line = new StringBuilder();
            if(Math.abs(value) > maxValue) {
                line.append(value >= 0 ? "> " : "< ");
                if(value < 0)
                    value = -maxValue;
                else
                    value = maxValue;
            }
            var evaluatedValue = expr.eval(value);
            if(evaluatedValue >= 0)
                line.append(" ");
            String prefix = "";
            line.append(String.format("%.2f %s", evaluatedValue, prefix));
            var textComponent = Lang.text(line.toString())
                    .style(getColor(Math.abs(value), maxValue));
            textComponent.add(Component.literal(getUnit(component)));
            textComponent.forGoggles(tooltip);
        } else {
            Lang.translate("gui.invalid_equation").style(ChatFormatting.RED).forGoggles(tooltip);
        }
        return true;
    }
}
