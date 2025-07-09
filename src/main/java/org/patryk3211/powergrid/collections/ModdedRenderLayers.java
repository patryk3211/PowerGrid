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
package org.patryk3211.powergrid.collections;

import net.minecraft.client.render.*;

import java.util.OptionalDouble;

public class ModdedRenderLayers {
    private static final RenderLayer DEBUG_LINES = RenderLayer.of(
            "powergrid_debug_lines",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.DEBUG_LINES,
            256,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .transparency(RenderPhase.NO_TRANSPARENCY)
                    .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1.0f)))
                    .build(false)
    );

    private static final RenderLayer COLOR = RenderLayer.of(
            "powergrid_color",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.QUADS,
            256,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.COLOR_PROGRAM)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .transparency(RenderPhase.NO_TRANSPARENCY)
                    .build(false)
    );

    public static RenderLayer getDebugLines() {
        return DEBUG_LINES;
    }

    public static RenderLayer getColor() {
        return COLOR;
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
