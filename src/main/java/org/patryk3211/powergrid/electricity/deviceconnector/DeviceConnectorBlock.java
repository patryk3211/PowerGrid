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
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import team.reborn.energy.api.EnergyStorage;

public class DeviceConnectorBlock extends ElectricBlock implements IBE<DeviceConnectorBlockEntity> {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty ALONG_FIRST_AXIS = CustomProperties.ALONG_FIRST_AXIS;
    public static final BooleanProperty POLARIZED = BooleanProperty.of("polarized");

    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 5.5, 1, -0.5, 10.5, 6, 4.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 5.5, 1, 11.5, 10.5, 6, 16.5)
    };

    // This assumes that terminal 0 is positive and terminal 1 is negative
    // which holds true for all blocks added by this mod.
    private final TerminalBoundingBox[] POLARIZED_TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 5.5, 1, -0.5, 10.5, 6, 4.5)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 5.5, 1, 11.5, 10.5, 6, 16.5)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_DOWN = createCuboidShape(4.5, 0, 3.5, 11.5, 4, 12.5);
    private static final VoxelShape SHAPE_DOWN_2 = createCuboidShape(3.5, 0, 4.5, 12.5, 4, 11.5);

    public DeviceConnectorBlock(AbstractBlock.Settings settings) {
        super(settings);

        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(SHAPE_DOWN_2, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(
                        state.get(POLARIZED) ? POLARIZED_TERMINALS_DOWN : TERMINALS_DOWN,
                        terminal -> {
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
                        })
                )
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
        builder.add(FACING, ALONG_FIRST_AXIS, POLARIZED);
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
        var neighbor = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(facing));
        var polarized = neighbor.getBlock() instanceof IAcceptConnector acceptor && acceptor.isPolarized();

        if(ctx.getPlayer() != null && ctx.getPlayer().isSneaking())
            along = !along;
        return getDefaultState()
                .with(FACING, facing)
                .with(ALONG_FIRST_AXIS, along)
                .with(POLARIZED, polarized);
    }

    public static boolean hasEnergyStorage(World world, BlockPos pos, Direction side) {
        var storage = EnergyStorage.SIDED.find(world, pos, side);
        return storage != null && storage.supportsInsertion();
    }

    public static VoxelShape makeCheckShape(Direction side) {
        var min = new Vector3f(6 / 16f);
        var max = new Vector3f(10 / 16f);

        switch(side.getAxis()) {
            case X -> { min.x = 0; max.x = 1; }
            case Y -> { min.y = 0; max.y = 1; }
            case Z -> { min.z = 0; max.z = 1; }
        }

        return VoxelShapes.cuboid(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public static boolean canSupport(WorldView world, BlockPos pos, BlockState state, Direction side) {
        var connectorShape = makeCheckShape(side);
        var shape = state.getCollisionShape(world, pos);
        // Check if side of the connector is covered by the supporting block's shape.
        return VoxelShapes.isSideCovered(connectorShape, shape, side.getOpposite());
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        var facing = state.get(FACING);
        var neighborPos = pos.offset(facing);
        var neighbor = world.getBlockState(neighborPos);
        if(neighbor.getBlock() == this)
            return false;
        if(world instanceof World world1) {
            if(hasEnergyStorage(world1, neighborPos, facing.getOpposite()))
                return canSupport(world, neighborPos, neighbor, facing.getOpposite());
        }
        if(!(neighbor.getBlock() instanceof IAcceptConnector acceptor))
            return false;
        return acceptor.canConnect(world, neighborPos, neighbor, facing.getOpposite());
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
