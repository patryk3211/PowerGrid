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
package org.patryk3211.powergrid.electricity.heater;

import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.INamedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class HeaterBlock extends ElectricBlock implements IBE<HeaterBlockEntity> {
    private static final TerminalBoundingBox NORTH_TERMINAL1 = new TerminalBoundingBox(INamedTerminal.CONNECTOR, 12, 12, 7, 15, 15, 10, 0.5);
    private static final TerminalBoundingBox NORTH_TERMINAL2 = new TerminalBoundingBox(INamedTerminal.CONNECTOR, 1, 12, 7, 4, 15, 10, 0.5);

    private static final TerminalBoundingBox SOUTH_TERMINAL1 = NORTH_TERMINAL1.rotated(Direction.SOUTH);
    private static final TerminalBoundingBox SOUTH_TERMINAL2 = NORTH_TERMINAL2.rotated(Direction.SOUTH);

    private static final TerminalBoundingBox EAST_TERMINAL1 = NORTH_TERMINAL1.rotated(Direction.EAST);
    private static final TerminalBoundingBox EAST_TERMINAL2 = NORTH_TERMINAL2.rotated(Direction.EAST);

    private static final TerminalBoundingBox WEST_TERMINAL1 = NORTH_TERMINAL1.rotated(Direction.WEST);
    private static final TerminalBoundingBox WEST_TERMINAL2 = NORTH_TERMINAL2.rotated(Direction.WEST);

    private static final VoxelShape SHAPE_NORTH = VoxelShapes.union(
            createCuboidShape(0, 0, 5, 16, 12, 11),
            NORTH_TERMINAL1.getShape(),
            NORTH_TERMINAL2.getShape()
    );

    private static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(
            createCuboidShape(0, 0, 5, 16, 12, 11),
            SOUTH_TERMINAL1.getShape(),
            SOUTH_TERMINAL2.getShape()
    );

    private static final VoxelShape SHAPE_EAST = VoxelShapes.union(
            createCuboidShape(5, 0, 0, 11, 12, 16),
            EAST_TERMINAL1.getShape(),
            EAST_TERMINAL2.getShape()
    );

    private static final VoxelShape SHAPE_WEST = VoxelShapes.union(
            createCuboidShape(5, 0, 0, 11, 12, 16),
            WEST_TERMINAL1.getShape(),
            WEST_TERMINAL2.getShape()
    );

    public HeaterBlock(Settings settings) {
        super(settings);
    }

    public static BlockEntry<HeaterBlock> register(Registrate registrate) {
        return registrate.block("heating_coil", HeaterBlock::new)
                .simpleItem()
                .register();
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public int terminalCount() {
        return 2;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return switch(state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH -> switch(index) {
                case 0 -> NORTH_TERMINAL1;
                case 1 -> NORTH_TERMINAL2;
                default -> null;
            };
            case SOUTH -> switch(index) {
                case 0 -> SOUTH_TERMINAL1;
                case 1 -> SOUTH_TERMINAL2;
                default -> null;
            };
            case EAST -> switch(index) {
                case 0 -> EAST_TERMINAL1;
                case 1 -> EAST_TERMINAL2;
                default -> null;
            };
            case WEST -> switch(index) {
                case 0 -> WEST_TERMINAL1;
                case 1 -> WEST_TERMINAL2;
                default -> null;
            };
            default -> null;
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> throw new IllegalArgumentException("Invalid horizontal facing");
        };
    }

    @Override
    public Class<HeaterBlockEntity> getBlockEntityClass() {
        return HeaterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeaterBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.HEATING_COIL.get();
    }
}
