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

    public static void render(CircuitSchematic schematic, GuiGraphics context, int x, int y, int scale) {

    }

    // It's not the most efficient, but it gets the job done. The only way to make this better is to dynamically create textures.
    public static void renderLayer(List<Line> verticalLines, List<Line> horizontalLines,
                                   GuiGraphics ctx, int x, int y, int scale, int color) {
        int x1, x2, y1, y2;

        for (var line : verticalLines) {
            x1 = line.position() * scale + x + TRACE_PADDING;
            x2 = line.position() * scale + scale + x - TRACE_PADDING;
            y1 = line.start() * scale + y + TRACE_PADDING;
            y2 = line.end() * scale + scale + y - TRACE_PADDING;
            ctx.fill(x1, y1, x2, y2, color);
        }

        for (var line : horizontalLines) {
            x1 = line.start() * scale + x + TRACE_PADDING;
            x2 = line.end() * scale + scale + x - TRACE_PADDING;
            y1 = line.position() * scale + y + TRACE_PADDING;
            y2 = line.position() * scale + scale + y - TRACE_PADDING;
            ctx.fill(x1, y1, x2, y2, color);
        }
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
