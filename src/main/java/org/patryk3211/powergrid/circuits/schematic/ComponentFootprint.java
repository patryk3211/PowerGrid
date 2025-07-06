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

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;

import java.util.*;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class ComponentFootprint {
    private static final Identifier ARROWS = PowerGrid.texture("gui/circuit_arrows");

    private static final PadData NONE = new PadData(-1, null);

    private final int width;
    private final int height;

    private final SortedMap<Point, PadData> pads;
    private final boolean outline;
    @Nullable
    private final Supplier<Item> renderedItem;
    @Nullable
    private final Orientation arrow;

    private ItemStack renderedStack;

    protected ComponentFootprint(int width, int height, SortedMap<Point, PadData> pads, boolean outline, @Nullable Supplier<Item> renderedItem, @Nullable Orientation arrow) {
        this.width = width;
        this.height = height;
        this.pads = pads;
        this.outline = outline;
        this.renderedItem = renderedItem;
        this.arrow = arrow;
    }

    protected void renderPads(@NotNull DrawContext ctx, int x, int y) {
        for(var point : pads.keySet()) {
            int x1 = point.x() + x;
            int y1 = point.y() + y;
            ctx.fill(x1, y1, x1 + 1, y1 + 1, COLOR_TERMINAL);
        }
    }

    public void render(@NotNull DrawContext ctx, int x, int y) {
        if(outline) {
            ctx.drawBorder(x, y, width * GRID_TO_GRID_SCALE, height * GRID_TO_GRID_SCALE, COLOR_COMPONENT_OUTLINE);
        }
        renderPads(ctx, x, y);
        var ms = ctx.getMatrices();
        if(renderedItem != null) {
            ms.push();
            var scale = Math.min(width, height) / 16f * GRID_TO_GRID_SCALE;
            if(width > height) {
                float offset = (width - height) * 0.5f;
                ms.translate(offset, 0, 0);
            } else if(height > width) {
                float offset = (height - width) * 0.5f;
                ms.translate(0, offset, 0);
            }
            ms.scale(scale, scale, scale);
            ctx.drawItem(getRenderedStack(), (int) (x / scale), (int) (y / scale));
            ms.pop();
        }
        if(arrow != null) {
            ms.push();
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
            ctx.drawTexture(ARROWS, 0, 0, u, v, 8, 8, 16, 16);

            ms.pop();
        }
    }

    @Nullable
    public Text getTooltip(int mouseX, int mouseY) {
        var pad = pads.get(new Point(mouseX, mouseY));
        if(pad == null)
            return null;
        return pad.tooltip;
    }

    @Nullable
    public ItemStack getRenderedStack() {
        if(renderedItem == null)
            return null;
        if(renderedStack == null)
            renderedStack = renderedItem.get().getDefaultStack();
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

        var footprint = new ComponentFootprint(width, height, pads, this.outline, this.renderedItem, arrow == null ? null : arrow.rotate(orientation));
        // Copy cached stack if one is available.
        footprint.renderedStack = this.renderedStack;
        return footprint;
    }

    public static class Builder {
        private final int width, height;
        private final SortedMap<Point, PadData> pads = new TreeMap<>();
        private Supplier<Item> itemSupplier;
        private boolean outline = false;
        private Orientation arrow = null;

        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
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
            return addPad(x, y, nodeIndex, null);
        }

        public Builder addPad(int x, int y, int nodeIndex, @Nullable Text tooltip) {
            validatePad(x, y);
            pads.put(new Point(x, y), new PadData(nodeIndex, tooltip));
            return this;
        }

        public Builder withOutline() {
            outline = true;
            return this;
        }

        public Builder withItem(Supplier<Item> itemSupplier) {
            this.itemSupplier = itemSupplier;
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
            return new ComponentFootprint(width, height, pads, outline, itemSupplier, arrow);
        }
    }

    public record PadData(int nodeIndex, @Nullable Text tooltip) { }
}
