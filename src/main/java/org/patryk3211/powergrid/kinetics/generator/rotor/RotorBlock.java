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
package org.patryk3211.powergrid.kinetics.generator.rotor;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;

public class RotorBlock extends RotatedPillarKineticBlock implements IBE<RotorBlockEntity> {
    public static final EnumProperty<ShaftDirection> SHAFT_DIRECTION = EnumProperty.of("shaft", ShaftDirection.class);

    public RotorBlock(Settings properties) {
        super(properties);
        setDefaultState(getDefaultState()
                .with(SHAFT_DIRECTION, ShaftDirection.POSITIVE));
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        var side = context.getSide();
        var world = context.getWorld();
        if(side.getAxis() == state.get(AXIS)) {
            var shaft = state.get(SHAFT_DIRECTION);
            BlockState alteredState = null;
            if(shaft.axisDirection() == side.getDirection()) {
                alteredState = state.with(SHAFT_DIRECTION, ShaftDirection.NONE);
            } else if(shaft == ShaftDirection.NONE) {
                alteredState = state.with(SHAFT_DIRECTION, ShaftDirection.from(side.getDirection()));
            } else {
                // Opposite side has shaft, fail since we can't have more than one shaft input.
                return ActionResult.FAIL;
            }

            if(!alteredState.canPlaceAt(world, context.getBlockPos()))
                return ActionResult.PASS;

            KineticBlockEntity.switchToBlockState(world, context.getBlockPos(), updateAfterWrenched(alteredState, context));
            playRotateSound(world, context.getBlockPos());
            return ActionResult.SUCCESS;
        } else {
            return super.onWrenched(state, context);
        }
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction face) {
        var facing = state.get(SHAFT_DIRECTION).with(state.get(AXIS));
        if(facing != null) {
            assert face.getAxis() != state.get(AXIS);
            facing = face.getDirection() == Direction.AxisDirection.POSITIVE ? facing.rotateClockwise(face.getAxis()) : facing.rotateCounterclockwise(face.getAxis());
            return state
                    .with(AXIS, facing.getAxis())
                    .with(SHAFT_DIRECTION, ShaftDirection.from(facing.getDirection()));
        } else {
            // Rotate only using axis.
            return super.getRotatedBlockState(state, face);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(SHAFT_DIRECTION);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.get(SHAFT_DIRECTION) == ShaftDirection.NONE ? null : blockState.get(AXIS);
    }

    @Override
    public boolean hasShaftTowards(WorldView world, BlockPos pos, BlockState state, Direction face) {
        if(state.get(AXIS) == face.getAxis()) {
            var direction = state.get(SHAFT_DIRECTION);
            return face.getDirection() == direction.axisDirection();
        }
        return false;
    }

    @Override
    public Class<RotorBlockEntity> getBlockEntityClass() {
        return RotorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RotorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.ROTOR.get();
    }

    public boolean hasPositive(World world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(switch(axis) {
            case X -> pos.east();
            case Y -> pos.up();
            case Z -> pos.south();
        });
        return state.isOf(this) && state.get(SHAFT_DIRECTION) != ShaftDirection.NEGATIVE && state.get(AXIS) == axis;
    }

    public boolean hasNegative(World world, BlockPos pos, Direction.Axis axis) {
        BlockState state = world.getBlockState(switch(axis) {
            case X -> pos.west();
            case Y -> pos.down();
            case Z -> pos.north();
        });
        return state.isOf(this) && state.get(SHAFT_DIRECTION) != ShaftDirection.POSITIVE && state.get(AXIS) == axis;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();

        Direction.Axis preferredAxis = null;
        for(Direction.Axis axis : Direction.Axis.VALUES) {
            if(hasPositive(world, pos, axis) ||
                hasNegative(world, pos, axis)) {
                if(preferredAxis != null) {
                    preferredAxis = null;
                    break;
                }
                preferredAxis = axis;
            }
        }

        if(preferredAxis == null)
            preferredAxis = getPreferredAxis(context);

        Direction direction = null;
        if(preferredAxis == null || (context.getPlayer() != null && context.getPlayer().isSneaking())) {
            direction = context.getPlayerLookDirection().getOpposite();
            preferredAxis = direction.getAxis();
        }

        boolean positive = hasPositive(world, pos, preferredAxis);
        boolean negative = hasNegative(world, pos, preferredAxis);

        ShaftDirection shaft = ShaftDirection.NONE;
        if(!positive && !negative && direction != null)
            shaft = ShaftDirection.from(direction.getDirection());

        return getDefaultState()
                .with(AXIS, preferredAxis)
                .with(SHAFT_DIRECTION, shaft);
    }
}
