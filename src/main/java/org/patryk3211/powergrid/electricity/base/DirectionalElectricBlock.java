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
package org.patryk3211.powergrid.electricity.base;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

@MethodsReturnNonnullByDefault
public abstract class DirectionalElectricBlock extends ElectricBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public DirectionalElectricBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ? ctx.getNearestLookingDirection() : ctx.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(FACING, player);
    }

    public static BlockStateTerminalCollection directionalNorthTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape northShape) {
        return directionalNorthTerminals(block, terminals, northShape, null);
    }

    public static BlockStateTerminalCollection directionalNorthTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape northShape, @Nullable VoxelShape upShape) {
        var shaper = VoxelShaper.forDirectional(northShape, Direction.NORTH);
        if(upShape != null) shaper.withVerticalShapes(upShape);
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminals, terminal -> switch(state.getValue(FACING)) {
                    case NORTH -> terminal;
                    case SOUTH -> terminal.rotateAroundY(180);
                    case EAST -> terminal.rotateAroundY(90);
                    case WEST -> terminal.rotateAroundY(-90);
                    case UP -> terminal.rotateAroundX(-90);
                    case DOWN -> terminal.rotateAroundX(90);
                }))
                .withShapeMapper(state -> shaper.get(state.getValue(FACING)))
                .build();
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }
}
