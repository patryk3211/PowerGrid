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
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public abstract class DirectionalElectricBlock extends ElectricBlock {
    public static final DirectionProperty FACING = Properties.FACING;

    public DirectionalElectricBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var player = ctx.getPlayer() == null || !ctx.getPlayer().isSneaking() ? ctx.getPlayerLookDirection() : ctx.getPlayerLookDirection().getOpposite();
        return getDefaultState().with(FACING, player);
    }

    public static BlockStateTerminalCollection directionalNorthTerminals(Block block, TerminalBoundingBox[] terminals, VoxelShape northShape) {
        var shaper = VoxelShaper.forDirectional(northShape, Direction.NORTH);
        return BlockStateTerminalCollection.builder(block)
                .forAllStates(state -> BlockStateTerminalCollection.each(terminals, terminal -> switch(state.get(FACING)) {
                    case NORTH -> terminal;
                    case SOUTH -> terminal.rotateAroundY(180);
                    case EAST -> terminal.rotateAroundY(90);
                    case WEST -> terminal.rotateAroundY(-90);
                    case UP -> terminal.rotateAroundX(-90);
                    case DOWN -> terminal.rotateAroundX(90);
                }))
                .withShapeMapper(state -> shaper.get(state.get(FACING)))
                .build();
    }
}
