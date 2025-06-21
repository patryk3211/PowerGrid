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
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static org.patryk3211.powergrid.circuits.schematic.CircuitSchematicRender.*;

public class ComponentFootprint {
    private final int width;
    private final int height;

    private final Map<Point, Integer> pads = new HashMap<>();
    private boolean outline;
    private ItemStack renderedStack = null;

    public ComponentFootprint(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public ComponentFootprint addPad(int x, int y) {
        return addPad(x, y, -1);
    }

    public ComponentFootprint addPad(int x, int y, int nodeIndex) {
        pads.put(new Point(x, y), nodeIndex);
        return this;
    }

    public ComponentFootprint withOutline() {
        outline = true;
        return this;
    }

    public ComponentFootprint withItem(ItemConvertible item) {
        renderedStack = item.asItem().getDefaultStack();
        return this;
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
        if(renderedStack != null) {
            var ms = ctx.getMatrices();
            ms.push();
            var scale = Math.min(width, height) / 8f;
            ms.scale(scale, scale, scale);
            ctx.drawItem(renderedStack, (int) (x / scale), (int) (y / scale));
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
}
