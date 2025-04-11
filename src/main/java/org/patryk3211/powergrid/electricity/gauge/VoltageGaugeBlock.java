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
package org.patryk3211.powergrid.electricity.gauge;

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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.*;

public class VoltageGaugeBlock extends ElectricBlock implements IBE<VoltageGaugeBlockEntity>, IGaugeBlock {
    private static final TerminalBoundingBox NORTH_TERMINAL_1 =
            new TerminalBoundingBox(INamedTerminal.POSITIVE, 13, 12, 7, 16, 16, 9, 0.5)
                    .withOrigin(14.5, 15, 8);
    private static final TerminalBoundingBox NORTH_TERMINAL_2 =
            new TerminalBoundingBox(INamedTerminal.NEGATIVE, 0, 12, 7, 3, 16, 9, 0.5)
                    .withOrigin(1.5, 15, 8);

    private static final TerminalBoundingBox SOUTH_TERMINAL_1 = NORTH_TERMINAL_1.rotated(Direction.SOUTH);
    private static final TerminalBoundingBox SOUTH_TERMINAL_2 = NORTH_TERMINAL_2.rotated(Direction.SOUTH);

    private static final TerminalBoundingBox EAST_TERMINAL_1 = NORTH_TERMINAL_1.rotated(Direction.EAST);
    private static final TerminalBoundingBox EAST_TERMINAL_2 = NORTH_TERMINAL_2.rotated(Direction.EAST);

    private static final TerminalBoundingBox WEST_TERMINAL_1 = NORTH_TERMINAL_1.rotated(Direction.WEST);
    private static final TerminalBoundingBox WEST_TERMINAL_2 = NORTH_TERMINAL_2.rotated(Direction.WEST);

    private static final VoxelShape SHAPE_NORTH_SOUTH = VoxelShapes.union(
            createCuboidShape(1, 0, 2, 15, 14, 14),
            NORTH_TERMINAL_1.getShape(),
            NORTH_TERMINAL_2.getShape()
    );

    private static final VoxelShape SHAPE_EAST_WEST = VoxelShapes.union(
            createCuboidShape(2, 0, 1, 14, 14, 15),
            EAST_TERMINAL_1.getShape(),
            EAST_TERMINAL_2.getShape()
    );

    public VoltageGaugeBlock(Settings settings) {
        super(settings);
    }

    public static BlockEntry<VoltageGaugeBlock> register(Registrate registrate) {
        return registrate.block("voltage_meter", VoltageGaugeBlock::new)
                .simpleItem()
                .register();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(Properties.HORIZONTAL_FACING)) {
            case NORTH, SOUTH -> SHAPE_NORTH_SOUTH;
            case EAST, WEST -> SHAPE_EAST_WEST;
            default -> throw new IllegalArgumentException("Invalid horizontal facing");
        };
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
            default -> null;
        };
    }

    @Override
    public Class<VoltageGaugeBlockEntity> getBlockEntityClass() {
        return VoltageGaugeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VoltageGaugeBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.VOLTAGE_METER.get();
    }

    @Override
    public float getMaxValue() {
        return 20;
    }

    @Override
    public boolean shouldRenderHeadOnFace(World world, BlockPos pos, BlockState state, Direction dir) {
        var facing = state.get(Properties.HORIZONTAL_FACING);
        return dir.getAxis() == facing.getAxis();
    }
}
