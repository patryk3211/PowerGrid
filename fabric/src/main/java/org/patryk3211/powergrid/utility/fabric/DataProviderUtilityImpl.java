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
package org.patryk3211.powergrid.utility.fabric;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import io.github.fabricators_of_create.porting_lib.models.generators.ConfiguredModel;
import io.github.fabricators_of_create.porting_lib.models.generators.ModelFile;
import io.github.fabricators_of_create.porting_lib.models.generators.block.MultiPartBlockStateBuilder;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.apache.logging.log4j.util.TriConsumer;
import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.circuitboard.CircuitBoardBlock;
import org.patryk3211.powergrid.circuits.editor.CircuitDesignTableBlock;
import org.patryk3211.powergrid.electricity.basinheater.BasinHeaterBlock;
import org.patryk3211.powergrid.electricity.carbonpile.CarbonPileBlock;
import org.patryk3211.powergrid.electricity.electricswitch.HvSwitchBlock;
import org.patryk3211.powergrid.electricity.electricswitch.SwitchBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseHolderBlock;
import org.patryk3211.powergrid.electricity.fuse.FuseState;
import org.patryk3211.powergrid.electricity.light.fixture.LightFixtureBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerMediumBlock;
import org.patryk3211.powergrid.electricity.transformer.TransformerSmallBlock;
import org.patryk3211.powergrid.kinetics.generator.housing.GeneratorHousing;
import org.patryk3211.powergrid.kinetics.generator.inductionrotor.VerticalCommutatorBlock;
import org.patryk3211.powergrid.kinetics.generator.rotor.AbstractRotorBlock;
import org.patryk3211.powergrid.kinetics.generator.winding.WindingBlock;

import java.util.function.Function;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.*;
import static org.patryk3211.powergrid.base.CustomProperties.ALONG_FIRST_AXIS;
import static org.patryk3211.powergrid.base.CustomProperties.ROTATION_4;

@SuppressWarnings("unused")
public class DataProviderUtilityImpl {
    public static ModelFile.ExistingModelFile modModel(RegistrateBlockstateProvider prov, String name) {
        return prov.models().getExistingFile(prov.modLoc(name));
    }

