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
package org.patryk3211.powergrid.electricity.deviceconnector;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class DeviceConnectorBlock extends ElectricBlock implements IBE<DeviceConnectorBlockEntity> {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;

    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 4, 4, 9, 8, 7)
                    .withOrigin(8, 7, 5.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 7, 4, 9, 9, 8, 12)
                    .withOrigin(8, 7, 10.5)
    };

    private static final VoxelShape SHAPE_DOWN = createCuboidShape(6, 0, 5, 10, 6, 11);
    private static final VoxelShape SHAPE_DOWN_2 = createCuboidShape(5, 0, 6, 11, 6, 10);

    public DeviceConnectorBlock(AbstractBlock.Settings settings) {
        super(settings);

        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(SHAPE_DOWN_2, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(TERMINALS_DOWN, terminal -> {
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
                .build());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, ALONG_FIRST_AXIS);
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

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var facing = state.get(FACING);
        var neighbor = world.getBlockState(pos.offset(facing));
        if(!(neighbor.getBlock() instanceof IAcceptConnector acceptor))
            return false;
        return acceptor.canConnect(neighbor, facing.getOpposite());
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if(!canPlaceAt(state, world, pos))
            world.breakBlock(pos, true);
    }

    @Override
    public Class<DeviceConnectorBlockEntity> getBlockEntityClass() {
        return DeviceConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DeviceConnectorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.DEVICE_CONNECTOR.get();
    }
}
