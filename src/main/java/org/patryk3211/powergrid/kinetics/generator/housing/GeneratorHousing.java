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
package org.patryk3211.powergrid.kinetics.generator.housing;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class GeneratorHousing extends Block implements IWrenchable {
    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;

    private static final VoxelShape SHAPE_SOUTH_DOWN = box(0, 0, 2, 16, 14, 16);
    private static final VoxelShape SHAPE_SOUTH_UP = box(0, 2, 2, 16, 16, 16);
    private static final VoxelShape SHAPE_NORTH_DOWN = box(0, 0, 0, 16, 14, 14);
    private static final VoxelShape SHAPE_NORTH_UP = box(0, 2, 0, 16, 16, 14);

    private static final VoxelShape SHAPE_EAST_DOWN = box(2, 0, 0, 16, 14, 16);
    private static final VoxelShape SHAPE_EAST_UP = box(2, 2, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_WEST_DOWN = box(0, 0, 0, 14, 14, 16);
    private static final VoxelShape SHAPE_WEST_UP = box(0, 2, 0, 14, 16, 16);

    public GeneratorHousing(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, UP);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        boolean up = state.getValue(UP);
        return switch(state.getValue(HORIZONTAL_FACING)) {
            case SOUTH -> up ? SHAPE_SOUTH_UP : SHAPE_SOUTH_DOWN;
            case NORTH -> up ? SHAPE_NORTH_UP : SHAPE_NORTH_DOWN;
            case EAST -> up ? SHAPE_EAST_UP : SHAPE_EAST_DOWN;
            case WEST -> up ? SHAPE_WEST_UP : SHAPE_WEST_DOWN;
            default -> null;
        };
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        BlockState newState = null;
        var facing = state.getValue(HORIZONTAL_FACING);
        if(context.getClickedFace().getAxis() == Direction.Axis.Y) {
            newState = state.setValue(HORIZONTAL_FACING, facing.getClockWise());
        } else if(context.getClickedFace().getAxis() == facing.getAxis()) {
            newState = state.setValue(UP, !state.getValue(UP));
        } else {
            var up = state.getValue(UP);
            if(up) {
                if(facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    newState = state.setValue(HORIZONTAL_FACING, facing.getOpposite());
                } else {
                    newState = state.setValue(UP, false);
                }
            } else {
                if(facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
                    newState = state.setValue(HORIZONTAL_FACING, facing.getOpposite());
                } else {
                    newState = state.setValue(UP, true);
                }
            }
        }

        var world = context.getLevel();
        world.setBlockAndUpdate(context.getClickedPos(), newState);
        IWrenchable.playRotateSound(world, context.getClickedPos());

        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getHorizontalDirection();
        var up = (ctx.getClickLocation().y - ctx.getClickedPos().getY()) > 0.5f;
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing).setValue(UP, up);
    }
}
