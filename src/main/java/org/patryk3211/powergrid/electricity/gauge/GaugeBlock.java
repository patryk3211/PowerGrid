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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(Rotation.CLOCKWISE_180);
    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(Rotation.CLOCKWISE_180);

    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(Rotation.CLOCKWISE_90);
    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(Rotation.CLOCKWISE_90);

    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(Rotation.COUNTERCLOCKWISE_90);
    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(Rotation.COUNTERCLOCKWISE_90);

    private static final VoxelShape SHAPE_NORTH_SOUTH = Shapes.or(
            box(1, 0, 2, 15, 14, 14),
            NORTH_TERMINAL_1.getShape(),
            NORTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_EAST_WEST = Shapes.or(
            box(2, 0, 1, 14, 14, 15),
            EAST_TERMINAL_1.getShape(),
            EAST_TERMINAL_2.getShape()
    );

    public GaugeBlock(Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case NORTH, SOUTH -> SHAPE_NORTH_SOUTH;
            case EAST, WEST -> SHAPE_EAST_WEST;
            default -> throw new IllegalArgumentException("Invalid horizontal facing");
        };
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        var be = getBlockEntity(level, pos);
        if(be == null)
            return 0;
        return be.redstoneOutput;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
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
    public boolean shouldRenderHeadOnFace(Level world, BlockPos pos, BlockState state, Direction dir) {
        var facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return dir.getAxis() == facing.getAxis();
    }
}
