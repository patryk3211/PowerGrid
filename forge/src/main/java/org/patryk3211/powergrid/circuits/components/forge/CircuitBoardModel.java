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

import com.mojang.math.Transformation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlockEntity;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardModelQuads;
import org.patryk3211.powergrid.circuits.components.ComponentModels;
import org.patryk3211.powergrid.circuits.components.IRenderedComponent;
import org.patryk3211.powergrid.circuits.components.properties.Orientation;
import org.patryk3211.powergrid.circuits.schematic.Area;
import org.patryk3211.powergrid.circuits.schematic.CircuitSchematic;
import org.patryk3211.powergrid.circuits.schematic.PlacedComponent;
import org.patryk3211.powergrid.circuits.schematic.Point;
import org.patryk3211.powergrid.circuits.components.Via2Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static org.patryk3211.powergrid.circuits.schematic.CircuitLayer.GRID_TO_GRID_SCALE;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CircuitBoardModel implements BakedModel {
    public static final ModelResourceLocation MODEL_ID = new ModelResourceLocation(PowerGrid.asResource("circuit_board"), "");
    public static final ResourceLocation BASE_MODEL = PowerGrid.asResource("block/circuit_board");

    public static final Material COPPER_SPRITE_ID = new Material(InventoryMenu.BLOCK_ATLAS, PowerGrid.asResource("block/circuit_board_trace"));
    public static final Material PAD_SPRITE_ID = new Material(InventoryMenu.BLOCK_ATLAS, PowerGrid.asResource("block/circuit_board_pad"));
    public static final Material PAD2_SPRITE_ID = new Material(InventoryMenu.BLOCK_ATLAS, PowerGrid.asResource("block/circuit_board_pad2"));

    public static final ModelProperty<CircuitBoardBlockEntity> ENTITY = new ModelProperty<>();
    public static final ModelProperty<List<Area>> FRONT_LAYER = new ModelProperty<>();
    public static final ModelProperty<List<Point>> PADS = new ModelProperty<>();
    public static final ModelProperty<List<PlacedComponent>> COMPONENTS = new ModelProperty<>();

    private static final FaceBakery bakery = new FaceBakery();

    private final TextureAtlasSprite particleSprite;
    private final TextureAtlasSprite padSprite;
    private final TextureAtlasSprite pad2Sprite;
    private final TextureAtlasSprite copperSprite;
    private final BakedModel baseModel;

    public CircuitBoardModel(BakedModel plateModel, TextureAtlasSprite padSprite, TextureAtlasSprite pad2Sprite ,TextureAtlasSprite copperSprite) {
        this.baseModel = plateModel;
        this.padSprite = padSprite;
        this.pad2Sprite = pad2Sprite;
        this.copperSprite = copperSprite;
        this.particleSprite = baseModel.getParticleIcon();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        return baseModel.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particleSprite;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        var be = level.getBlockEntity(pos);
        if(be instanceof CircuitBoardBlockEntity circuit) {
            // Emit components
            var schematic = circuit.getSchematic();
            return ModelData.builder()
                    .with(FRONT_LAYER, schematic.calculateAreas(CircuitSchematic.Layer.FRONT))
                    .with(PADS, schematic.pads().calculatePoints())
                    .with(COMPONENTS, List.copyOf(schematic.components()))
                    .with(ENTITY, circuit)
                    .build();
        }
        return modelData;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        var circuit = data.get(ENTITY);
        if(circuit != null) {
            if(circuit.quads != null) {
                var cached = circuit.quads.getQuads(state, side, renderType);
                if (cached != null)
                    return cached;
            } else {
                circuit.quads = new CircuitBoardModelQuads();
            }
        }
        var quads = new ArrayList<>(baseModel.getQuads(state, side, rand, data, renderType));

        if(data.has(COMPONENTS)) {
            var components = data.get(COMPONENTS);
            for(var placed : components) {
                if(placed instanceof IRenderedComponent rendered && !rendered.emitBaked())
                    continue;
                var model = ComponentModels.getModel(placed);

                IQuadTransformer transformer;
                if(placed.has(Orientation.PROPERTY)) {
                    var orientation = placed.get(Orientation.PROPERTY);
                    var footprint = placed.component.footprint(placed);
                        transformer = QuadTransformers.applying(new Transformation(
                                new Vector3f(placed.x / 16f - 0.5f, 2 / 16f - 0.5f, placed.y / 16f - 0.5f),
                                new Quaternionf().rotationY(orientation.ordinal() * (float) Math.PI * 0.5f),
                                null, null
                        ));
                    if(orientation != Orientation.RIGHT) {
                        transformer = transformer.andThen(QuadTransformers.applying(new Transformation(
                                switch(orientation) {
                                    case DOWN -> new Vector3f(footprint.getOriginalHeight() / 16f, 0, 0);
                                    case LEFT -> new Vector3f(footprint.getOriginalWidth() / 16f, 0, footprint.getOriginalHeight() / 16f);
                                    case UP -> new Vector3f(0, 0, footprint.getOriginalWidth() / 16f);
                                    case RIGHT -> throw new IllegalStateException();
                                }, null, null, null
                        )));
                    }
                } else {
                    transformer = QuadTransformers.applying(
                            new Transformation(new Vector3f(placed.x / 16f - 0.5f, 2 / 16f - 0.5f, placed.y / 16f - 0.5f),
                                    null, null, null)
                    );
                }
                if(placed.destroyed) {
                    transformer = transformer.andThen(QuadTransformers.applyingColor(64, 64, 64));
                }
                var componentQuads = model.getQuads(state, side, rand, data, renderType);
                quads.addAll(transformer.process(componentQuads));
            }
        }
        if(side == null) {
            if (data.has(FRONT_LAYER)) {
                var areas = data.get(FRONT_LAYER);
                for (var area : areas) {
                    quads.add(emitTrace(area));
                }
            }
            if (data.has(PADS)) {
                var pads = data.get(PADS);
                for (var pad : pads) {
                    if (isVia2Pad(pad, data)) {
                        quads.add(emitPad2(pad));
                    } else {
                        quads.add(emitPad(pad));
                    }
                }
            }
        }

        if(state != null) {
            var transformer = QuadTransformers.applying(new Transformation(
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    new Quaternionf()
                            .rotateY((float) Math.PI * CircuitBoardBlock.getAngleY(state) / 180f)
                            .rotateX((float) Math.PI * CircuitBoardBlock.getAngleX(state) / 180f),
                    null, null
            ));
            var trQuads = transformer.process(quads);
            if(circuit != null && circuit.quads != null)
                circuit.quads.putQuads(state, side, renderType, trQuads);
            return trQuads;
        }
        if(circuit != null && circuit.quads != null)
            circuit.quads.putQuads(state, side, renderType, quads);
        return quads;
    }

    public BakedQuad emitTrace(Area area) {
        float x1 = (float) area.x1() / GRID_TO_GRID_SCALE;
        float y1 = (float) area.y1() / GRID_TO_GRID_SCALE;
        float x2 = (float) area.x2() / GRID_TO_GRID_SCALE;
        float y2 = (float) area.y2() / GRID_TO_GRID_SCALE;

        return bakery.bakeQuad(
                new Vector3f(x1 - 8, 2.05f - 8, y1 - 8),
                new Vector3f(x2 - 8, 2.05f - 8, y2 - 8),
                new BlockElementFace(
                        null, BlockElementFace.NO_TINT, "circuit_board_trace",
                        new BlockFaceUV(new float[] { x1, y1, x2, y2 }, 0)
                ),
                copperSprite,
                Direction.UP,
                BlockModelRotation.X0_Y0,
                null,
                true,
                null // Dangerous but it should be fine if the rotation is not uv locked
        );
    }
    public BakedQuad emitPad2(Point point) {
        float x1 = point.x(), y1 = point.y(), x2 = x1 + 1, y2 = y1 + 1;
        x1 /= GRID_TO_GRID_SCALE;
        x2 /= GRID_TO_GRID_SCALE;
        y1 /= GRID_TO_GRID_SCALE;
        y2 /= GRID_TO_GRID_SCALE;
        return bakery.bakeQuad(
                new Vector3f(x1 - 8, 2.05f - 8, y1 - 8),
                new Vector3f(x2 - 8, 2.05f - 8, y2 - 8),
                new BlockElementFace(
                        null, BlockElementFace.NO_TINT, "circuit_board_trace",
                        new BlockFaceUV(new float[] { x1, y1, x2, y2 }, 0)
                ),
                pad2Sprite,
                Direction.UP,
                BlockModelRotation.X0_Y0,
                null,
                true,
                null // Dangerous but it should be fine if the rotation is not uv locked
        );
    }

    public BakedQuad emitPad(Point point) {
        float x1 = point.x(), y1 = point.y(), x2 = x1 + 1, y2 = y1 + 1;
        x1 /= GRID_TO_GRID_SCALE;
        x2 /= GRID_TO_GRID_SCALE;
        y1 /= GRID_TO_GRID_SCALE;
        y2 /= GRID_TO_GRID_SCALE;
        return bakery.bakeQuad(
                new Vector3f(x1 - 8, 2.05f - 8, y1 - 8),
                new Vector3f(x2 - 8, 2.05f - 8, y2 - 8),
                new BlockElementFace(
                        null, BlockElementFace.NO_TINT, "circuit_board_trace",
                        new BlockFaceUV(new float[] { x1, y1, x2, y2 }, 0)
                ),
                padSprite,
                Direction.UP,
                BlockModelRotation.X0_Y0,
                null,
                true,
                null // Dangerous but it should be fine if the rotation is not uv locked
        );
    }
    private boolean isVia2Pad(Point pad, ModelData data) {
        var components = data.get(COMPONENTS);

        for (var placed : components) {
            if (placed.component instanceof Via2Component) {
                // since your via is 1x1, this works perfectly
                if (placed.x == pad.x() && placed.y == pad.y()) {
                    return true;
                }
            }
        }
        return false;
    }
}


