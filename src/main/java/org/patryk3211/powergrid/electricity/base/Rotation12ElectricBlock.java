/*
 * Copyright 2026 patryk3211
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public abstract class Rotation12ElectricBlock extends HorizontalElectricBlock {
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_3;

    public Rotation12ElectricBlock(Properties settings) {
        super(settings);
    }

    public static BlockStateTerminalCollection rotation12Terminals(Block block, TerminalBoundingBox[] northBottomTerminals, VoxelShape northShape) {
        var shaper = VoxelShaper.forHorizontal(northShape, Direction.NORTH);
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(northBottomTerminals, terminal -> {
                    terminal = switch(state.getValue(ROTATION)) {
                        case 0 -> terminal;
                        case 1 -> terminal.rotateAroundX(-90);
                        case 2 -> terminal.rotateAroundX(180).rotateAroundY(180);
                        default -> throw new IllegalStateException();
                    };
                    return switch(state.getValue(HORIZONTAL_FACING)) {
                        case NORTH -> terminal;
                        case SOUTH -> terminal.rotateAroundY(180);
                        case EAST -> terminal.rotateAroundY(90);
                        case WEST -> terminal.rotateAroundY(-90);
                        default -> throw new IllegalStateException();
                    };
                }))
                .withShapeMapper(state -> shaper.get(state.getValue(HORIZONTAL_FACING)))
                .build();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var face = ctx.getClickedFace();
        if(face.getAxis() == Direction.Axis.Y) {
            var player = ctx.getPlayer() == null || !ctx.getPlayer().isShiftKeyDown() ?
                    ctx.getHorizontalDirection() : ctx.getHorizontalDirection().getOpposite();
            return defaultBlockState()
                    .setValue(HORIZONTAL_FACING, player)
                    .setValue(ROTATION, face == Direction.UP ? 0 : 2);
        } else {
            return defaultBlockState()
                    .setValue(ROTATION, 1)
                    .setValue(HORIZONTAL_FACING, face.getOpposite());
        }
    }
}
