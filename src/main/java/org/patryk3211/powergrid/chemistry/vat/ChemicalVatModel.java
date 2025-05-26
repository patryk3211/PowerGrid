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
package org.patryk3211.powergrid.chemistry.vat;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.PowerGrid;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.chemistry.vat.ChemicalVatBlock.checkState;

public class ChemicalVatModel implements UnbakedModel, BakedModel, FabricBakedModel {
    public static final ModelIdentifier MODEL_ID = new ModelIdentifier(new Identifier(PowerGrid.MOD_ID, "chemical_vat_connected"), "");

    public static final SpriteIdentifier[] SPRITE_IDS = new SpriteIdentifier[] {
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PowerGrid.asResource("block/vat/side")),
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PowerGrid.asResource("block/vat/top")),
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PowerGrid.asResource("block/vat/bottom")),
            new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PowerGrid.asResource("block/vat/inner_side"))
    };

    public static final int SPRITE_SIDE = 0;
    public static final int SPRITE_TOP = 1;
    public static final int SPRITE_BOTTOM = 2;
    public static final int SPRITE_INNER = 3;

    private static final float CORNER = 2 / 16f;
    private static final float SIDE = 12 / 16f;

    private final Sprite[] sprites = new Sprite[SPRITE_IDS.length];

    private Mesh[] sideMeshes;
    private Mesh bottomMesh;

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return sprites[SPRITE_SIDE];
    }

    @Override
    public ModelTransformation getTransformation() {
        return null;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return null;
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {

    }

    private Mesh cube(Renderer renderer, float x0, float y0, float z0, float x1, float y1, float z1, @Nullable Direction... innerDirs) {
        var builder = renderer.meshBuilder();
        var emitter = builder.getEmitter();

        for(var dir : Direction.values()) {
            int spriteIndex = switch(dir) {
                case UP -> SPRITE_TOP;
                case DOWN -> SPRITE_BOTTOM;
                default -> SPRITE_SIDE;
            };
            for(var innerDir : innerDirs) {
                if(innerDir == dir) {
                    if(dir == Direction.UP) {
                        spriteIndex = SPRITE_BOTTOM;
                    } else {
                        spriteIndex = SPRITE_INNER;
                    }
                    break;
                }
            }

            float left = 0, bottom = 0, right = 1, top = 1;
            switch(dir) {
                case EAST -> {
                    left = 1.0f - z1;
                    right = 1.0f - z0;
                    bottom = y0;
                    top = y1;
                }
                case WEST -> {
                    left = z0;
                    right = z1;
                    bottom = y0;
                    top = y1;
                }
                case SOUTH -> {
                    left = x0;
                    right = x1;
                    bottom = y0;
                    top = y1;
                }
                case NORTH -> {
                    left = 1.0f - x1;
                    right = 1.0f - x0;
                    bottom = y0;
                    top = y1;
                }
                case UP -> {
                    bottom = 1.0f - z1;
                    top = 1.0f - z0;
                    left = x0;
                    right = x1;
                }
                case DOWN -> {
                    bottom = z0;
                    top = z1;
                    left = x0;
                    right = x1;
                }
            }
            float depth = switch(dir) {
                case WEST -> x0;
                case EAST -> 1.0f - x1;
                case DOWN -> y0;
                case UP -> 1.0f - y1;
                case NORTH -> z0;
                case SOUTH -> 1.0f - z1;
            };

            emitter.square(dir, left, bottom, right, top, depth);
            emitter.color(-1, -1, -1, -1);

            if(dir.getAxis() != Direction.Axis.Y) {
                emitter.uv(0, left, 1.0f - top);
                emitter.uv(1, left, 1.0f - bottom);
                emitter.uv(2, right, 1.0f - bottom);
                emitter.uv(3, right, 1.0f - top);
            } else {
                emitter.uv(0, left, top);
                emitter.uv(1, left, bottom);
                emitter.uv(2, right, bottom);
                emitter.uv(3, right, top);
            }
            emitter.spriteBake(sprites[spriteIndex], MutableQuadView.BAKE_NORMALIZED);

            emitter.emit();
        }

        return builder.build();
    }

    @Override
    public @Nullable BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        for(int i = 0; i < sprites.length; ++i) {
            sprites[i] = textureGetter.apply(SPRITE_IDS[i]);
        }

        var renderer = RendererAccess.INSTANCE.getRenderer();
        assert renderer != null;

        /*
         * Mesh array mapping:
         *     6 7 8
         *   ^ 3 4 5
         * x | 0 1 2
         *   + - >
         *     z
         */
        sideMeshes = new Mesh[9];
        sideMeshes[0] = cube(renderer, 0.0f, CORNER, 0.0f, CORNER, 1.0f, CORNER, Direction.EAST, Direction.SOUTH);
        sideMeshes[1] = cube(renderer, 0.0f, CORNER, CORNER, CORNER, 1.0f, CORNER + SIDE, Direction.EAST);
        sideMeshes[2] = cube(renderer, 0.0f, CORNER, CORNER + SIDE, CORNER, 1.0f, 1.0f, Direction.EAST, Direction.NORTH);
        sideMeshes[3] = cube(renderer, CORNER, CORNER, 0.0f, CORNER + SIDE, 1.0f, CORNER, Direction.SOUTH);
        sideMeshes[4] = null; // This is the block that's checking its neighbors
        sideMeshes[5] = cube(renderer, CORNER, CORNER, CORNER + SIDE, CORNER + SIDE, 1.0f, 1.0f, Direction.NORTH);
        sideMeshes[6] = cube(renderer, CORNER + SIDE, CORNER, 0.0f, 1.0f, 1.0f, CORNER, Direction.WEST, Direction.SOUTH);
        sideMeshes[7] = cube(renderer, CORNER + SIDE, CORNER, CORNER, 1.0f, 1.0f, CORNER + SIDE, Direction.WEST);
        sideMeshes[8] = cube(renderer, CORNER + SIDE, CORNER, CORNER + SIDE, 1.0f, 1.0f, 1.0f, Direction.WEST, Direction.NORTH);

        bottomMesh = cube(renderer, 0.0f, 0.0f, 0.0f, 1.0f, CORNER, 1.0f, Direction.UP);

        return this;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        for(int x = -1; x <= 1; ++x) {
            for(int z = -1; z <= 1; ++z) {
                if(x == 0 && z == 0)
                    continue;
                int meshIndex = z + 1 + (x + 1) * 3;
                if(x == 0 || z == 0) {
                    var neighbor = blockView.getBlockState(pos.add(x, 0, z));
                    if(checkState(state.getBlock(), neighbor)) {
                        sideMeshes[meshIndex].outputTo(context.getEmitter());
                    }
                } else {
                    var neighbor1 = blockView.getBlockState(pos.add(x, 0, 0));
                    var neighbor2 = blockView.getBlockState(pos.add(0, 0, z));
                    var corner = blockView.getBlockState(pos.add(x, 0, z));

                    var block = state.getBlock();
                    if(checkState(block, neighbor1) || checkState(block, neighbor2) || checkState(block, corner)) {
                        sideMeshes[meshIndex].outputTo(context.getEmitter());
                    }
                }
            }
        }
        bottomMesh.outputTo(context.getEmitter());
    }
}
