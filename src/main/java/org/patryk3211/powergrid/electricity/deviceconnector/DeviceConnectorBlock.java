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
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.patryk3211.powergrid.base.CustomProperties;
import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.DirectionalElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.electricity.wire.powercord.IAcceptCord;
import org.patryk3211.powergrid.utility.proxy.ProxyProvider;
import org.patryk3211.powergrid.utility.proxy.TFMGProxy;

@MethodsReturnNonnullByDefault
public class DeviceConnectorBlock extends DirectionalElectricBlock implements IBE<DeviceConnectorBlockEntity>, IAcceptCord {
    public static final IntegerProperty ROTATION = CustomProperties.ROTATION_4;
    public static final BooleanProperty POLARIZED = BooleanProperty.create("polarized");

    private final TerminalBoundingBox[] TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 5.5, 1, 1.5, 10.5, 4, 4.5),
            new TerminalBoundingBox(IDecoratedTerminal.CONNECTOR, 5.5, 1, 11.5, 10.5, 4, 14.5)
    };

    // This assumes that terminal 0 is positive and terminal 1 is negative
    // which holds true for all blocks added by this mod.
    private final TerminalBoundingBox[] POLARIZED_TERMINALS_DOWN = new TerminalBoundingBox[] {
            new TerminalBoundingBox(IDecoratedTerminal.POSITIVE, 5.5, 1, 1.5, 10.5, 4, 4.5)
                    .withColor(IDecoratedTerminal.RED),
            new TerminalBoundingBox(IDecoratedTerminal.NEGATIVE, 5.5, 1, 11.5, 10.5, 4, 14.5)
                    .withColor(IDecoratedTerminal.BLUE)
    };

    private static final VoxelShape SHAPE_DOWN = box(4.5, 0, 3.5, 11.5, 4, 12.5);
    private static final VoxelShape SHAPE_DOWN_2 = box(3.5, 0, 4.5, 12.5, 4, 11.5);

    public DeviceConnectorBlock(BlockBehaviour.Properties settings) {
        super(settings);

        var shaper = VoxelShaper.forDirectional(SHAPE_DOWN, Direction.DOWN);
        var shaper2 = VoxelShaper.forDirectional(SHAPE_DOWN_2, Direction.DOWN);
        setTerminalCollection(BlockStateTerminalCollection.builder(this)
                .forAllStates(state -> BlockStateTerminalCollection.each(
                        state.getValue(POLARIZED) ? POLARIZED_TERMINALS_DOWN : TERMINALS_DOWN,
                        terminal -> {
                            var facing = state.getValue(FACING);
                            terminal = switch(facing) {
                                case DOWN -> terminal;
                                case UP -> terminal.rotateAroundX(180);
                                case EAST -> terminal.rotateAroundZ(-90);
                                case WEST -> terminal.rotateAroundZ(90);
                                case NORTH -> terminal.rotateAroundZ(90).rotateAroundY(90);
                                case SOUTH -> terminal.rotateAroundZ(90).rotateAroundY(-90);
                            };
                            var rotation = state.getValue(ROTATION);
                            terminal = terminal.rotate(facing.getAxis(), 90 * rotation - 90);
                            return terminal;
                        })
                )
                .withShapeMapper(state -> {
                    var facing = state.getValue(FACING);
                    var rotation = state.getValue(ROTATION);
                    boolean axis_along = rotation % 2 == 1;
                    var prov = (axis_along ^ facing.getAxis() == Direction.Axis.Y) ? shaper2 : shaper;
                    return prov.get(facing);
                })
                .build());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ROTATION, POLARIZED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getClickedFace().getOpposite();
        int rotation = 0;
        if(facing.getAxis() == Direction.Axis.Y) {
            var player = ctx.getHorizontalDirection();
            rotation = player.get2DDataValue();
        } else {
            rotation = 1;
        }
        var neighbor = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(facing));
        var polarized = neighbor.getBlock() instanceof IAcceptConnector acceptor && acceptor.isPolarized();

        if(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown())
            rotation = (rotation + 2) % 3;
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(ROTATION, rotation)
                .setValue(POLARIZED, polarized);
    }

    @ExpectPlatform
    public static boolean hasEnergyStorage(Level world, BlockPos pos, Direction side) {
        throw new AssertionError();
    }

    public static VoxelShape makeCheckShape(Direction side) {
        var min = new Vector3f(6 / 16f);
        var max = new Vector3f(10 / 16f);

        switch(side.getAxis()) {
            case X -> { min.x = 0; max.x = 1; }
            case Y -> { min.y = 0; max.y = 1; }
            case Z -> { min.z = 0; max.z = 1; }
        }

        return Shapes.box(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public static boolean canSupport(LevelReader world, BlockPos pos, BlockState state, Direction side) {
        var connectorShape = makeCheckShape(side);
        var shape = state.getCollisionShape(world, pos);
        // Check if side of the connector is covered by the supporting block's shape.
        return Shapes.blockOccudes(connectorShape, shape, side.getOpposite());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        var facing = state.getValue(FACING);
        var neighborPos = pos.relative(facing);
        var neighbor = world.getBlockState(neighborPos);
        if(neighbor.getBlock() == this)
            return false;
        if(world instanceof Level world1) {
            if(hasEnergyStorage(world1, neighborPos, facing.getOpposite()))
                return canSupport(world, neighborPos, neighbor, facing.getOpposite());
        }
        if(ProxyProvider.get(TFMGProxy.class)
                .map(proxy -> proxy.canConnect(world, neighborPos, facing.getOpposite()))
                .orElse(false)) {
            return true;
        }
        if(!(neighbor.getBlock() instanceof IAcceptConnector acceptor))
            return false;
        return acceptor.canConnect(world, neighborPos, neighbor, facing.getOpposite());
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborChanged(state, world, pos, sourceBlock, sourcePos, notify);
        if(!canSurvive(state, world, pos))
            world.destroyBlock(pos, true);
    }

    @Override
    public Class<DeviceConnectorBlockEntity> getBlockEntityClass() {
        return DeviceConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DeviceConnectorBlockEntity> getBlockEntityType() {
        return ModdedBlockEntities.DEVICE_CONNECTOR.get();
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        if(targetedFace.getAxis() == originalState.getValue(FACING).getAxis()) {
            return originalState.cycle(ROTATION);
        } else {
            return super.getRotatedBlockState(originalState, targetedFace);
        }
    }

    @Override
    public @Nullable AutoCordEndpoint getEndpoint(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var facing = state.getValue(FACING);

        var center = Vec3.atCenterOf(pos);
        var normal = facing.getNormal();
        var point = center.add(normal.getX() * 0.3125, normal.getY() * 0.3125, normal.getZ() * 0.3125);

        return new AutoCordEndpoint(context.getClickedPos(), 0, 1, point,
                renderPlug() ? context.getClickedFace() : null);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if(state.getValue(FACING).getAxis() == Direction.Axis.Y) {
            int rotation = (state.getValue(ROTATION) + switch(rot) {
                case NONE -> 0;
                case CLOCKWISE_90 -> 1;
                case CLOCKWISE_180 -> 2;
                case COUNTERCLOCKWISE_90 -> 3;
            } % 4);
            return state.setValue(ROTATION, rotation);
        }
        return super.rotate(state, rot);
    }
}
