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
package org.patryk3211.powergrid.kinetics.generator.coil;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.IConnectableBlock;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class CoilBlock extends ElectricBlock implements IBE<CoilBlockEntity>, IConnectableBlock {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty HAS_TERMINALS = BooleanProperty.of("terminals");

    private static final TerminalBoundingBox UP_TERMINAL_1 =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 2, 0, 2, 5, 2, 5)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox UP_TERMINAL_2 =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 11, 0, 11, 14, 2, 14)
                    .withColor(IDecoratedTerminal.BLUE);

    private static final TerminalBoundingBox DOWN_TERMINAL_1 =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 11, 14, 2, 14, 16, 5)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox DOWN_TERMINAL_2 =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 2, 14, 11, 5, 16, 14)
                    .withColor(IDecoratedTerminal.BLUE);

    private static final TerminalBoundingBox NORTH_TERMINAL_1 =
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 2, 2, 14, 5, 5, 16)
                    .withColor(IDecoratedTerminal.RED);
    private static final TerminalBoundingBox NORTH_TERMINAL_2 =
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 11, 11, 14, 14, 14, 16)
                    .withColor(IDecoratedTerminal.BLUE);

    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_180);
    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_180);
    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.CLOCKWISE_90);
    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);
    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotateAroundY(BlockRotation.COUNTERCLOCKWISE_90);

    private static final VoxelShape SHAPE_UP = VoxelShapes.union(
            createCuboidShape(0, 2, 0,16, 14, 16),
            UP_TERMINAL_1.getShape(),
            UP_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_DOWN = VoxelShapes.union(
            createCuboidShape(0, 2, 0,16, 14, 16),
            DOWN_TERMINAL_1.getShape(),
            DOWN_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            createCuboidShape(0, 0, 2, 16, 16, 14),
            NORTH_TERMINAL_1.getShape(),
            NORTH_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(
            createCuboidShape(0, 0, 2, 16, 16, 14),
            SOUTH_TERMINAL_1.getShape(),
            SOUTH_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(
            createCuboidShape(2, 0, 0, 14, 16, 16),
            EAST_TERMINAL_1.getShape(),
            EAST_TERMINAL_2.getShape()
    );
    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(
            createCuboidShape(2, 0, 0, 14, 16, 16),
            WEST_TERMINAL_1.getShape(),
            WEST_TERMINAL_2.getShape()
    );

    public CoilBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ActionResult onWrenched(BlockState state, ItemUsageContext context) {
        if(context.getSide() == state.get(FACING).getOpposite()) {
            var world = context.getWorld();
            if(!world.isClient) {
                boolean hasTerminals = !state.get(HAS_TERMINALS);
                if (world.getBlockEntity(context.getBlockPos()) instanceof CoilBlockEntity coil) {
                    if (hasTerminals) {
                        coil.getAggregate().makeOutput(coil);
                    } else {
                        coil.getAggregate().removeOutput(coil);
                    }
                }
                return ActionResult.SUCCESS;
            }
        }
        var result = super.onWrenched(state, context);
        if(result == ActionResult.SUCCESS && !context.getWorld().isClient) {
            var behaviour = BlockEntityBehaviour.get(context.getWorld(), context.getBlockPos(), CoilBehaviour.TYPE);
            if(behaviour != null) {
                behaviour.grabRotor();
            }
        }
        return result;
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        var behaviour = BlockEntityBehaviour.get(world, pos, CoilBehaviour.TYPE);
        if(behaviour != null)
            behaviour.onNeighborChanged(sourcePos);
        if(world.getBlockEntity(pos) instanceof CoilBlockEntity coil) {
            coil.rebuildAggregate();
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, HAS_TERMINALS);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var preferredFacing = getPreferredFacing(ctx.getBlockPos(), ctx.getWorld());
        var facing = preferredFacing == null || ctx.getPlayer() == null || ctx.getPlayer().isSneaking() ? ctx.getPlayerLookDirection() : preferredFacing;
        return getDefaultState()
                .with(FACING, facing)
                .with(HAS_TERMINALS, false);
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(FACING)) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        if(!state.get(HAS_TERMINALS))
            return null;
        return switch(state.get(FACING)) {
            case UP -> switch(index) {
                case 0 -> UP_TERMINAL_1;
                case 1 -> UP_TERMINAL_2;
                default -> null;
            };
            case DOWN -> switch(index) {
                case 0 -> DOWN_TERMINAL_1;
                case 1 -> DOWN_TERMINAL_2;
                default -> null;
            };
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
        };
    }

    @Override
    public boolean connects(BlockState state, Direction side, BlockState checkState) {
        // Coils only connect if their facing is the same
        if(checkState.isOf(this) && state.get(FACING) == checkState.get(FACING))
            return true;
        var housing = ModdedBlocks.GENERATOR_HOUSING.get();
        if(checkState.isOf(housing)) {
            // Use the housing implementation for this check.
            return housing.connects(checkState, side.getOpposite(), state);
        }
        return false;
    }

    @Override
    public boolean canPropagate(BlockState state, Direction direction) {
        return state.get(FACING).getAxis() != direction.getAxis();
    }

    @Nullable
    public Direction getPreferredFacing(BlockPos pos, World world) {
        Direction facing = null;
        for(var dir : Direction.values()) {
            var state = world.getBlockState(pos.offset(dir));
            if(state.isOf(this)) {
                if(facing != null)
                    return null;
                facing = state.get(FACING);
                if(facing.getAxis() == dir.getAxis())
                    facing = null;
            }
        }
        return facing;
    }

    @Override
    public Class<CoilBlockEntity> getBlockEntityClass() {
        return CoilBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CoilBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.GENERATOR_COIL.get();
    }

    public static float resistance() {
        return 0.1f;
    }
}
