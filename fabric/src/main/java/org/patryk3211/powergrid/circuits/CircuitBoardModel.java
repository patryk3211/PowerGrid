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
package org.patryk3211.powergrid.circuits;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
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
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.Area;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.Point;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_SIZE;

@Environment(EnvType.CLIENT)
public class CircuitBoardModel implements UnbakedModel, BakedModel {
    public static final ModelIdentifier MODEL_ID = new ModelIdentifier(new Identifier(PowerGrid.MOD_ID, "circuit_board"), "");
    public static final Identifier BASE_MODEL = PowerGrid.asResource("block/circuit_board");

    private static final SpriteIdentifier COPPER_SPRITE_ID = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PowerGrid.asResource("block/circuit_board_trace"));
    private static final SpriteIdentifier PAD_SPRITE_ID = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, PowerGrid.asResource("block/circuit_board_pad"));

    private Sprite particleSprite;
    private Sprite padSprite;
    private Sprite copperSprite;
    private BakedModel baseModel;

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return baseModel.getQuads(state, face, random);
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
        return particleSprite;
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelTransformation.NONE;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of(BASE_MODEL);
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {

    }

    @Override
    public @Nullable BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer, Identifier modelId) {
        copperSprite = textureGetter.apply(COPPER_SPRITE_ID);
        padSprite = textureGetter.apply(PAD_SPRITE_ID);
        baseModel = baker.bake(BASE_MODEL, rotationContainer);
        particleSprite = baseModel.getParticleSprite();
        return this;
    }

    public boolean isVanillaAdapter() {
        return false;
    }

    public void emitTrace(Area area, RenderContext context) {
        var emitter = context.getEmitter();
        float x1 = (float) area.x1() / GRID_SIZE;
        float y1 = (float) area.y1() / GRID_SIZE;
        float x2 = (float) area.x2() / GRID_SIZE;
        float y2 = (float) area.y2() / GRID_SIZE;
        emitter.square(Direction.UP, x1, 1.0f - y2, x2, 1.0f - y1, 14f / 16f - 0.0025f);
        emitter.uv(0, x1, y1);
        emitter.uv(1, x1, y2);
        emitter.uv(2, x2, y2);
        emitter.uv(3, x2, y1);
        emitter.color(-1, -1, -1, -1);
        emitter.spriteBake(copperSprite, MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
    }

    public void emitPad(Point point, RenderContext context) {
        var emitter = context.getEmitter();
        float x1 = point.x(), y1 = point.y(), x2 = x1 + 1, y2 = y1 + 1;
        x1 /= GRID_SIZE;
        x2 /= GRID_SIZE;
        y1 /= GRID_SIZE;
        y2 /= GRID_SIZE;
        emitter.square(Direction.UP, x1, 1.0f - y2, x2, 1.0f - y1, 14f / 16f - 0.0025f);
        emitter.uv(0, x1, y1);
        emitter.uv(1, x1, y2);
        emitter.uv(2, x2, y2);
        emitter.uv(3, x2, y1);
        emitter.color(-1, -1, -1, -1);
        emitter.spriteBake(padSprite, MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        baseModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);
        var be = blockView.getBlockEntity(pos);
        if(be instanceof CircuitBoardBlockEntity circuit) {
            // Emit components
            var schematic = circuit.getSchematic();
            for(var placed : schematic.components()) {
                if(placed instanceof IRenderedComponent rendered && !rendered.emitBaked())
                    continue;
                var model = ComponentModels.getModel(placed);
                if(placed.has(Orientation.PROPERTY)) {
                    var orientation = placed.get(Orientation.PROPERTY);
                    // We need the raw footprint dimensions (without rotations)
                    var footprint = placed.component.footprint(null);
                    context.pushTransform(new RotateOffsetTransform(placed.x, 2, placed.y, orientation, footprint.getWidth(), footprint.getHeight()));
                } else {
                    context.pushTransform(new OffsetTransform(placed.x, 2, placed.y));
                }
                model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
                context.popTransform();
            }

            // Emit traces
            var areas = circuit.getSchematic().calculateAreas(CircuitSchematic.Layer.FRONT);
            for(var area : areas) {
                emitTrace(area, context);
            }
            // Emit pads
            var pads = circuit.getSchematic().pads();
            var points = pads.calculatePoints();
            for(var point : points) {
                emitPad(point, context);
            }
        }
    }

    private static class OffsetTransform implements RenderContext.QuadTransform {
        protected final float x, y, z;

        public OffsetTransform(int x, int y, int z) {
            this.x = x / 16f;
            this.y = y / 16f;
            this.z = z / 16f;
        }

        @Override
        public boolean transform(MutableQuadView view) {
            for(int i = 0; i < 4; ++i) {
                var x = view.x(i);
                var y = view.y(i);
                var z = view.z(i);
                view.pos(i, x + this.x, y + this.y, z + this.z);
            }
            return true;
        }
    }

    private static class RotateOffsetTransform extends OffsetTransform {
        protected final Orientation orientation;
        protected final float width, height;

        public RotateOffsetTransform(int x, int y, int z, Orientation orientation, int width, int height) {
            super(x, y, z);
            this.orientation = orientation;
            this.width = width / 16f;
            this.height = height / 16f;
        }

        @Override
        public boolean transform(MutableQuadView view) {
            for(int i = 0; i < 4; ++i) {
                var x = view.x(i);
                var y = view.y(i);
                var z = view.z(i);
                switch(orientation) {
                    case DOWN -> {
                        // 90 degrees
                        var buf = x;
                        x = this.height - z;
                        z = buf;
                    }
                    case LEFT -> {
                        // 180 degrees
                        x = this.width - x;
                        z = this.height - z;
                    }
                    case UP -> {
                        // 270 degrees
                        var buf = z;
                        z = this.width - x;
                        x = buf;
                    }
                }
                view.pos(i, x + this.x, y + this.y, z + this.z);
            }
            return true;
        }
    }
}
