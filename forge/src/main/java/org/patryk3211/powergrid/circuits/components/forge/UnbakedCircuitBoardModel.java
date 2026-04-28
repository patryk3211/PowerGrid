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
package org.patryk3211.powergrid.circuits.components.forge;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class UnbakedCircuitBoardModel implements IUnbakedGeometry<UnbakedCircuitBoardModel> {
    @Override
    public BakedModel bake(@NotNull IGeometryBakingContext context, ModelBaker bakery, Function<Material, TextureAtlasSprite> textureProvider, @NotNull ModelState state, @NotNull ItemOverrides overrides) {
        return new CircuitBoardModel(
                Objects.requireNonNull(bakery.bake(CircuitBoardModel.BASE_MODEL, state)),
                textureProvider.apply(CircuitBoardModel.PAD_SPRITE_ID),
                textureProvider.apply(CircuitBoardModel.COPPER_SPRITE_ID)
        );
    }
}