    public static ModelFile.UncheckedModelFile unchecked(String name) {
        return new ModelFile.UncheckedModelFile(PowerGrid.asResource(name));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalBlock(String model) {
        return (ctx, prov) ->
                prov.horizontalBlock(ctx.getEntry(), modModel(prov, model));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalBlock(Function<BlockState, String> model) {
        return (ctx, prov) ->
                prov.horizontalBlock(ctx.getEntry(), state -> modModel(prov, model.apply(state)));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> alternateDirectionalBlock(Function<BlockState, String> modelProvider) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry())
                .forAllStates(state -> {
                    var builder = ConfiguredModel.builder().modelFile(modModel(prov, modelProvider.apply(state)));
                    switch(state.getValue(FACING)) {
                        case SOUTH -> builder.rotationY(180);
                        case EAST -> builder.rotationY(90);
                        case WEST -> builder.rotationY(-90);
                        case UP -> builder.rotationX(-90);
                        case DOWN -> builder.rotationX(90);
                    }
                    return builder.build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> horizontalAxisBlock(String model) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder().modelFile(modModel(prov, model));
                    if(state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == Direction.Axis.X)
                        builder.rotationY(90);
                    return builder.build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> surfaceSwitch(String baseName) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder();
                    var suffix = state.getValue(SwitchBlock.OPEN) ? "_off" : "_on";
                    surfaceFacingTransforms(state, (x, y, vertical) -> {
                        var suffix2 = vertical ? "_v" : "_h";
                        builder.modelFile(modModel(prov, baseName + suffix + suffix2));
                        builder.rotationX(x).rotationY(y);
                    });
                    return builder.build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> surfaceBlock(String baseName) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder();
                    surfaceFacingTransforms(state, (x, y, vertical) -> {
                        var suffix = vertical ? "_v" : "_h";
                        builder.modelFile(modModel(prov, baseName + suffix));
                        builder.rotationX(x).rotationY(y);
                    });
                    return builder.build();
                });
    }

    public static NonNullBiConsumer<DataGenContext<Block, LightFixtureBlock>, RegistrateBlockstateProvider> lightFixture(String baseName) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder();
                    var facing = state.getValue(FACING);
                    var axis_along_first = state.getValue(ALONG_FIRST_AXIS);

                    int x = 0, y = 0;
                    boolean vertical= false;
                    switch(facing) {
                        case UP -> vertical = true;
                        case DOWN -> { x = 180; vertical = true; }
                        case EAST -> y = 180;
                        case NORTH -> y = 90;
                        case SOUTH -> y = -90;
                    }

                    if(!axis_along_first && vertical)
                        y = 90;
                    else if(axis_along_first && !vertical)
                        x = -90;

                    var suffix = vertical ? "_v" : "_h";
                    builder.modelFile(modModel(prov, baseName + suffix));
                    builder.rotationX(x).rotationY(y);
                    return builder.build();
                });
    }

    // This function needs two models. One for Y axis and one for other axis.
    public static void surfaceFacingTransforms(BlockState state, TriConsumer<Integer, Integer, Boolean> transformer) {
        var facing = state.getValue(FACING);
        boolean axis_along_first = false;
        if(state.hasProperty(ALONG_FIRST_AXIS)) {
            axis_along_first = state.getValue(ALONG_FIRST_AXIS);
        } else if (state.hasProperty(ROTATION_4)) {
            axis_along_first = state.getValue(ROTATION_4) % 2 == 1;
        }

        int x = 0, y = 0;
        boolean verticalModel = false;
        switch(facing) {
            case UP -> { x = 180; verticalModel = true; }
            case DOWN -> verticalModel = true;
            case WEST -> y = 180;
            case NORTH -> y = -90;
            case SOUTH -> y = 90;
        }

        if(!axis_along_first) {
            if(verticalModel) {
                y = 90;
            } else {
                x = -90;
            }
        }

        transformer.accept(x, y, verticalModel);
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> housing(String name) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder().modelFile(modModel(prov, name));
                    int x = 0;
                    int y = 0;
                    var facing = state.getValue(GeneratorHousing.HORIZONTAL_FACING);
                    if(facing.getAxis() == Direction.Axis.X)
                        y = -90;
                    if(facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
                        x = -90;
                    if(state.getValue(GeneratorHousing.UP)) {
                        x = 90 - x;
                    }
                    return builder.rotationX(x).rotationY(y).build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> basinHeater(String baseName) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder();
                    var model = switch(state.getValue(BasinHeaterBlock.HEAT_LEVEL)) {
                        case NONE, SMOULDERING -> baseName;
                        case FADING, KINDLED -> baseName + "_on";
                        case SEETHING -> baseName + "_seething";
                    };
                    builder.modelFile(modModel(prov, model));
                    return builder.build();
                });
    }

    public static <T extends AbstractRotorBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> rotorModel(String name) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            int x = 90;
            int y = 0;
            var model = modModel(prov, name);
            var builder = ConfiguredModel.builder().modelFile(model);
            Direction.Axis axis;
            if(state.hasProperty(AXIS)) {
                axis = state.getValue(AXIS);
            } else if(state.hasProperty(HORIZONTAL_AXIS)) {
                axis = state.getValue(HORIZONTAL_AXIS);
            } else {
                axis = Direction.Axis.Z;
            }
            switch(axis) {
                case X -> builder.rotationY(y - 90);
                case Z -> builder.rotationY(y);
                case Y -> builder.rotationX(x);
            }
            return builder.build();
        });
    }

    public static <T extends VerticalCommutatorBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> verticalCommutator(String name) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            var model = modModel(prov, name);
            var builder = ConfiguredModel.builder().modelFile(model);
            Direction facing = state.getValue(HORIZONTAL_FACING);
            if(!state.getValue(UP)) {
                builder.rotationX(180);
            }
            builder.rotationY((int) (facing.toYRot() - 180));
            return builder.build();
        });
    }

    public static <T extends HvSwitchBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> hvSwitch(String baseName) {
        return (ctx, prov) ->
                prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
                    var builder = ConfiguredModel.builder();
                    var part = state.getValue(HvSwitchBlock.PART);
                    if (part == 0) {
                        builder.modelFile(modModel(prov, baseName + "_body"));
                    } else {
                        builder.modelFile(modModel(prov, baseName + "_receptacle"));
                    }
                    var facing = state.getValue(HORIZONTAL_FACING);
                    builder.rotationY((int) facing.toYRot());
                    return builder.build();
                });
    }

    public static NonNullBiConsumer<DataGenContext<Block, TransformerSmallBlock>, RegistrateBlockstateProvider> transformerSmall() {
        return (ctx, prov) ->
                prov.getMultipartBuilder(ctx.getEntry())
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small")).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.Z)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil1")).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.Z)
                        .condition(TransformerSmallBlock.COILS, 1, 2)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil2")).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.Z)
                        .condition(TransformerSmallBlock.COILS, 2)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small")).rotationY(90).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil1")).rotationY(90).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                        .condition(TransformerSmallBlock.COILS, 1, 2)
                        .end()
                        .part()
                        .modelFile(modModel(prov, "block/transformer/small_coil2")).rotationY(90).addModel()
                        .condition(TransformerSmallBlock.HORIZONTAL_AXIS, Direction.Axis.X)
                        .condition(TransformerSmallBlock.COILS, 2)
                        .end();
    }

    public static NonNullBiConsumer<DataGenContext<Block, TransformerMediumBlock>, RegistrateBlockstateProvider> transformerMedium() {
        return (ctx, prov) -> {
            var builder = prov.getMultipartBuilder(ctx.getEntry());
            transformerMedium(builder, prov, Direction.Axis.Z);
            transformerMedium(builder, prov, Direction.Axis.X);
        };
    }

    public static void transformerMedium(MultiPartBlockStateBuilder builder, RegistrateBlockstateProvider prov, Direction.Axis axis) {
        transformerMediumPart(builder, prov, "block/transformer/medium_bottom", axis, 0, false).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_bottom_1", axis, 1, false).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_top", axis, 2, false).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_top_1", axis, 3, false).end();

        transformerMediumPart(builder, prov, "block/transformer/medium_coil1", axis, 0, true)
                .condition(TransformerMediumBlock.COILS, 1, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_coil1", axis, 1, true)
                .condition(TransformerMediumBlock.COILS, 1, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_coil2", axis, 2, true)
                .condition(TransformerMediumBlock.COILS, 2).end();
        transformerMediumPart(builder, prov, "block/transformer/medium_coil2", axis, 3, true)
                .condition(TransformerMediumBlock.COILS, 2).end();
    }

    private static MultiPartBlockStateBuilder.PartBuilder transformerMediumPart(MultiPartBlockStateBuilder builder, RegistrateBlockstateProvider prov, String model, Direction.Axis axis, int part, boolean coil) {
        int y = 0;
        if((part % 2 == 1) && coil)
            y = 180;
        if(axis == Direction.Axis.X)
            y -= 90;
        return builder.part()
                .modelFile(modModel(prov, model))
                .rotationY(y)
                .addModel()
                .condition(TransformerMediumBlock.HORIZONTAL_AXIS, axis)
                .condition(TransformerMediumBlock.PART, part);
    }

    public static <T extends WindingBlock> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> windingModel() {
        return (ctx, prov) -> {
            var builder = prov.getMultipartBuilder(ctx.getEntry());
            for(var axis : Direction.Axis.values()) {
                for(int i = 0; i <= 2; ++i) {
                    windingModel(axis, i, true, builder, prov);
                    windingModel(axis, i, false, builder, prov);
                }
            }
        };
    }

    private static void windingModel(Direction.Axis axis, int part, boolean alongFirst, MultiPartBlockStateBuilder builder, RegistrateBlockstateProvider prov) {
        int x = 0, y = 0;
        switch(axis) {
            case X -> y = -90;
            case Y -> x =  90;
        }
        if(part == 2) {
            if(axis == Direction.Axis.Y) {
                x += 180;
            } else {
                y += 180;
            }
        }
        var model = switch(part) {
            case 0, 2 -> {
                var base = alongFirst ? "block/winding/end_v" : "block/winding/end";
                if(part == 2 && !alongFirst && axis.isHorizontal())
                    base += "_1";
                yield modModel(prov, base);
            }
            case 1 -> modModel(prov, alongFirst ? "block/winding/middle_v" : "block/winding/middle");
            default -> throw new IllegalStateException();
        };

        Function<Boolean, ModelFile.ExistingModelFile> caseModel = half -> {
            boolean condition = !half;
            if(axis == Direction.Axis.Z)
                condition ^= alongFirst && part == 2;
            else if(axis == Direction.Axis.Y)
                condition ^= !alongFirst && part != 2;
            else
                condition ^= alongFirst && part != 2;
            var halfStr = condition ? "p" : "n";
            return switch(part) {
                case 0, 2 -> modModel(prov, "block/winding/end" + (alongFirst ? "_v" : "") + "_case_" + halfStr);
                case 1 -> modModel(prov, "block/winding/middle" + (alongFirst ? "_v" : "") + "_case_" + halfStr);
                default -> throw new IllegalStateException();
            };
        };
        // Case halves
        builder.part()
                .modelFile(caseModel.apply(false))
                .rotationX(x)
                .rotationY(y)
                .addModel()
                .condition(AXIS, axis)
                .condition(WindingBlock.PART, part)
                .condition(ALONG_FIRST_AXIS, alongFirst)
                .condition(WindingBlock.CASE_LEFT, true)
                .end();
        builder.part()
                .modelFile(caseModel.apply(true))
                .rotationX(x)
                .rotationY(y)
                .addModel()
                .condition(AXIS, axis)
                .condition(WindingBlock.PART, part)
                .condition(ALONG_FIRST_AXIS, alongFirst)
                .condition(WindingBlock.CASE_RIGHT, true)
                .end();
        if(axis == Direction.Axis.Y && alongFirst && part == 0)
            y = 180;
        else if(axis == Direction.Axis.Y && alongFirst && part == 1)
            y += 180;
        // Main
        builder.part()
                .modelFile(model)
                .rotationX(x)
                .rotationY(y)
                .addModel()
                .condition(AXIS, axis)
                .condition(WindingBlock.PART, part)
                .condition(ALONG_FIRST_AXIS, alongFirst)
                .end();
    }

    public static void rotateDownFacingModel(ConfiguredModel.Builder<?> builder, Direction facing) {
        switch(facing) {
            case UP -> builder.rotationX(180);
            case NORTH -> builder.rotationX(-90);
            case SOUTH -> builder.rotationX(90);
            case WEST -> builder.rotationX(90).rotationY(90);
            case EAST -> builder.rotationX(90).rotationY(-90);
        }
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> northFacing(String name) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            var builder = ConfiguredModel.builder();
            builder.modelFile(modModel(prov, name));
            switch(state.getValue(FACING)) {
                case DOWN -> builder.rotationX(90);
                case UP -> builder.rotationX(-90);
                case SOUTH -> builder.rotationY(180);
                case EAST -> builder.rotationY(90);
                case WEST -> builder.rotationY(-90);
            }
            return builder.build();
        });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> downFacing(String name) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry())
                .forAllStates(state -> {
                    var builder = ConfiguredModel.builder().modelFile(modModel(prov, name));
                    rotateDownFacingModel(builder, state.getValue(FACING));
                    return builder.build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> upFacing(String name) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            var builder = ConfiguredModel.builder();
            builder.modelFile(modModel(prov, name));
            switch(state.getValue(FACING)) {
                case DOWN -> builder.rotationX(180);
                case NORTH -> builder.rotationX(90);
                case SOUTH -> builder.rotationX(-90);
                case EAST -> builder.rotationX(90).rotationY(90);
                case WEST -> builder.rotationX(90).rotationY(-90);
            }
            return builder.build();
        });
    }

    public static NonNullBiConsumer<DataGenContext<Block, CircuitBoardBlock>, RegistrateBlockstateProvider> circuitBoard() {
        return (ctx, prov) ->
                prov.simpleBlock(ctx.getEntry(), unchecked("circuit_board"));
    }

    public static NonNullBiConsumer<DataGenContext<Block, CircuitDesignTableBlock>, RegistrateBlockstateProvider> circuitDesignTable() {
        return (ctx, prov) -> prov
                .simpleBlock(ctx.getEntry(), prov.models()
                        .cubeBottomTop(ctx.getName(),
                                prov.modLoc("block/circuit_design_table_side"),
                                prov.mcLoc("block/spruce_planks"),
                                prov.modLoc("block/circuit_design_table_top")));
    }

    public static NonNullBiConsumer<DataGenContext<Block, FuseHolderBlock>, RegistrateBlockstateProvider> fuseHolder() {
        return (ctx, prov) -> {
            var builder = prov.getMultipartBuilder(ctx.getEntry());
            for(var facing : FACING.getPossibleValues()) {
                for(var axis : ALONG_FIRST_AXIS.getPossibleValues()) {
                    var state = ctx.getEntry().defaultBlockState()
                            .setValue(FACING, facing)
                            .setValue(ALONG_FIRST_AXIS, axis);
                    surfaceFacingTransforms(state, (x, y, vertical) -> {
                        var part = builder.part();
                        if (vertical) {
                            part.modelFile(modModel(prov, "block/fuse_holder"));
                        } else {
                            part.modelFile(modModel(prov, "block/fuse_holder_h"));
                        }
                        part.rotationX(x).rotationY(y);
                        part.addModel().condition(FACING, facing).condition(ALONG_FIRST_AXIS, axis);
                    });
                }
                var part = builder.part();
                part.modelFile(modModel(prov, "block/fuse"));
                rotateDownFacingModel(part, facing);
                part.addModel().condition(FACING, facing).condition(FuseHolderBlock.STATE, FuseState.CLOSED);

                part = builder.part();
                part.modelFile(modModel(prov, "block/fuse_blown"));
                rotateDownFacingModel(part, facing);
                part.addModel().condition(FACING, facing).condition(FuseHolderBlock.STATE, FuseState.BLOWN);
            }
        };
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cubeColumn(String side, String end) {
        return (ctx, prov) -> prov
                .simpleBlock(ctx.getEntry(), prov.models().cubeColumn(ctx.getName(), prov.modLoc(side), prov.modLoc(end)));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> cubeAllWithItem(String name) {
        return (ctx, prov) -> prov
                .simpleBlockWithItem(ctx.getEntry(), prov.models().cubeAll(ctx.getName(), prov.modLoc(name)));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> simple(String model) {
        return (ctx, prov) -> prov
                .simpleBlock(ctx.getEntry(), modModel(prov, model));
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> carbonPile(String baseName) {
        return (ctx, prov) -> prov.getVariantBuilder(ctx.getEntry())
                .forAllStates(state -> {
                    var builder = ConfiguredModel.builder();
                    builder.modelFile(modModel(prov, baseName + (state.getValue(CarbonPileBlock.TOP) ? "/top" : "/center")));
                    return builder.build();
                });
    }

    public static <T extends Block> NonNullBiConsumer<DataGenContext<Block, T>, RegistrateBlockstateProvider> air() {
        return (ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                prov.models().getExistingFile(prov.mcLoc("block/air")));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> generated() {
        return (ctx, prov) -> prov
                .generated(ctx.lazy());
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> gauge(String model, String texture) {
        return (ctx, prov) ->
                prov.withExistingParent(ctx.getName(), prov.modLoc(model))
                        .texture("2", prov.modLoc(texture));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> itemWithParent(String parent) {
        return (ctx, prov) ->
                prov.withExistingParent(ctx.getName(), prov.modLoc(parent));
    }

    public static <T extends Item> NonNullBiConsumer<DataGenContext<Item, T>, RegistrateItemModelProvider> barrier() {
        return (ctx, prov) ->
                prov.withExistingParent(ctx.getName(), prov.mcLoc("item/barrier"));
    }
}
