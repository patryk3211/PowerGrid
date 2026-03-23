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
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import static org.patryk3211.powergrid.circuits.editor.CircuitDesignTableEditScreen.TRACE_PADDING;
import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;

@Environment(EnvType.CLIENT)
public class CircuitSchematicRender {
    public static final int COLOR_TERMINAL = 0xFFFCB603;
    public static final int COLOR_TRACE_FRONT = 0xFFFFFFFF;
    public static final int COLOR_TRACE_BACK = 0x80FFFFFF;
    public static final int COLOR_COMPONENT_OUTLINE = 0x80F078EE;
    public static final int COLOR_SELECT_OUTLINE = 0x80EBBA34;

    public static void render(CircuitSchematic schematic, GuiGraphics context, int x, int y, int scale) {}

    // It's not the most efficient, but it gets the job done. The only way to make this better is to dynamically create textures.
    public static void renderLayer(CircuitLayer layer, GuiGraphics ctx, int xOffset, int yOffset, int scale, int color) {
        for (var line : layer.readVerticalLines()) {
            drawTrace(line.position(), line.start(), line.position(), line.end(), ctx, xOffset, yOffset, scale, color);
        }

        int startX, startY, endY;

        for (var line : layer.readHorizontalLines()) {
            if ((color & 0xFF000000) != 0xFF000000) {
                // Overlaps will be visible for non-opaque colors, so trade off more fill calls for avoiding overlaps
                startX = layer.hasVerticalTrace(line.start(), line.position()) ?
                        line.start() * scale + scale + xOffset - TRACE_PADDING :
                        line.start() * scale + xOffset + TRACE_PADDING;

                startY = line.position() * scale + yOffset + TRACE_PADDING;
                endY = line.position() * scale + scale + yOffset - TRACE_PADDING;

                for (int i = line.start(); i <= line.end(); i++) {
                    if (layer.hasVerticalTrace(i, line.position())) {
                        // Flush line, skip the center of this cell, and start a new line
                        ctx.fill(startX, startY, i * scale + xOffset + TRACE_PADDING, endY, color);
                        startX = i * scale + scale + xOffset - TRACE_PADDING;
                    }
                    else if (i == line.end()) {
                        ctx.fill(startX, startY, i * scale + scale + xOffset - TRACE_PADDING, endY, color);
                    }
                }
            }
            else {
                // Overlaps with vertical lines are invisible for opaque colors
                drawTrace(line.start(), line.position(), line.end(), line.position(), ctx, xOffset, yOffset, scale, color);
            }
        }
    }

    private static void drawTrace(int x1, int y1, int x2, int y2, GuiGraphics ctx, int xOffset, int yOffset, int scale, int color) {
        ctx.fill(
            x1 * scale + xOffset + TRACE_PADDING,
            y1 * scale + yOffset + TRACE_PADDING,
            x2 * scale + scale + xOffset - TRACE_PADDING,
            y2 * scale + scale + yOffset - TRACE_PADDING,
            color
        );
    }

    public static void renderPoints(List<Point> points, GuiGraphics ctx, int x, int y, int scale, int color) {
        for(var point : points) {
            int x1 = x + point.x() * scale;
            int y1 = y + point.y() * scale;
            ctx.fill(x1, y1, x1 + scale, y1 + scale, color);
        }
    }

    public static void renderComponents(CircuitSchematic schematic, GuiGraphics ctx, int x, int y, int scale) {
        var ms = ctx.pose();
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.scale(scale, scale, scale);
        for(var placed : schematic.components()) {
            placed.footprint().render(ctx, placed.component, placed.x * GRID_TO_GRID_SCALE, placed.y * GRID_TO_GRID_SCALE, false);
        }
        ms.popPose();
    }

    public static void renderComponents(CircuitSchematic schematic, GuiGraphics ctx, int x, int y, int scale, int mouseX, int mouseY) {
        var ms = ctx.pose();
        ms.pushPose();
        ms.translate(x, y, 0);
        ms.scale(scale, scale, scale);
        var mX = (mouseX - x) / scale;
        var mY = (mouseY - y) / scale;
        for(var placed : schematic.components()) {
            var hovering = mX >= placed.x && mY >= placed.y && mX < placed.x + placed.footprint().getWidth() && mY < placed.y + placed.footprint().getHeight();
            placed.footprint().render(ctx, placed.component, placed.x * GRID_TO_GRID_SCALE, placed.y * GRID_TO_GRID_SCALE, hovering);
        }
        ms.popPose();
    }
}
