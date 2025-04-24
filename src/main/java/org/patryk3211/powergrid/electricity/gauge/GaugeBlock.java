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
package org.patryk3211.powergrid.electricity.gauge;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public abstract class GaugeBlock<BE extends GaugeBlockEntity> extends ElectricBlock implements IBE<BE>, IGaugeBlock {
    private static final TerminalBoundingBox NORTH_TERMINAL_1 =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 13, 12, 7, 16, 16, 9)
                    .withOrigin(14.5, 15, 8)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox NORTH_TERMINAL_2 =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 0, 12, 7, 3, 16, 9)
                    .withOrigin(1.5, 15, 8)
                    .withColor(IDecoratedTerminal.BLUE);

    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_180);
    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_180);

    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);

    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);

    private static final VoxelShape SHAPE_NORTH_SOUTH = VoxelShapes.union(
            createCuboidShape(1, 0, 2, 15, 14, 14),
            NORTH_TERMINAL_1.getShape(),
            NORTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_EAST_WEST = VoxelShapes.union(
            createCuboidShape(2, 0, 1, 14, 14, 15),
            EAST_TERMINAL_1.getShape(),
            EAST_TERMINAL_2.getShape()
    );

    public enum Material {
        ANDESITE,
        BRASS
    }

    float maxValue;
    Material material;

    public GaugeBlock(Settings settings) {
        super(settings);
        maxValue = 0;
        material = null;
    }

    public static <B extends GaugeBlock<?>, P> NonNullUnaryOperator<BlockBuilder<B, P>> setMaxValue(float value) {
        return builder -> builder.onRegister(block -> block.maxValue = value);
    }

    public static <B extends GaugeBlock<?>, P> NonNullUnaryOperator<BlockBuilder<B, P>> setMaterial(Material material) {
        return builder -> builder.onRegister(block -> block.material = material);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH, SOUTH -> SHAPE_NORTH_SOUTH;
            case EAST, WEST -> SHAPE_EAST_WEST;
            default -> throw new IllegalArgumentException("Invalid horizontal facing");
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH -> switch(index) {
                case 0 -> NORTH_TERMINAL_1;
                case 1 -> NORTH_TERMINAL_2;
                default -> null;
            };
            case SOUTH -> switch(index) {
                case 0 -> SOUTH_TERMINAL_1;
                case 1 -> SOUTH_TERMINAL_2;
                default -> null;
            };
            case EAST -> switch(index) {
                case 0 -> EAST_TERMINAL_1;
                case 1 -> EAST_TERMINAL_2;
                default -> null;
            };
            case WEST -> switch(index) {
                case 0 -> WEST_TERMINAL_1;
                case 1 -> WEST_TERMINAL_2;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public float getMaxValue() {
        return maxValue;
    }

    @Override
    public boolean shouldRenderHeadOnFace(World world, BlockPos pos, BlockState state, Direction dir) {
        var facing = state.get(Properties.HORIZONTAL_FACING);
        return dir.getAxis() == facing.getAxis();
    }

    public Material getMaterial() {
        return material;
    }
}
