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

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.patryk3211.powergrid.electricity.wire.BlockWireEntity;
import org.patryk3211.powergrid.electricity.wire.BlockWireRenderer;

import java.util.OptionalDouble;

public class ModdedRenderLayers {
    private static final RenderLayer WIRES = RenderLayer.of(
            "wire",
        VertexFormats.POSITION_COLOR_LIGHT,
        VertexFormat.DrawMode.TRIANGLES,
        512,
        RenderLayer.MultiPhaseParameters.builder()
                .program(RenderPhase.POSITION_COLOR_LIGHTMAP_PROGRAM)
                .texture(RenderPhase.NO_TEXTURE)
                .cull(RenderPhase.DISABLE_CULLING)
                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                .build(false)
        );
    private static final RenderLayer BLOCK_WIRES = RenderLayer.of(
            "block_wire",
            VertexFormats.POSITION_TEXTURE_COLOR_NORMAL,
            VertexFormat.DrawMode.QUADS,
            1024,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_TEXTURE_PROGRAM)
                    .texture(new RenderPhase.Texture(BlockWireRenderer.TEXTURE, false, false))
                    .cull(RenderPhase.ENABLE_CULLING)
                    .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                    .build(false)
    );

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

    public static RenderLayer getWireLayer() {
        return WIRES;
    }

    public static RenderLayer getBlockWireLayer() {
        return BLOCK_WIRES;
    }

    public static RenderLayer getDebugLines() {
        return DEBUG_LINES;
    }

    @SuppressWarnings("EmptyMethod")
    public static void register() { /* Initialize static fields. */ }
}
