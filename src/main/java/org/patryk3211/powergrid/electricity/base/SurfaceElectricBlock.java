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
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class SurfaceElectricBlock extends DirectionalElectricBlock {
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    public SurfaceElectricBlock(Properties settings) {
        super(settings);
    }

    public static BlockStateTerminalCollection surfaceTerminals(Block block, TerminalBoundingBox[] terminalsDown, VoxelShape shapeDown1, VoxelShape shapeDown2, Property<?>... ignored) {
        var shaper = VoxelShaper.forDirectional(shapeDown1, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(shapeDown2, Direction.DOWN);
        return BlockStateTerminalCollection.builder(block)
                .forAllStatesExcept(state -> BlockStateTerminalCollection.each(terminalsDown, terminal -> {
                    var facing = state.getValue(FACING);
                    terminal = switch(facing) {
                        case DOWN -> terminal;
                        case UP -> terminal.rotateAroundX(180);
                        case EAST -> terminal.rotateAroundZ(90).rotateAroundY(180);
                        case WEST -> terminal.rotateAroundZ(90);
                        case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                        case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                    };
                    if(!state.getValue(ALONG_FIRST_AXIS)) {
                        terminal = terminal.rotate(facing.getAxis(), 90);
                    }
                    return terminal;
                }), ignored)
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    var axis_along = state.getValue(ALONG_FIRST_AXIS);
                    var prov = (axis_along ^ facing.getAxis() == Direction.Axis.Y) ? shaper2 : shaper;
                    return prov.get(facing);
                })
                .build();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ALONG_FIRST_AXIS);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace().getOpposite();
        boolean along = true;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            if(player.getAxis() == Direction.Axis.X)
                along = false;
        } else {
            along = ctx.getNearestLookingDirection().getAxis() == facing.getClockWise().getAxis();
        }

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ALONG_FIRST_AXIS, along);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if(state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            if(rot == Rotation.NONE || rot == Rotation.CLOCKWISE_180)
                return state;
            return state.setValue(ALONG_FIRST_AXIS, !state.getValue(ALONG_FIRST_AXIS));
        }
        return super.rotate(state, rot);
    }
}
