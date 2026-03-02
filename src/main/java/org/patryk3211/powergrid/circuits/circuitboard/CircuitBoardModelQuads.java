/*
 * Copyright 2026 patryk3211
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
package org.patryk3211.powergrid.circuits.circuitboard;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class CircuitBoardModelQuads {
    private final Map<CacheKey, List<BakedQuad>> quads = new Object2ObjectArrayMap<>(8);

    @Nullable
    public List<BakedQuad> getQuads(@Nullable Direction side, @Nullable RenderType type) {
        return quads.get(new CacheKey(side, type));
    }

    public void putQuads(@Nullable Direction side, @Nullable RenderType type, List<BakedQuad> quads) {
        this.quads.put(new CacheKey(side, type), quads);
    }

    private record CacheKey(@Nullable Direction side, @Nullable RenderType type) { }
}
