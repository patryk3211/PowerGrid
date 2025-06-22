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

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class ComponentFootprint {
    private final int width;
    private final int height;

    private final ImmutableMap<Point, Integer> pads;
    private final boolean outline;
    @Nullable
    private final Supplier<Item> renderedItem;

    private ItemStack cachedStack;

    protected ComponentFootprint(int width, int height, Map<Point, Integer> pads, boolean outline, @Nullable Supplier<Item> renderedItem) {
        this.width = width;
        this.height = height;
        this.pads = ImmutableMap.copyOf(pads);
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
            ctx.drawBorder(x, y, width * 2, height * 2, COLOR_COMPONENT_OUTLINE);
        }
        renderPads(ctx, x, y);
        if(renderedItem != null) {
            if(cachedStack == null) {
                cachedStack = renderedItem.get().getDefaultStack();
            }

            var ms = ctx.getMatrices();
            ms.push();
            var scale = Math.min(width, height) / 8f;
            ms.scale(scale, scale, scale);
            ctx.drawItem(cachedStack, (int) (x / scale), (int) (y / scale));
            ms.pop();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Map<Point, Integer> getPads() {
        return pads;
    }

    public static class Builder {
        private final int width, height;
        private final Map<Point, Integer> pads = new HashMap<>();
        private Supplier<Item> itemSupplier;
        private boolean outline = false;

        public Builder(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public Builder addPad(int x, int y) {
            return addPad(x, y, -1);
        }

        public Builder addPad(int x, int y, int nodeIndex) {
            pads.put(new Point(x, y), nodeIndex);
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
            return new ComponentFootprint(width, height, pads, outline, itemSupplier);
        }
    }
}
