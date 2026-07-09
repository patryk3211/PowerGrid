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

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.utility.ShaperUtils;

public class SurfaceSwitchBlock extends SwitchBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_4;

    public SurfaceSwitchBlock(Properties settings) {
        super(settings);
    }

    public static BlockStateTerminalCollection switchDownTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape downShape) {
        var shapers = new VoxelShaper[] {
                VoxelShaper.forDirectional(downShape, Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.EAST), Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.SOUTH), Direction.DOWN),
                VoxelShaper.forDirectional(ShaperUtils.rotate(downShape, Direction.NORTH, Direction.WEST), Direction.DOWN)
        };
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminals,
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
                            int rotation = state.getValue(ROTATION);
                            if(facing == Direction.SOUTH) {
                                terminal = terminal.rotate(facing.getAxis(), -(90 * rotation - 90));
                            } else if(facing == Direction.EAST) {
                                terminal = terminal.rotate(facing.getAxis(), 180 - (90 * rotation - 90));
                            } else {
                                terminal = terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                            }
                            return terminal;
                        })
                )
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    int rotation = state.getValue(ROTATION);
                    if(facing.getAxis() == Direction.Axis.Y)
                        rotation = (rotation + 1) % 4;
                    return shapers[rotation].get(facing);
                })
                .build();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace().getOpposite();
        int rotation = 0;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            rotation = player.get2DDataValue() + 3;
        }

        if(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())
            rotation += 3;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ROTATION, rotation % 4);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace.getAxis() == originalState.getValue(FACING).getAxis()) {
            return originalState.cycle(ROTATION);
        }
        return super.getRotatedBlockState(originalState, targetedFace);
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
}
