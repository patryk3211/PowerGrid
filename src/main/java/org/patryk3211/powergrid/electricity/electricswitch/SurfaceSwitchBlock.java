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
package org.patryk3211.powergrid.electricity.electricswitch;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SideShapeType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;

public class SurfaceSwitchBlock extends SwitchBlock {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    public SurfaceSwitchBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, ALONG_FIRST_AXIS);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getSide().getOpposite();
        boolean along = true;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalPlayerFacing();
            if(player.getAxis() == Direction.Axis.X)
                along = false;
        } else {
            along = false;
            if(ctx.getPlayerLookDirection().getAxis() == facing.rotateYClockwise().getAxis())
                along = true;
        }

        return getDefaultState()
                .with(FACING, facing)
                .with(ALONG_FIRST_AXIS, along);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var facing = state.get(FACING);
        var blockState = world.getBlockState(pos.offset(facing));
        return blockState.isSideSolid(world, pos, facing.getOpposite(), SideShapeType.RIGID);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return direction == state.get(FACING) && !canPlaceAt(state, world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
}
