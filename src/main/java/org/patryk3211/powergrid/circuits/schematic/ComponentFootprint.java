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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;
import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class ComponentFootprint {
    private static final PadData NONE = new PadData(-1, null);

    private final int width;
    private final int height;

    private final SortedMap<Point, PadData> pads;
    private final boolean outline;
    @Nullable
    private final Supplier<Item> renderedItem;

    private ItemStack renderedStack;

    protected ComponentFootprint(int width, int height, SortedMap<Point, PadData> pads, boolean outline, @Nullable Supplier<Item> renderedItem) {
        this.width = width;
        this.height = height;
        this.pads = pads;
        this.outline = outline;
        this.renderedItem = renderedItem;
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
        if(renderedItem != null) {
            var ms = ctx.getMatrices();
            ms.push();
            var scale = Math.min(width, height) / 16f * GRID_TO_GRID_SCALE;
            ms.scale(scale, scale, scale);
            ctx.drawItem(getRenderedStack(), (int) (x / scale), (int) (y / scale));
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

    public static class Builder {
        private final int width, height;
        private final SortedMap<Point, PadData> pads = new TreeMap<>();
        private Supplier<Item> itemSupplier;
        private boolean outline = false;

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
            return new ComponentFootprint(width, height, pads, outline, itemSupplier);
        }
    }

    public record PadData(int nodeIndex, @Nullable Text tooltip) { }
}
