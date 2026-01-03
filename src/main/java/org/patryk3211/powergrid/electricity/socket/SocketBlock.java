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
package org.patryk3211.powergrid.electricity.socket;

import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SocketBlock extends DirectionalElectricBlock implements IBE<SocketBlockEntity>, ISocketElectric {
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_4;

    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 3, 10, 2, 4)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 6, 0, 12, 10, 2, 13)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private final TerminalBoundingBox SOCKET_DOWN = new TerminalBoundingBox(IDecoratedTerminal.SOCKET, 6, 3, 6, 10, 5, 10);
    private final TerminalBoundingBox SOCKET_UP = SOCKET_DOWN.rotateAroundX(180);
    private final TerminalBoundingBox SOCKET_NORTH = SOCKET_DOWN.rotateAroundX(-90);
    private final TerminalBoundingBox SOCKET_SOUTH = SOCKET_DOWN.rotateAroundX(90);
    private final TerminalBoundingBox SOCKET_EAST = SOCKET_DOWN.rotateAroundX(90).rotateAroundY(-90);
    private final TerminalBoundingBox SOCKET_WEST = SOCKET_DOWN.rotateAroundX(90).rotateAroundY(90);

    private static final VoxelShape SHAPE_DOWN = box(4, 0, 4, 12, 4, 12);

    public SocketBlock(Properties settings) {
        super(settings);
        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS_DOWN,
                        terminal -> {
                            var facing = state.getValue(FACING);
                            terminal = switch(facing) {
                                case DOWN -> terminal;
                                case UP -> terminal.rotateAroundX(180);
                                case EAST -> terminal.rotateAroundZ(-90);
                                case WEST -> terminal.rotateAroundZ(90);
                                case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                                case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                            };
                            var rotation = state.getValue(ROTATION);
                            terminal = terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                            return terminal;
                        })
                )
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    return shaper.get(facing);
                })
                .build());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace().getOpposite();
        int rotation = 0;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            rotation = player.get2DDataValue();
        } else {
            rotation = 1;
        }

        if(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())
            rotation = (rotation + 2) % 3;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ROTATION, rotation);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(FACING);
        return canSupportCenter(world, pos.relative(facing, 1), facing.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        var facing = state.getValue(FACING);
        return direction == facing && !canSurvive(state, world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace.getAxis() == originalState.getValue(FACING).getAxis()) {
            return originalState.cycle(ROTATION);
        } else {
            return super.getRotatedBlockState(originalState, targetedFace);
        }
    }

    @Override
    public Class<SocketBlockEntity> getBlockEntityClass() {
        return SocketBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SocketBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.SOCKET.get();
    }

    @Override
    public ITerminalPlacement socket(BlockState state) {
        return switch(state.getValue(FACING)) {
            case DOWN -> SOCKET_DOWN;
            case UP -> SOCKET_UP;
            case NORTH -> SOCKET_NORTH;
            case SOUTH -> SOCKET_SOUTH;
            case EAST -> SOCKET_EAST;
            case WEST -> SOCKET_WEST;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if(state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            int rotation = (state.getValue(ROTATION) + switch(rot) {
                case NONE -> 0;
                case CLOCKWISE_90 -> 1;
                case CLOCKWISE_180 -> 2;
                case COUNTERCLOCKWISE_90 -> 3;
            } % 4);
            return state.setValue(ROTATION, rotation);
        }
        return super.rotate(state, rot);
    }
}
