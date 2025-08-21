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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlocks;

public class VerticalGeneratorHousing extends Block implements IWrenchable {
    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = box(1, 1, 1, 15, 15, 15);

    public VerticalGeneratorHousing(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HORIZONTAL_FACING);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        var firstFacing = originalState.getValue(HORIZONTAL_FACING);
        var secondFacing = firstFacing.getCounterClockWise();
        BlockState newState = ModdedBlocks.GENERATOR_HOUSING.getDefaultState();
        switch (targetedFace.getAxis()) {
            case Y -> {
                return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);
            }
            case X -> {
                if(firstFacing.getAxis() == Direction.Axis.X) {
                    newState = newState
                            .setValue(GeneratorHousing.HORIZONTAL_FACING, firstFacing)
                            .setValue(GeneratorHousing.UP, secondFacing.getAxisDirection() != firstFacing.getAxisDirection());
                } else {
                    newState = newState
                            .setValue(GeneratorHousing.HORIZONTAL_FACING, secondFacing)
                            .setValue(GeneratorHousing.UP, firstFacing.getAxisDirection() != secondFacing.getAxisDirection());
                }
            }
            case Z -> {
                if(firstFacing.getAxis() == Direction.Axis.Z) {
                    newState = newState
                            .setValue(GeneratorHousing.HORIZONTAL_FACING, firstFacing)
                            .setValue(GeneratorHousing.UP, secondFacing.getAxisDirection() == firstFacing.getAxisDirection());
                } else {
                    newState = newState
                            .setValue(GeneratorHousing.HORIZONTAL_FACING, secondFacing)
                            .setValue(GeneratorHousing.UP, firstFacing.getAxisDirection() == secondFacing.getAxisDirection());
                }
            }
        }
        return newState;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getHorizontalDirection();
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing);
    }
}
