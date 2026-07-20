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
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.kinetics.generator.winding.IWindingConnectable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VerticalGeneratorHousing extends Block implements IWrenchable, IWindingConnectable {
    public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int placementHelperId = PlacementHelpers.register(new VerticalGeneratorHousing.PlacementHelper());

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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(hand);
        IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(heldItem)) {
                placementHelper.getOffset(player, level, state, pos, hit)
                        .placeInWorld(level, (BlockItem) heldItem.getItem(), player, hand, hit);
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(state, level, pos, player, hand, hit);
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

    @Override
    public boolean canConnect(BlockState state, Direction side) {
        var face = state.getValue(HORIZONTAL_FACING);
        return side == face || side == face.getCounterClockWise();
    }

    @Override
    public Direction getOtherSide(BlockState state, Direction sideIn) {
        if(sideIn == state.getValue(HORIZONTAL_FACING)) {
            return sideIn.getCounterClockWise();
        } else {
            return sideIn.getClockWise();
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(HORIZONTAL_FACING, rot.rotate(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper implements IPlacementHelper {
        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return ModdedBlocks.VERTICAL_GENERATOR_HOUSING::isIn;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof VerticalGeneratorHousing;
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
            List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
                    state.getValue(HORIZONTAL_FACING).getAxis(), dir -> dir == Direction.DOWN || dir == Direction.UP &&
                            world.getBlockState(pos.relative(dir)).canBeReplaced()
                    );


            if (directions.isEmpty())
                return PlacementOffset.fail();
            else {
                return PlacementOffset.success(pos.relative(directions.get(0)),
                        s -> s.setValue(HORIZONTAL_FACING, state.getValue(HORIZONTAL_FACING)));
            }
        }
    }
}
