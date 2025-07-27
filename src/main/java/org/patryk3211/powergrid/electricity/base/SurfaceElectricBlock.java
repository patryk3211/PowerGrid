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

import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public abstract class SurfaceElectricBlock extends DirectionalElectricBlock {
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    public SurfaceElectricBlock(Settings settings) {
        super(settings);
    }

    public static BlockStateTerminalCollection surfaceTerminals(Block block, TerminalBoundingBox[] terminalsDown, VoxelShape shapeDown1, VoxelShape shapeDown2) {
        var shaper = VoxelShaper.forDirectional(shapeDown1, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(shapeDown2, Direction.DOWN);
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminalsDown, terminal -> {
                    var facing = state.get(FACING);
                    terminal = switch(facing) {
                        case DOWN -> terminal;
                        case UP -> terminal.rotateAroundX(180);
                        case EAST -> terminal.rotateAroundZ(-90);
                        case WEST -> terminal.rotateAroundZ(90);
                        case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                        case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                    };
                    if(!state.get(ALONG_FIRST_AXIS)) {
                        terminal = terminal.rotate(facing.getAxis(), 90);
                    }
                    return terminal;
                }))
                .withShapeMapper(state -> {
                    var facing = state.get(FACING);
                    var axis_along = state.get(ALONG_FIRST_AXIS);
                    var prov = (axis_along ^ facing.getAxis() == Direction.Axis.Y) ? shaper2 : shaper;
                    return prov.get(facing);
                })
                .build();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ALONG_FIRST_AXIS);
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
            along = ctx.getPlayerLookDirection().getAxis() == facing.rotateYClockwise().getAxis();
        }

        return getDefaultState()
                .with(FACING, facing)
                .with(ALONG_FIRST_AXIS, along);
    }
}
