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
package org.patryk3211.powergrid.electricity.wireconnector;

import com.google.common.collect.ImmutableMap;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.INamedTerminal;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import java.util.function.Function;

public class ConnectorBlock extends ElectricBlock implements IBE<ConnectorBlockEntity> {
    private static final ITerminalPlacement TERMINAL = new TerminalBoundingBox(INamedTerminal.CONNECTOR, 0, 0, 0, 16, 16, 16);

    private static final VoxelShape NORTH_SHAPE = createCuboidShape(5, 5, 0, 11, 11, 9);
    private static final VoxelShape SOUTH_SHAPE = createCuboidShape(5, 5, 7, 11, 11, 16);
    private static final VoxelShape WEST_SHAPE = createCuboidShape(0, 5, 5, 9, 11, 11);
    private static final VoxelShape EAST_SHAPE = createCuboidShape(7, 5, 5, 16, 11, 11);
    private static final VoxelShape UP_SHAPE = createCuboidShape(5, 7, 5, 11, 16, 11);
    private static final VoxelShape DOWN_SHAPE = createCuboidShape(5, 0, 5, 11, 9, 11);

    public ConnectorBlock(Settings settings) {
        super(settings);
    }

    public static BlockEntry<ConnectorBlock> register(final Registrate registrate) {
        return registrate.block("wire_connector", ConnectorBlock::new)
                .simpleItem()
                .register();
    }

    @Override
    public Class<ConnectorBlockEntity> getBlockEntityClass() {
        return ConnectorBlockEntity.class;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(Properties.FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState()
                .with(Properties.FACING, ctx.getSide().getOpposite());
    }

    @Override
    public BlockEntityType<? extends ConnectorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.WIRE_CONNECTOR.get();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch(state.get(Properties.FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            case UP -> UP_SHAPE;
            case DOWN -> DOWN_SHAPE;
        };
    }

    @Override
    public int terminalCount() {
        return 1;
    }

    @Override
    public ITerminalPlacement terminal(BlockState state, int index) {
        return index == 0 ? TERMINAL : null;
    }
}
