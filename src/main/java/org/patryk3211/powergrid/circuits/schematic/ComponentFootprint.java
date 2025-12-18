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
package org.patryk3211.powergrid.circuits.schematic;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.ComponentRegistry;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;

import java.util.*;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.COLOR_COMPONENT_OUTLINE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.COLOR_TERMINAL;

public class ComponentFootprint {
    private static final ResourceLocation ARROWS = PowerGrid.texture("gui/circuit_arrows");

    private static final PadData NONE = new PadData(-1, null, null);

    private final int width;
    private final int height;

    private final SortedMap<Point, PadData> pads;
    private final boolean outline;
    private final boolean withItem;
    @Nullable
    private final Orientation arrow;

    private ItemStack renderedStack;

    protected ComponentFootprint(int width, int height, SortedMap<Point, PadData> pads, boolean outline, boolean withItem, @Nullable Orientation arrow) {
        this.width = width;
        this.height = height;
        this.pads = pads;
        this.outline = outline;
        this.withItem = withItem;
        this.arrow = arrow;
    }

    @Environment(EnvType.CLIENT)
    protected void renderPads(@NotNull GuiGraphics ctx, int x, int y) {
        var ms = ctx.pose();
        ms.pushPose();
        ms.scale(0.25f, 0.25f, 0.25f);
        for (var point : pads.keySet()) {
            int x1 = (point.x() + x) * 4;
            int y1 = (point.y() + y) * 4;
            ctx.fill(x1 + 1, y1 + 1, x1 + 3, y1 + 3, COLOR_TERMINAL);
        }
        ms.popPose();
    }

    @Environment(EnvType.CLIENT)
    public void render(@NotNull GuiGraphics ctx, @NotNull org.patryk3211.powergrid.circuits.components.Component component, int x, int y, boolean hovering) {
        var ms = ctx.pose();
        if(outline) {
            ms.pushPose();
            ms.scale(0.5f, 0.5f, 0.5f);
            ctx.renderOutline(x * 2, y * 2, width * GRID_TO_GRID_SCALE * 2, height * GRID_TO_GRID_SCALE * 2, COLOR_COMPONENT_OUTLINE);
            ms.popPose();
        }
        renderPads(ctx, x, y);

        if(withItem && !hovering) {
            ms.pushPose();
            final int maxSize = 2;
            var scale = Math.min(Math.min(width, height), maxSize) / 16f * GRID_TO_GRID_SCALE;
            if(width > maxSize && height > maxSize) {
                ms.translate(
                        (width - maxSize) * 0.5f,
                        (height - maxSize) * 0.5f,
                        0
                );
            } else if(width > height) {
                float offset = (width - height) * 0.5f;
                ms.translate(offset, 0, 0);
            } else if(height > width) {
                float offset = (height - width) * 0.5f;
                ms.translate(0, offset, 0);
            }
            ms.scale(scale, scale, scale);
            ctx.renderItem(getRenderedStack(component), (int) (x / scale), (int) (y / scale));
            ms.popPose();
        }
        if(arrow != null) {
            ms.pushPose();
            int u = (arrow.ordinal() % 2) * 8;
            int v = (arrow.ordinal() / 2) * 8;
            ms.translate(x + (width * GRID_TO_GRID_SCALE * 0.5f), y + (height * GRID_TO_GRID_SCALE * 0.5f), 0);

            switch(arrow) {
                case RIGHT -> ms.translate(width / 2, 0, 0);
                case LEFT -> ms.translate(-width / 2, 0, 0);
                case DOWN -> ms.translate(0, height / 2, 0);
                case UP -> ms.translate(0, -height / 2, 0);
            }

            ms.scale(0.25f, 0.25f, 1);
            ms.translate(-4, -4, 0);
            ctx.blit(ARROWS, 0, 0, u, v, 8, 8, 16, 16);

            ms.popPose();
        }
    }

    @Environment(EnvType.CLIENT)
    public void renderPadIndices(@NotNull GuiGraphics ctx, @NotNull Font textRenderer, int x, int y) {
        var ms = ctx.pose();
        ms.pushPose();
        int scale = 12;
        ms.translate(0.5f, 0.5f, 100);
        ms.scale(1.0f / scale, 1.0f / scale, 1.0f);
        for(var entry : pads.entrySet()) {
            var point = entry.getKey();
            int x1 = (point.x() + x) * scale;
            int y1 = (point.y() + y) * scale;

            var text = entry.getValue().shortText();
            if(text == null)
                continue;
            int width = textRenderer.width(text);
            ctx.drawString(textRenderer, text, x1 - width / 2, y1 - 4, -1, false);
        }
        ms.popPose();
    }

