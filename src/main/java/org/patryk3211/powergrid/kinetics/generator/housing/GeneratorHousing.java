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
import net.minecraft.world.item.context.BlockPlaceContext;
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
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.kinetics.generator.winding.IWindingConnectable;

public class GeneratorHousing extends Block implements IWrenchable, IWindingConnectable {
    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UP = BlockStateProperties.UP;

    private static final VoxelShape SHAPE = box(1, 1, 1, 15, 15, 15);

    public GeneratorHousing(Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING, UP);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        BlockState newState = null;
        var facing = state.getValue(HORIZONTAL_FACING);
        if(targetedFace.getAxis() == Direction.Axis.Y) {
            newState = state.setValue(HORIZONTAL_FACING, facing.getClockWise());
        } else if(targetedFace.getAxis() == facing.getAxis()) {
            if(state.getValue(UP)) {
                newState = ModdedBlocks.VERTICAL_GENERATOR_HOUSING.getDefaultState()
                        .setValue(HORIZONTAL_FACING, facing.getClockWise());
            } else {
                newState = ModdedBlocks.VERTICAL_GENERATOR_HOUSING.getDefaultState()
                        .setValue(HORIZONTAL_FACING, facing);
            }
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
        return newState;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getHorizontalDirection();
        var up = (ctx.getClickLocation().y - ctx.getClickedPos().getY()) > 0.5f;
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing).setValue(UP, up);
    }

    @Override
    public boolean canConnect(BlockState state, Direction side) {
        if(side.getAxis() == Direction.Axis.Y) {
            var up = state.getValue(UP);
            return (up && side == Direction.UP) ||
                   (!up && side == Direction.DOWN);
        } else {
            return side == state.getValue(HORIZONTAL_FACING);
        }
    }

    @Override
    public Direction getOtherSide(BlockState state, Direction sideIn) {
        if(sideIn.getAxis() == Direction.Axis.Y) {
            return state.getValue(HORIZONTAL_FACING);
        } else {
            return state.getValue(UP) ? Direction.UP : Direction.DOWN;
        }
    }
}
