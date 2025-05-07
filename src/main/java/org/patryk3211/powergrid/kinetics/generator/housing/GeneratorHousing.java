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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.IConnectableBlock;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.kinetics.generator.coil.CoilBlock;

public class GeneratorHousing extends Block implements IConnectableBlock, IWrenchable {
    public static final EnumProperty<Direction> HORIZONTAL_FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = Properties.UP;

    private static final VoxelShape SHAPE_SOUTH_DOWN = createCuboidShape(0, 0, 2, 16, 14, 16);
    private static final VoxelShape SHAPE_SOUTH_UP = createCuboidShape(0, 2, 2, 16, 16, 16);
    private static final VoxelShape SHAPE_NORTH_DOWN = createCuboidShape(0, 0, 0, 16, 14, 14);
    private static final VoxelShape SHAPE_NORTH_UP = createCuboidShape(0, 2, 0, 16, 16, 14);

    private static final VoxelShape SHAPE_EAST_DOWN = createCuboidShape(2, 0, 0, 16, 14, 16);
    private static final VoxelShape SHAPE_EAST_UP = createCuboidShape(2, 2, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_WEST_DOWN = createCuboidShape(0, 0, 0, 14, 14, 16);
    private static final VoxelShape SHAPE_WEST_UP = createCuboidShape(0, 2, 0, 14, 16, 16);

    public GeneratorHousing(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HORIZONTAL_FACING, UP);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        boolean up = state.get(UP);
        return switch(state.get(HORIZONTAL_FACING)) {
            case SOUTH -> up ? SHAPE_SOUTH_UP : SHAPE_SOUTH_DOWN;
            case NORTH -> up ? SHAPE_NORTH_UP : SHAPE_NORTH_DOWN;
            case EAST -> up ? SHAPE_EAST_UP : SHAPE_EAST_DOWN;
            case WEST -> up ? SHAPE_WEST_UP : SHAPE_WEST_DOWN;
            default -> null;
        };
    }

    @Override
    public boolean connects(BlockState state, Direction side, BlockState checkState) {
        // Generator housing can only connect to coils.
        if(!checkState.isOf(ModdedBlocks.COIL.get()))
            return false;
        if(side.getAxis() == Direction.Axis.Y) {
            // Up or down side, coil facing must reflect housing facing.
            return state.get(HORIZONTAL_FACING) == checkState.get(CoilBlock.FACING);
        } else {
            // Other sides, coil facing must be up or down, depending on the housing up value.
            var coilFacing = checkState.get(CoilBlock.FACING);
            var up = state.get(UP);
            return up ? coilFacing == Direction.UP : coilFacing == Direction.DOWN;
        }
    }

    @Override
    public boolean canPropagate(BlockState state, Direction direction) {
        if(direction == state.get(HORIZONTAL_FACING))
            return true;
        var up = state.get(UP);
        return up ? direction == Direction.UP : direction == Direction.DOWN;
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        BlockState newState = null;
        var facing = state.get(HORIZONTAL_FACING);
        if(context.getSide().getAxis() == Direction.Axis.Y) {
            newState = state.with(HORIZONTAL_FACING, facing.rotateYClockwise());
        } else if(context.getSide().getAxis() == facing.getAxis()) {
            newState = state.with(UP, !state.get(UP));
        } else {
            var up = state.get(UP);
            if(up) {
                if(facing.getDirection() == Direction.AxisDirection.POSITIVE) {
                    newState = state.with(HORIZONTAL_FACING, facing.getOpposite());
                } else {
                    newState = state.with(UP, false);
                }
            } else {
                if(facing.getDirection() == Direction.AxisDirection.NEGATIVE) {
                    newState = state.with(HORIZONTAL_FACING, facing.getOpposite());
                } else {
                    newState = state.with(UP, true);
                }
            }
        }

        var world = context.getWorld();
        world.setBlockState(context.getBlockPos(), newState);
        this.playRotateSound(world, context.getBlockPos());

        return ActionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getHorizontalPlayerFacing();
        var up = (ctx.getHitPos().y - ctx.getBlockPos().getY()) > 0.5f;
        return getDefaultState().with(HORIZONTAL_FACING, facing).with(UP, up);
    }
}