    @Nullable
    public Component getTooltip(int mouseX, int mouseY) {
        var pad = pads.get(new Point(mouseX, mouseY));
        if(pad == null)
            return null;
        return pad.tooltip;
    }

    @Nullable
    public ItemStack getRenderedStack(org.patryk3211.powergrid.circuits.components.Component component) {
        if(!withItem)
            return null;
        if(renderedStack == null)
            renderedStack = ComponentRegistry.getItem(component).getDefaultInstance();
        return renderedStack;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Map<Point, PadData> getPads() {
        return pads;
    }

    public ComponentFootprint rotated(Orientation orientation) {
        int width, height;
        if(orientation == Orientation.UP || orientation == Orientation.DOWN) {
            width = this.height;
            height = this.width;
        } else {
            width = this.width;
            height = this.height;
        }
        var pads = new TreeMap<Point, PadData>();
        for(var pad : this.pads.entrySet()) {
            var position = pad.getKey();
            int x, y;
            switch(orientation) {
                case RIGHT -> {
                    // No rotation
                    x = position.x();
                    y = position.y();
                }
                case DOWN -> {
                    // 90 degree rotation
                    x = this.height - position.y() - 1;
                    y = position.x();
                }
                case LEFT -> {
                    // 180 degree rotation
                    x = this.width - position.x() - 1;
                    y = this.height - position.y() - 1;
                }
                case UP -> {
                    // 270 degree rotation
                    x = position.y();
                    y = this.width - position.x() - 1;
                }
                default -> throw new IllegalStateException("Invalid orientation: " + orientation);
            }
            pads.put(new Point(x, y), pad.getValue());
        }

        var footprint = new ComponentFootprint(width, height, pads, this.outline, withItem, arrow == null ? null : arrow.rotate(orientation));
        // Copy cached stack if one is available.
        footprint.renderedStack = this.renderedStack;
        return footprint;
    }

    public static class Builder {
        private final int width, height;
        private final SortedMap<Point, PadData> pads = new TreeMap<>();
        private boolean withItem;
        private boolean outline = false;
        private Orientation arrow = null;
        @Nullable
        private final String translationKey;
        public final Map<String, String> translatedPads = new HashMap<>();

        public Builder(int width, int height) {
            this(width, height, null);
        }

        public Builder(int width, int height, String translationKeyBase) {
            this.width = width;
            this.height = height;
            this.translationKey = translationKeyBase;
        }

        private void validatePad(int x, int y) {
            if(x < 0 || y < 0 || x >= width * GRID_TO_GRID_SCALE || y >= height * GRID_TO_GRID_SCALE)
                throw new IllegalArgumentException("Pad position must be inside defined footprint size");
        }

        public Builder addPad(int x, int y) {
            validatePad(x, y);
            pads.put(new Point(x, y), NONE);
            return this;
        }

        public Builder addPad(int x, int y, int nodeIndex) {
            return addPad(x, y, nodeIndex, (Component) null, null);
        }

        public Builder addPad(int x, int y, int nodeIndex, @Nullable Component tooltip, @Nullable Component shortText) {
            validatePad(x, y);
            pads.put(new Point(x, y), new PadData(nodeIndex, tooltip, shortText));
            return this;
        }

        public Builder addPad(int x, int y, int nodeIndex, String defaultLang, String defaultShort) {
            if(translationKey == null)
                throw new IllegalCallerException("This method may only be used when the translation key base is set");
            var key = translationKey + "." + nodeIndex;
            translatedPads.put(key, defaultLang);
            if(defaultShort != null)
                translatedPads.put(key + ".short", defaultShort);
            return addPad(x, y, nodeIndex, Component.translatable(key), defaultShort == null ? null : Component.translatable(key + ".short"));
        }

        public Builder withOutline() {
            outline = true;
            return this;
        }

        public Builder withItem() {
            this.withItem = true;
            return this;
        }

        public Builder withArrow(Orientation facing) {
            this.arrow = facing;
            return this;
        }

        public Builder withArrow() {
            this.arrow = Orientation.RIGHT;
            return this;
        }

        public ComponentFootprint build() {
            var padIndices = new TreeSet<Integer>();
            for(var pad : pads.values()) {
                if(pad.nodeIndex >= 0)
                    padIndices.add(pad.nodeIndex);
            }
            if(!padIndices.isEmpty()) {
                if (padIndices.first() != 0)
                    throw new IllegalStateException("Footprint pad indices must start from 0");
                if (padIndices.last() != padIndices.size() - 1)
                    throw new IllegalStateException("Footprint pad indices must not contain any gaps");
            }
            return new ComponentFootprint(width, height, pads, outline, withItem, arrow);
        }
    }

    public record PadData(int nodeIndex, @Nullable Component tooltip, @Nullable Component shortText) { }
}
