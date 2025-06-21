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
import net.minecraft.client.gui.DrawContext;

import java.util.List;

@Environment(EnvType.CLIENT)
public class CircuitSchematicRender {
    public static void render(CircuitSchematic schematic, DrawContext context, int x, int y, int scale) {

    }

    // It's not the most efficient, but it gets the job done. The only way to make this better is to dynamically create textures.
    public static void renderLayer(List<CircuitLayer.Line> lines, DrawContext ctx, int x, int y, int scale, int color) {
        for(var line : lines) {
            int x1, x2, y1, y2;
            if(line.vertical()) {
                x1 = line.position() * scale + x;
                x2 = line.position() * scale + scale + x;
                y1 = line.start() * scale + y;
                y2 = line.end() * scale + y;
            } else {
                x1 = line.start() * scale + x;
                x2 = line.end() * scale + x;
                y1 = line.position() * scale + y;
                y2 = line.position() * scale + scale + y;
            }
            ctx.fill(x1, y1, x2, y2, color);
        }
    }
}
